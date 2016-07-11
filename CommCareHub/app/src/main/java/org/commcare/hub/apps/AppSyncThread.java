package org.commcare.hub.apps;

import android.content.Context;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
import org.commcare.hub.util.FileUtil;
import org.commcare.hub.util.StreamsUtil;
import org.commcare.hub.util.WebUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 *
 * Created by ctsims on 12/14/2015.
 */
public class AppSyncThread extends HubRunnable{
    Context context;

    public AppSyncThread(Context context) {
        this.context = context;
    }

    public void runInternal() throws DatabaseUnavailableException {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();
        Cursor c = database.query(AppAssetModel.TABLE_NAME, null, null, null, null, null, null);

        try {
            AppAssetModel actionable = null;
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                AppAssetModel app = AppAssetModel.fromDb(c);

                if(app.getStatus() != AppAssetModel.Status.Processed && !app.isPaused()) {
                    actionable = app;
                }
            }

            if (actionable == null) {
                return;
            }

            switch(actionable.getStatus()) {
                case Requested:
                    fetchAppArchive(actionable, database);
                    break;
                case Downloaded:
                    unzZipAppArchive(actionable, database);
                    break;
            }

        } finally {
            c.close();
            //database.close();
        }

    }

    //HttpURLConnection

    private void fetchAppArchive(AppAssetModel actionable, SQLiteDatabase database) {
        String localFileUri = fetchAppCCZ(actionable.getSourceUri(), database);
        if(localFileUri != null) {
            actionable.setArchiveFetched(localFileUri);
            actionable.writeToDb(database);
            ServicesMonitor.reportMessage("Archive fetch successful for " + actionable.getAppManifestId());
        }

    }

    private void unzZipAppArchive(AppAssetModel actionable, SQLiteDatabase database) {
        String guid = UUID.randomUUID().toString();

        File tempRootPath = new File(FileUtil.path(FileUtil.TEMP_ROOT), guid);

        if(!tempRootPath.exists()) {
            tempRootPath.mkdirs();
        }
        try {
            try {
                FileUtil.unzipArchiveToFolder(new File(actionable.getArchivePath()), tempRootPath);
            } catch(ZipException ze) {
                //Bad Zip file
                ServicesMonitor.reportMessage("Bad zip file download at " + tempRootPath);
                //TOOD: Set state as corrupt and allow redownload
                return;
            }

            String appId = getAppIdforModel(actionable, database);

            processUnzippedAppArchive(actionable, tempRootPath);
    
            File appsRoot = new File(FileUtil.path(FileUtil.APP_ROOT), appId);
            if(!appsRoot.exists()) {
                appsRoot.mkdirs();
            }
    
            File appRoot = new File(appsRoot, String.valueOf(actionable.getVersion()));
    
            if(appRoot.exists()) {
                clearFolder(appRoot);
            }

            if(!tempRootPath.renameTo(appRoot)) {
                throw new IOException("Couldn't rename installed app folder from:\n" +
                tempRootPath.getAbsolutePath() +"\nto\n" +
                appRoot.getAbsolutePath());
            }

            actionable.setAppProcessed(appRoot.getAbsolutePath());
            actionable.writeToDb(database);
            this.getServiceConnector().queueBroadcast(new AppAssetBroadcast(actionable.getAppManifestId()));

            ServicesMonitor.reportMessage("Succesfully unpackaged " + appId);

            return;
        } catch (IOException ioe) {
            processAndReport(ioe, actionable, database);
            return;
        }
    }

    public static final String MANIFEST_TABLE = "AppManifest";

    private String getAppIdforModel(AppAssetModel actionable, SQLiteDatabase database) {
        int id = actionable.getAppManifestId();
        Cursor c = database.query(MANIFEST_TABLE,new String[] {"app_guid"},"_id = ?",new String[] {String.valueOf(id)}, null, null, null);
        if(c.getCount() == 0) {
            throw new RuntimeException("Missing manifest!");
        }
        c.moveToFirst();
        String guid = c.getString(c.getColumnIndexOrThrow("app_guid"));
        c.close();
        return guid;
    }

    private void clearFolder(File appRoot) throws IOException{
        FileUtil.deleteFileOrDir(appRoot);
    }

    private void processAndReport(IOException ioe, AppAssetModel actionable, SQLiteDatabase database) {
        ioe.printStackTrace();
        ServicesMonitor.reportMessage("Serious Error unzipping archive!\n" + ioe.getMessage());
        actionable.setPaused(true);
        actionable.writeToDb(database);
    }

    private void processUnzippedAppArchive(AppAssetModel actionable, File tempRootPath) throws IOException{
        File profileFile = new File(tempRootPath, "profile.ccpr");
        if(!profileFile.exists()) {
            throw new FileNotFoundException("CommCare Archive has no profile");
        }

        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document profile = parser.parse(profileFile);

            XPath xpath = XPathFactory.newInstance().newXPath();

            String version = (String)xpath.evaluate("/profile/@version", profile, XPathConstants.STRING);

            if(version == null) {
                throw new IOException("Missing Version number in profile!");
            }

            int versionNum;
            try {
                versionNum = Integer.valueOf(version);
            }catch (NumberFormatException e) {
                throw new IOException("Invalid profile version: " + version);
            }

            String updateUrl = (String)xpath.evaluate("/profile/@update", profile, XPathConstants.STRING);

            if(updateUrl == null) {
                updateUrl = actionable.getAuthUri();
            }

            String name = (String)xpath.evaluate("/profile/@name", profile, XPathConstants.STRING);

            String uniqueid = (String)xpath.evaluate("/profile/@uniqueid", profile, XPathConstants.STRING);

            actionable.setProcessedData(versionNum, updateUrl);

        } catch (ParserConfigurationException e) {
            throw new IOException("Parser Error reading profile file", e);
        } catch (SAXException e) {
            throw new IOException("Parser Error reading profile file", e);
        } catch (XPathExpressionException e) {
            throw new IOException("XPath Expression error when parsing profile ", e);
        }
    }

    private String fetchAppCCZ(String template, SQLiteDatabase database) {
        try {
            URL url = new URL(template);

            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            WebUtil.setupGetConnection(conn);

            int response = conn.getResponseCode();

            ServicesMonitor.reportMessage("Fetching CCZ Archive for " + template +  " response code: " + response);

            if(response >= 500) {
                ServicesMonitor.reportMessage("Error fetching CCZ Archive" + conn.getResponseMessage());
                return null;
            }
            else if(response >= 400) {
                ServicesMonitor.reportMessage("Auth Error Fetching CCZ Archive");
                return null;
            }

            else if(response == 200) {
                try {
                    String guid = UUID.randomUUID().toString();
                    File download = new File(FileUtil.path(FileUtil.TEMP_ROOT), guid);
                    File finalLocation = new File(FileUtil.path(FileUtil.CCZ_ARCHIVES), guid);
                    InputStream responseStream = new BufferedInputStream(conn.getInputStream());
                    StreamsUtil.writeFromInputToOutput(responseStream, new BufferedOutputStream(new FileOutputStream(download)));

                    //once the download is complete, move it over (risk of failure past this point is acceptable)
                    download.renameTo(finalLocation);

                    ServicesMonitor.reportMessage("CCZ Archive fetched to " + finalLocation);
                    return finalLocation.getAbsolutePath();

                } catch(IOException e) {
                    e.printStackTrace();
                    ServicesMonitor.reportMessage("Error downloading CCZ Archive record " + e.getMessage());
                }
            }
        }catch( IOException ioe) {
            ioe.printStackTrace();
            ServicesMonitor.reportMessage("Error fetching archive " + ioe.getMessage());
        }
        return null;
    }


}
