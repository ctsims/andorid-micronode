package org.commcare.hub.application;

import android.app.Application;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.commcare.hub.apps.AppSyncService;
import org.commcare.hub.database.DatabaseOpenHelper;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.mirror.UserSyncService;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.nsd.NsdHubService;
import org.commcare.hub.server.ServerService;
import org.commcare.hub.services.HubService;
import org.commcare.hub.services.ServiceHost;
import org.commcare.hub.util.FileUtil;

import java.io.File;

/**
 * Created by ctsims on 12/14/2015.
 */
public class HubApplication extends Application {
    private static HubApplication singleton;

    ServiceHost host;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        SQLiteDatabase.loadLibs(this);
        configureFileLocations();

        //Make DB create code execute synchronously
        getDatabaseHandle().close();

        createServiceConnectors();
    }

    public static void configureFileLocations() {
        for(String root : FileUtil.fileRoots) {
            File f = new File(FileUtil.path(root));
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    public static HubApplication _() {
        return singleton;
    }

    public SQLiteDatabase getDatabaseHandle() throws DatabaseUnavailableException {
        SQLiteDatabase database;
        try {
            database = new DatabaseOpenHelper(this).getWritableDatabase("password");
            return database;
        } catch (SQLiteException e) {
            e.printStackTrace();
            ServicesMonitor.reportMessage("Failed to open local database : " + e.getMessage());
            throw new DatabaseUnavailableException();
        }
    }

    private void createServiceConnectors() {
        host = new ServiceHost(new HubService[]{new ServerService(), new UserSyncService(this), new NsdHubService(this), new AppSyncService(this)});
        host.start();
    }


}