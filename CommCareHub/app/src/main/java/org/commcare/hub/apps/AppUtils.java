package org.commcare.hub.apps;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsims on 2/11/2016.
 */
public class AppUtils {
    public static Map<String, AppAssetModel> getUniqueInstalledApps() {
        SQLiteDatabase db = HubApplication._().getDatabaseHandle();
        Cursor c = db.query(AppAssetModel.TABLE_NAME, null, "status = ?", new String[]{"Processed"}, null, null, null);
        try {
            Map<String, AppAssetModel> apps = new HashMap<>();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                AppAssetModel app = AppAssetModel.fromDb(c);
                app.linkToManifest(db);

                if (apps.containsKey(app.getAppManifestGuid())) {
                    if (apps.get(app.getAppManifestGuid()).getVersion() < app.getVersion()) {
                        apps.put(app.getAppManifestGuid(), app);
                    }
                } else {
                    apps.put(app.getAppManifestGuid(), app);
                }
            }
        return apps;
    } finally {
            c.close();
            db.close();
        }

    }


    public static AppAssetModel getAppModel(int appId) {
        SQLiteDatabase db = HubApplication._().getDatabaseHandle();
        try {
            return AppAssetModel.getRow(appId, db);
        } finally {
            db.close();
        }
    }
}
