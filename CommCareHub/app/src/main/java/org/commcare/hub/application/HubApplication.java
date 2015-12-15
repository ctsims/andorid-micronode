package org.commcare.hub.application;

import android.app.Application;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.commcare.hub.database.DatabaseOpenHelper;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.mirror.UserSyncService;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.server.ServerService;
import org.commcare.hub.services.HubService;
import org.commcare.hub.services.ServiceHost;

/**
 * Created by ctsims on 12/14/2015.
 */
public class HubApplication extends Application {
    private static HubApplication singleton;

    ServiceHost host;

    @Override
    public void onCreate() {
        super.onCreate();
        SQLiteDatabase.loadLibs(this);

        singleton = this;
        createServiceConnectors();
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
        host = new ServiceHost(new HubService[]{new ServerService(), new UserSyncService(this)});
        host.start();
    }


}