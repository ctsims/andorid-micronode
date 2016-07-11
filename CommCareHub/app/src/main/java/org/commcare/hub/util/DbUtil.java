package org.commcare.hub.util;

import android.content.ContentValues;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.AppAssetBroadcast;
import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsims on 7/8/2016.
 */
public class DbUtil {

    public static final String TABLE_APP_MANIFEST = "AppManifest";

    public static void upsertRow(SQLiteDatabase db, String table, ContentValues cv, String uniqueId) {
        upsertRow(db, table, cv, uniqueId, "_id");
    }

    public static void upsertRow(SQLiteDatabase db, String table, ContentValues cv, String uniqueId, String primaryKey) {
        Cursor c = db.query(table, new String[] {primaryKey}, uniqueId + " = ?", new String[] {cv.getAsString(uniqueId)}, null, null, null);
        try {
            if (c.getCount() == 0) {
                db.insert(table, null, cv);
            } else {
                db.update(table, cv, uniqueId + " = ?", new String[]{cv.getAsString(uniqueId)});
            }
        }finally {
            c.close();
        }
    }

    public static List<AppAssetModel> getAppAssetsForManifest(SQLiteDatabase db, int manifestId) {
        Cursor c = db.query(AppAssetModel.TABLE_NAME, null, "app_manifest_id = (? + 0)", new String[] {String.valueOf(manifestId)}, null, null, null);
        try {
            ArrayList<AppAssetModel> models = new ArrayList();

            while (c.moveToNext()) {
                models.add(AppAssetModel.fromDb(c));
            }
            return models;
        } finally {
            c.close();
        }

    }

    public static void requestManifestAsset(SQLiteDatabase db, int manifestId, int version,
                                            String url) {
        db.beginTransaction();
        try {

            //If we have any existing app assets for this (installed or not),
            //we can skip the download
            for (AppAssetModel model : getAppAssetsForManifest(db, manifestId) ){
                if (model.getVersion() >= version) {
                    ServicesMonitor.reportMessage("Update/install request request rejected for version " +version+ "of existing app [" + manifestId + "] with version + " + model.getVersion());
                    db.setTransactionSuccessful();
                    return;
                }
            }

            //Otherwise we should create a new asset request
            AppAssetModel.createAppModelRequest(url, version, manifestId).writeToDb(db);
            ServicesMonitor.reportMessage("Creating new app asset request[" + version + "] at: " + url);

            db.setTransactionSuccessful();
            HubApplication._().getServiceHost().broadcast(new AppAssetBroadcast(manifestId));
        }finally {
            db.endTransaction();


        }
    }
}