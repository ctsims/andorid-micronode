package org.commcare.hub.util;

import android.content.ContentValues;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;
import android.widget.ProgressBar;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.AppAssetBroadcast;
import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.apps.MobileUserModelBroadcast;
import org.commcare.hub.domain.DomainSyncThread;
import org.commcare.hub.events.HubEventBroadcast;
import org.commcare.hub.mirror.SyncableUser;
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
    public static final String TABLE_USER_LIST = "MobileUser";

    /**
     * @return true if a row was added, false if one was updated
     */
    public static Pair<Long, Boolean> upsertRow(SQLiteDatabase db, String table, ContentValues cv, String uniqueId) {
        return upsertRow(db, table, cv, uniqueId, "_id");
    }

    /**
     * @return true if a row was added, false if one was updated
     */
    public static Pair<Long, Boolean> upsertRow(SQLiteDatabase db, String table, ContentValues cv, String uniqueId, String primaryKey) {
        Cursor c = db.query(table, new String[] {primaryKey}, uniqueId + " = ?", new String[] {cv.getAsString(uniqueId)}, null, null, null);
        try {
            if (c.getCount() == 0) {
                long newId = db.insert(table, null, cv);
                return new Pair<>(newId, true);
            } else {
                c.moveToFirst();
                long updatedId = c.getLong(c.getColumnIndexOrThrow(primaryKey));
                db.update(table, cv, uniqueId + " = ?", new String[]{cv.getAsString(uniqueId)});
                return new Pair<>(updatedId, false);
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

    public static List<SyncableUser> getSyncableUserForWebUser(SQLiteDatabase db, int webUserID) {
        Cursor c = db.query(SyncableUser.TABLE_NAME, null, "mobile_user_id = (? + 0)", new String[] {String.valueOf(webUserID)}, null, null, null);
        try {
            ArrayList<SyncableUser> models = new ArrayList();

            while (c.moveToNext()) {
                models.add(SyncableUser.fromDb(c));
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

    public static final String getDomainGuidFromId(SQLiteDatabase db, int domainId) {
        Cursor c = db.query(DomainSyncThread.TABLE_DOMAIN_LIST, new String[] {"domain_guid"}, "id = (? + 0)", new String[] {String.valueOf(domainId)}, null, null, null);
        String retVal = null;
        if(c.moveToFirst()) {
            retVal = c.getString(c.getColumnIndexOrThrow("domain_guid"));
        }
        c.close();
        return retVal;
    }

    public static void requestUserSyncData(SQLiteDatabase db, int userId, String username, int domainId) {
        String domainGuid = getDomainGuidFromId(db, domainId);

        db.beginTransaction();
        try {
            //If we have any existing app assets for this (installed or not),
            //we can skip the download
            for (SyncableUser model : getSyncableUserForWebUser(db, userId) ){
                if(model.getStatus() == SyncableUser.UserStatus.AuthError) {
                    model.setRetry();
                    model.writeToDb(db);
                }
                else {
                    ServicesMonitor.reportMessage("Already have userdata for user");
                    db.setTransactionSuccessful();
                    return;
                }
            }

            SyncableUser user = new SyncableUser(username, domainGuid);
            user.writeToDb(db);
            ServicesMonitor.reportMessage("Creating request for user sync: " + username);

            db.setTransactionSuccessful();
            HubApplication._().getServiceHost().broadcast(new MobileUserModelBroadcast(domainId, userId, HubEventBroadcast.EventType.Update));
        }finally {
            db.endTransaction();
        }
    }

}
