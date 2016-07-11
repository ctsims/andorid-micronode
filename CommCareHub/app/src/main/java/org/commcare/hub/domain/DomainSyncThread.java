package org.commcare.hub.domain;

import android.content.ContentValues;
import android.content.Context;
import android.support.v4.util.Pair;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
import org.commcare.hub.sync.*;
import org.commcare.hub.sync.ServerMetadataSyncService;
import org.commcare.hub.util.DbUtil;
import org.commcare.hub.util.WebUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;

/**
 *
 * Created by ctsims on 12/14/2015.
 */
public class DomainSyncThread extends HubRunnable {
    Context context;
    SQLiteDatabase db;

    public static final String TABLE_DOMAIN_LIST = "DomainList";

    public DomainSyncThread(Context context) {
        this.context = context;
    }

    public void runInternal() throws DatabaseUnavailableException {
        db = HubApplication._().getDatabaseHandle();

        Cursor c = db.query(TABLE_DOMAIN_LIST, new String[]{"domain_guid", "id"}, "status = ?", new String[] {"new"}, null, null, null);
        try {
            Pair<String, Integer> guid = null;
            if (c.getCount() > 0) {
                c.moveToFirst();
                guid = new Pair<>(c.getString(c.getColumnIndex("domain_guid")), c.getInt(c.getColumnIndex("id")));
            }

            if (guid == null) {
                return;
            }

            System.out.println("Performing domain sync for domain: " + guid);

            fetchAndSyncDomain(guid);
        } finally {
            c.close();
            db.close();
        }
    }

    private void fetchAndSyncDomain(Pair<String, Integer> domainData) {
        String guid = domainData.first;
        final Pair<String, String> credentials = HubApplication._().getCredentials();

        String template = "https://www.commcarehq.org/a/" + guid + "/apps/api/list_apps/";
        if("".equals(credentials.first)) {
            ServicesMonitor.reportMessage("No user authentication available");
            return;
        }

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(credentials.first, credentials.second.toCharArray());
            }
        });

        try {

            URL url = new URL(template);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            WebUtil.setupGetConnection(conn);

            int response = conn.getResponseCode();

            if (response >= 500) {
                ServicesMonitor.reportMessage("Error logging in user" + conn.getResponseMessage());
                return;
            } else if (response >= 400) {
                ServicesMonitor.reportMessage("Auth error syncing user " + conn.getResponseMessage());
                return;
            } else if (response == 200) {
                try {
                    JSONObject data = WebUtil.readJsonResponse(conn);
                    JSONArray appList = data.getJSONArray("applications");
                    for(int i = 0 ; i < appList.length(); ++i) {
                        processApplication(appList.getJSONObject(i), domainData.second);
                    }


                    ContentValues cv = new ContentValues();
                    cv.put("status", "synced");
                    db.update(TABLE_DOMAIN_LIST, cv, "domain_guid = ?", new String[]{guid});
                    System.out.println("Synced domain: " + guid);
                }catch (JSONException e) {
                    e.printStackTrace();
                    ServicesMonitor.reportMessage("invalid domains response: " + e.getMessage());
                    return;
                }
            }
        } catch(IOException e) {
            ServicesMonitor.reportMessage("IO Exception syncing user metadata: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void processApplication(JSONObject application, int id) throws JSONException {
        ContentValues cv = new ContentValues();
        cv.put("app_guid", application.getString("app_id"));
        cv.put("domain_id", id);
        cv.put("app_descriptor", application.getString("name"));
        cv.put("version", application.getString("version"));
        cv.put("download_url", application.getString("download_url"));

        DbUtil.upsertRow(db, DbUtil.TABLE_APP_MANIFEST, cv, "app_guid");
    }
}
