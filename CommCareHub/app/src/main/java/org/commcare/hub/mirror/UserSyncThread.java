package org.commcare.hub.mirror;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Base64;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
import org.commcare.hub.util.FileUtil;
import org.commcare.hub.util.StreamsUtil;
import org.commcare.hub.util.WebUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by ctsims on 12/14/2015.
 */
public class UserSyncThread extends HubRunnable {
    Context context;

    public UserSyncThread(Context context) {
        this.context = context;
    }

    public void runInternal() throws DatabaseUnavailableException {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();

        Cursor c = database.query("SyncableUser", null, null, null, null, null, null);
        try {
            SyncableUser actionable = null;
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                SyncableUser u = SyncableUser.fromDb(c);

                if (u.getStatus() != SyncableUser.UserStatus.ModelFetched && u.getStatus() != SyncableUser.UserStatus.AuthError) {
                    //Only grab the first user to act on, we don't wanna be greedy in this thread
                    actionable = u;
                    break;
                }
            }

            if (actionable == null) {
                return;
            }

            if (actionable.getStatus() == SyncableUser.UserStatus.Requested) {
                fetchKeyForUser(actionable, database);
            } else if(actionable.getStatus() == SyncableUser.UserStatus.KeysFetched) {
                try {
                    fetchModelForUser(actionable, database);
                } catch(IOException ioe) {
                    ServicesMonitor.reportMessage("Error fetching user model for " + actionable.getUsername()+ " - " + ioe.getMessage());
                }
            }

        } finally {
            c.close();
        }

    }

    //HttpURLConnection

    private void fetchModelForUser(SyncableUser actionable, SQLiteDatabase database) throws IOException{
        String template = "https://www.commcarehq.org/hq/admin/phone/restore/?version=2.0&raw=true&as=" + actionable.getUsername();
        String syncLocation = fetchForUser(template, actionable, database);
        if(syncLocation != null) {
            actionable.setSyncFile(syncLocation);
            String passwordHash = processSyncFileForPassword(syncLocation);
            actionable.setPasswordHash(passwordHash);
            actionable.updateStatus(SyncableUser.UserStatus.ModelFetched);
            actionable.writeToDb(database);

            ServicesMonitor.reportMessage("Sync record fetch successful for " + actionable.getUsername());
        }

    }

    private String processSyncFileForPassword(String syncLocation) throws IOException {
        Pattern p = Pattern.compile("(sha1\\$[^<]*)<");
        String chunk = FileUtil.getFirstChunkOfFile(syncLocation, 300);
        Matcher m = p.matcher(chunk);
        if(m.find()) {
            return m.group(1);
        } else {
            throw new IOException("Didn't find sync pw in user restore");
        }
    }

    private void fetchKeyForUser(final SyncableUser actionable, SQLiteDatabase database) {
        String template = "https://www.commcarehq.org/a/" +actionable.getDomain() + "/phone/keys/";
        String keyLocation = fetchForUser(template, actionable, database);
        if(keyLocation != null) {
            actionable.setKeyFile(keyLocation);
            actionable.updateStatus(SyncableUser.UserStatus.KeysFetched);
            actionable.writeToDb(database);

            ServicesMonitor.reportMessage("Key record fetch successful for " + actionable.getUsername());
        }
    }

    private String fetchForUser(String template, final SyncableUser actionable, SQLiteDatabase database) {
        try {
            URL url = new URL(template);

            final Pair<String, String> credentials = HubApplication._().getCredentials();

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(credentials.first, credentials.second.toCharArray());
                }
            });


            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            WebUtil.setupGetConnection(conn, credentials);

            int response = conn.getResponseCode();

            ServicesMonitor.reportMessage("HTTP Record Request for " + actionable.getUsername() +  " response code: " + response);

            if(response >= 500) {
                actionable.updateStatus(SyncableUser.UserStatus.AuthError);
                actionable.writeToDb(database);

                ServicesMonitor.reportMessage("Error fetching user data" + conn.getResponseMessage());
                return null;
            }
            else if(response >= 400) {
                actionable.updateStatus(SyncableUser.UserStatus.AuthError);
                actionable.writeToDb(database);
                return null;
            }

            else if(response == 200) {
                try {
                    String guid = UUID.randomUUID().toString();
                    InputStream responseStream = new BufferedInputStream(conn.getInputStream());
                    StreamsUtil.writeFromInputToOutput(responseStream, context.openFileOutput(guid, Context.MODE_PRIVATE));

                    return guid;

                } catch(IOException e) {
                    e.printStackTrace();
                    ServicesMonitor.reportMessage("Error downloading user record " + e.getMessage());
                }
            }
        }catch( IOException ioe) {

        }
        return null;
    }


}
