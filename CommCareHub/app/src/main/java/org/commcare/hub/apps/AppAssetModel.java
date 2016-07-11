package org.commcare.hub.apps;

import android.content.ContentValues;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.util.DbUtil;

/**
 * An app asset represents a physical binary CCZ file and/or a request to download or process one
 *
 * Created by ctsims on 2/11/2016.
 */
public class AppAssetModel {
    public static final String TABLE_NAME = "AppAsset";

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

    public void setProcessedData(int versionNum, String updateUrl) {
        this.appVersion = versionNum;
        this.authUri = updateUrl;
    }

    public static AppAssetModel getRow(int appId, SQLiteDatabase db) {
        Cursor c = db.query(AppAssetModel.TABLE_NAME, null,
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

    private AppAssetModel() {

    }

    private long rowId;
    private int appManifest;
    private int appVersion;
    private String appArchivePath;
    private String appSandboxPath;
    private Status status;
    private String authUri;
    private String sourceUri;

    private boolean paused = false;


    public static AppAssetModel createAppModelRequest(String sourceUri, int appVersion, int appManifest) {
        AppAssetModel m = new AppAssetModel();
        m.sourceUri = sourceUri;
        m.status = Status.Requested;
        m.paused = false;
        m.appVersion = appVersion;
        m.appManifest = appManifest;
        m.rowId = -1;
        return m;
    }


    public void setArchiveFetched(String archivePath) {
        this.status = Status.Downloaded;
        this.appArchivePath = archivePath;
    }

    public static AppAssetModel fromDb(Cursor c) {
        AppAssetModel app = new AppAssetModel();

        app.rowId = c.getInt(c.getColumnIndexOrThrow("id"));
        app.appManifest= c.getInt(c.getColumnIndexOrThrow("app_manifest_id"));
        app.appVersion = c.getInt(c.getColumnIndexOrThrow("version"));
        app.appArchivePath = c.getString(c.getColumnIndexOrThrow("ccz_path"));
        app.appSandboxPath = c.getString(c.getColumnIndexOrThrow("sandbox_path"));
        app.status = Status.valueOf(c.getString(c.getColumnIndexOrThrow("status")));
        app.authUri = c.getString(c.getColumnIndexOrThrow("auth_uri"));
        app.sourceUri = c.getString(c.getColumnIndexOrThrow("source_uri"));
        app.paused = Boolean.valueOf(c.getString(c.getColumnIndexOrThrow("paused")));


        return app;
    }

    public void writeToDb(SQLiteDatabase database) {
        ContentValues cv = new ContentValues();

        cv.put("app_manifest_id", appManifest);
        cv.put("version", appVersion);
        cv.put("ccz_path", appArchivePath);
        cv.put("sandbox_path", appSandboxPath);
        cv.put("status", status.name());
        cv.put("auth_uri", authUri);
        cv.put("source_uri", sourceUri);
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

    public int getAppManifestId() {
        return appManifest;
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

    private String guid;
    private String name;

    public void linkToManifest(SQLiteDatabase db) {
        Cursor c = db.query(DbUtil.TABLE_APP_MANIFEST, new String[] {"app_guid", "app_descriptor"}, "_id = (? + 0)", new String[] { String.valueOf(this.getAppManifestId())}, null, null, null);
        if(c.moveToFirst()) {
            guid = c.getString(c.getColumnIndexOrThrow("app_guid"));
            name = c.getString(c.getColumnIndexOrThrow("app_descriptor"));
        }
    }

    public String getAppManifestGuid() {
        return guid;
    }

    public String getAppName() {
        return name;
    }
}
