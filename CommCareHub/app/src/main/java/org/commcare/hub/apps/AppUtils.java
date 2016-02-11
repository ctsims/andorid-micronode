package org.commcare.hub.apps;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsims on 2/11/2016.
 */
public class AppUtils {
    public static Map<String, AppModel> getUniqueInstalledApps() {
        Map<String, AppModel> apps = new HashMap<>();
        SQLiteDatabase db = HubApplication._().getDatabaseHandle();
        Cursor c = db.query(AppModel.TABLE_NAME, null, "status = ?", new String[] {"Processed"}, null, null, null);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            AppModel app = AppModel.fromDb(c);
            if(apps.containsKey(app.getAppId())) {
                if(apps.get(app.getAppId()).getVersion() < app.getVersion()) {
                    apps.put(app.getAppId(), app);
                }
            } else {
                apps.put(app.getAppId(), app);
            }
        }
        c.close();
        db.close();
        return apps;
    }


    public static AppModel getAppModel(int appId) {
        SQLiteDatabase db = HubApplication._().getDatabaseHandle();
        try {
            return AppModel.getRow(appId, db);
        } finally {
            db.close();
        }
    }
}
