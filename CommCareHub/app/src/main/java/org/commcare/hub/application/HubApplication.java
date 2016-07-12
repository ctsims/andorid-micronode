package org.commcare.hub.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.commcare.hub.LoginActivity;
import org.commcare.hub.apps.AppSyncService;
import org.commcare.hub.database.DatabaseOpenHelper;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.domain.DomainSyncService;
import org.commcare.hub.domain.DomainSyncThread;
import org.commcare.hub.mirror.UserSyncService;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.nsd.NsdHubService;
import org.commcare.hub.server.ServerService;
import org.commcare.hub.services.HubService;
import org.commcare.hub.services.ServiceHost;
import org.commcare.hub.sync.ServerMetadataSyncService;
import org.commcare.hub.util.FileUtil;

import java.io.File;

/**
 * Created by ctsims on 12/14/2015.
 */
public class HubApplication extends Application {
    private static HubApplication singleton;

    private ServiceHost host;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        SQLiteDatabase.loadLibs(this);
        FileUtil.configureFileLocations();

        //Make DB create code execute synchronously
        getDatabaseHandle().close();
        databaseCache = null;

        createServiceConnectors();
    }

    public static HubApplication _() {
        return singleton;
    }

    SQLiteDatabase databaseCache;
    public SQLiteDatabase getDatabaseHandle() throws DatabaseUnavailableException {
        SQLiteDatabase database;
        try {
            if(databaseCache == null) {
                databaseCache = new DatabaseOpenHelper(this).getWritableDatabase("password");
            }
            return databaseCache;
        } catch (SQLiteException e) {
            e.printStackTrace();
            ServicesMonitor.reportMessage("Failed to open local database : " + e.getMessage());
            throw new DatabaseUnavailableException();
        }
    }

    private void createServiceConnectors() {
        //host = new ServiceHost(new HubService[]{new ServerService(), new UserSyncService(this), new NsdHubService(this), new AppSyncService(this), new ServerMetadataSyncService(this)});
        host = new ServiceHost(new HubService[]{new ServerService(), new UserSyncService(this), new NsdHubService(this), new AppSyncService(this), new DomainSyncService(this)});
        host.start();
    }

    public ServiceHost getServiceHost() {
        return host;
    }

    public Pair<String, String> getCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        return new Pair<>(preferences.getString(LoginActivity.KEY_USERNAME,""),
                preferences.getString(LoginActivity.KEY_PASSWORD,""));

    }
}