package org.commcare.hub.util;

import android.os.Environment;
import android.util.Log;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.monitor.ServicesMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Created by ctsims on 2/11/2016.
 */
public class FileUtil {

    public static final String CCZ_ARCHIVES = "app_archives";
    public static final String USER_STORAGE = "user_data";
    public static final String APP_ROOT = "apps";
    public static final String TEMP_ROOT = "temp";

    public static final String[] fileRoots = new String[] {CCZ_ARCHIVES, USER_STORAGE, APP_ROOT, TEMP_ROOT};

    public static String path(String relativeSubDir) {
        return new File(storageRoot(), relativeSubDir).getAbsolutePath();
    }


    public static String storageRoot() {
        return Environment.getExternalStorageDirectory().toString() + "/Android/data/" +
                HubApplication._().getPackageName();
    }

    public static void unzipArchiveToFolder(File archiveFile, File destinationFolder) throws IOException {
        Log.d("FileUtil", "Unzipping archive '" + archiveFile + "' to  '" + destinationFolder + "'");

        ZipFile zipfile = new ZipFile(archiveFile);;
        for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry)e.nextElement();

            if (entry.isDirectory()) {
                new File(destinationFolder, entry.getName()).mkdirs();
                //If it's a directory we can move on to the next one
                continue;
            }

            File outputFile = new File(destinationFolder, entry.getName());
            if (!outputFile.getParentFile().exists()) {
                new File(outputFile.getParentFile().toString()).mkdirs();
            }
            if (outputFile.exists()) {
                //Try to overwrite if we can
                if (!outputFile.delete()) {
                    //If we couldn't, just skip for now
                    continue;
                }
            }
            BufferedInputStream inputStream;
            try {
                inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
            } catch (IOException ioe) {
                throw new IOException("Error reading file: " + entry, ioe);
            }

            BufferedOutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            } catch (IOException ioe) {
                throw new IOException("Error creating destination file for unzip: " + outputFile, ioe);
            }


            try {
                try {
                    StreamsUtil.writeFromInputToOutput(inputStream, outputStream);
                } catch (IOException ioe) {
                    throw new IOException("Error unzipping file: " + entry, ioe);
                }
            } finally {
                    outputStream.close();

                    inputStream.close();
            }
        }
    }


    // Returns true if the file and all of its contents were deleted successfully, false otherwise
    public static boolean deleteFileOrDir(File f) throws IOException {
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                if (!deleteFileOrDir(child)) {
                    throw new IOException("Could not remove file: " + child);
                }
            }
        }

        if(!f.delete()) {
            f.deleteOnExit();
            return false;
        }
        return true;
    }


}

