package org.commcare.hub.apps;

import android.content.ContentValues;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.util.Date;

/**
 * Created by ctsims on 2/11/2016.
 */
public class AppModel {
    public static final String TABLE_NAME = "AppModel";

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setAppProcessed(String unzippedAppPath) {
        this.status = Status.Processed;
        this.appSandboxPath = unzippedAppPath;
    }

    public void setProcessedData(int versionNum, String updateUrl, String uniqueid, String name) {
        this.appVersion = versionNum;
        this.authUri = updateUrl;
        this.appId = uniqueid;
        this.name = name;
    }

    public String getAppName() {
        return name;
    }

    public static AppModel getRow(int appId, SQLiteDatabase db) {
        Cursor c = db.query(AppModel.TABLE_NAME, null,
                "id = ?", new String[] {String.valueOf(appId)}, null, null, null);

        try {
            if (!c.moveToFirst()) {
                return null;
            }

            return fromDb(c);
        } finally {
            c.close();
        }
    }

    public static enum Status {
        Requested,
        Downloaded,
        Processed
    }

    private AppModel() {

    }

    private long rowId;
    private String appId;
    private int appVersion;
    private String appArchivePath;
    private String appSandboxPath;
    private Status status;
    private String authUri;
    private String sourceUri;

    String name;
    private boolean paused = false;


    public static AppModel createAppModelRequest(String sourceUri) {
        AppModel m = new AppModel();
        m.sourceUri = sourceUri;
        m.status = Status.Requested;
        m.paused = false;
        m.rowId = -1;
        return m;
    }


    public void setArchiveFetched(String archivePath) {
        this.status = Status.Downloaded;
        this.appArchivePath = archivePath;
    }

    public static AppModel fromDb(Cursor c) {
        AppModel app = new AppModel();

        app.rowId = c.getInt(c.getColumnIndexOrThrow("id"));
        app.appId= c.getString(c.getColumnIndexOrThrow("app_id"));
        app.appVersion = c.getInt(c.getColumnIndexOrThrow("version"));
        app.appArchivePath = c.getString(c.getColumnIndexOrThrow("ccz_path"));
        app.appSandboxPath = c.getString(c.getColumnIndexOrThrow("sandbox_path"));
        app.status = Status.valueOf(c.getString(c.getColumnIndexOrThrow("status")));
        app.authUri = c.getString(c.getColumnIndexOrThrow("auth_uri"));
        app.sourceUri = c.getString(c.getColumnIndexOrThrow("source_uri"));
        app.name = c.getString(c.getColumnIndexOrThrow("app_descriptor"));
        app.paused = Boolean.valueOf(c.getString(c.getColumnIndexOrThrow("paused")));


        return app;
    }

    public void writeToDb(SQLiteDatabase database) {
        ContentValues cv = new ContentValues();

        cv.put("app_id", appId);
        cv.put("version", appVersion);
        cv.put("ccz_path", appArchivePath);
        cv.put("sandbox_path", appSandboxPath);
        cv.put("status", status.name());
        cv.put("auth_uri", authUri);
        cv.put("source_uri", sourceUri);
        cv.put("app_descriptor", name);
        cv.put("paused", Boolean.toString(paused));


        if(rowId == -1) {
            rowId = database.insertOrThrow(TABLE_NAME, null, cv);
        } else {
            database.update(TABLE_NAME, cv, "id=?", new String[]{String.valueOf(rowId)});
        }

    }

    public long getRowId() {
        return rowId;
    }

    public String getAppId() {
        return appId;
    }

    public int getVersion() {
        return appVersion;
    }

    public String getArchivePath() {
        return appArchivePath;
    }

    public String getSandboxPath() {
        return appSandboxPath;
    }

    public Status getStatus() {
        return status;
    }

    public String getAuthUri() {
        return authUri;
    }

    public String getSourceUri() {
        return sourceUri;
    }




}
