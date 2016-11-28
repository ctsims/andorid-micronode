package org.commcare.hub.domain;

import android.content.ContentValues;
import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Base64;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.MobileUserModelBroadcast;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.events.HubEventBroadcast;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
import org.commcare.hub.util.DbUtil;
import org.commcare.hub.util.InvalidConfigException;
import org.commcare.hub.util.ProcessingException;
import org.commcare.hub.util.WebUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 *
 * Created by ctsims on 12/14/2015.
 */
public class DomainSyncThread extends HubRunnable {
    Context context;
    SQLiteDatabase db;
    private boolean domainsSynced = false;

    public static final String TABLE_DOMAIN_LIST = "DomainList";

    public DomainSyncThread(Context context) {
        this.context = context;
    }

    @Override
    public void runInternal() throws DatabaseUnavailableException {
        db = HubApplication._().getDatabaseHandle();
        Pair<String, Integer> domainData = null;

        if(!domainsSynced){
            try{
                fetchDomainList();
                domainsSynced = true;
            }catch(Exception e){
                ServicesMonitor.reportMessage(e.getMessage());
            }
        }

        Cursor c = db.query(TABLE_DOMAIN_LIST, new String[]{"domain_guid", "id", "pending_sync_request"}, "pending_sync_request != ?", new String[] {"synced"}, null, null, null);
        try {
            if(!c.moveToFirst() ){
                return;
            }
            domainData = new Pair<>(c.getString(c.getColumnIndex("domain_guid")), c.getInt(c.getColumnIndex("id")));
            String nextSyncRequest = c.getString(c.getColumnIndex("pending_sync_request"));

            System.out.println("Performing domain sync for domain: " + domainData.first);

            if("apps".equals(nextSyncRequest)) {
                fetchAndSyncApps(domainData);
            } else if("users".equals(nextSyncRequest)) {
                fetchAndSyncUsers(domainData);
            }
        } catch (ProcessingException e) {
            ServicesMonitor.reportMessage(e.getMessage());
        } catch (InvalidConfigException e) {
            ContentValues cv = new ContentValues();
            cv.put("pending_sync_request", "synced");
            db.update(TABLE_DOMAIN_LIST, cv, "domain_guid = ?", new String[]{domainData.first});
        } finally {
            c.close();
        }
    }

    private JSONObject fetch(String uri) throws ProcessingException, InvalidConfigException {
        final Pair<String, String> credentials = HubApplication._().getCredentials();

        if ("".equals(credentials.first)) {
            throw new ProcessingException("No user authentication available");
        }

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(credentials.first, credentials.second.toCharArray());
            }
        });

        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            WebUtil.setupGetConnection(conn, credentials);

            int response = conn.getResponseCode();

            if (response >= 500) {
                throw new ProcessingException("Error logging in user" + conn.getResponseMessage());
            } else if (response >= 400) {
                if(conn.getResponseMessage().contains("UNAUTHORIZED")) {
                    throw new InvalidConfigException("Domain is not capable of API requests");
                }
                throw new ProcessingException("Auth error syncing user " + conn.getResponseMessage());
            } else if (response == 200) {
                try {
                    return WebUtil.readJsonResponse(conn);
                } catch (JSONException e) {
                    throw new ProcessingException("invalid domains response: " + e.getMessage());
                }
            } else {
                throw new ProcessingException("Unexpcted web response: " + response);
            }
        } catch (IOException e) {
            throw new ProcessingException("IO Exception syncing user metadata: " + e.getMessage());
        }
    }

    private void fetchDomainList() throws ProcessingException, InvalidConfigException{
        String domainEndpoint = "https://www.commcarehq.org/hq/admin/web_user_data/";
        JSONObject domains = fetch(domainEndpoint);

        try{
            JSONArray domainList = domains.getJSONArray("domains");
            for(int i = 0; i < domainList.length(); i++){
                String domainName = domainList.getString(i);
                ContentValues cv = new ContentValues();
                cv.put("domain_guid", domainName);
                cv.put("pending_sync_request", "apps");
                db.insert(TABLE_DOMAIN_LIST, null, cv);
            }
        }catch(JSONException e){
            throw new ProcessingException(e.getMessage());
        }
    }


    private void fetchAndSyncUsers(Pair<String, Integer> domainData) throws ProcessingException, InvalidConfigException {
        String guid = domainData.first;

        String nextQuery ="?limit=20&format=json";

        while(nextQuery != null) {
            try {
                String uri = "https://www.commcarehq.org/" + "a/" + domainData.first +
                        "/api/v0.4/user/" + nextQuery;

                Pair<String, JSONArray> results = parseTastyPieResult(fetch(uri));

                JSONArray list = results.second;
                for (int i = 0; i < list.length(); ++i) {
                    processUser(list.getJSONObject(i), domainData.second);
                }
                nextQuery = results.first;
            } catch (JSONException e) {
                throw new ProcessingException(e.getMessage());
            }
        }


        ContentValues cv = new ContentValues();
        cv.put("pending_sync_request", "synced");
        db.update(TABLE_DOMAIN_LIST, cv, "domain_guid = ?", new String[]{guid});
        System.out.println("Synced domain: " + guid);
    }

    private void processUser(JSONObject jsonObject, int domainId) throws JSONException {
        ContentValues cv = new ContentValues();
        cv.put("user_id", jsonObject.getString("id"));
        cv.put("domain_id", domainId);
        cv.put("username", jsonObject.getString("username"));
        String firstName = jsonObject.getString("first_name");
        String lastName = jsonObject.getString("last_name");

        String readableName = jsonObject.getString("username").split("@")[0];

        if (!"".equals(firstName) || !"".equals(lastName)) {
            readableName = firstName + " " + lastName;
        }

        cv.put("readable_name", readableName);

        HubEventBroadcast.EventType type = HubEventBroadcast.EventType.Addition;
        Pair<Long, Boolean> upsertResult = DbUtil.upsertRow(db, DbUtil.TABLE_USER_LIST, cv, "user_id");

        if(!upsertResult.second) {
            type = HubEventBroadcast.EventType.Update;
        }

        this.getServiceConnector().queueBroadcast(new MobileUserModelBroadcast(domainId, upsertResult.first, type));
    }

    private Pair<String, JSONArray> parseTastyPieResult(JSONObject data) throws JSONException {
        String nextDataUri = data.getJSONObject("meta").getString("next");
        if("null".equals(nextDataUri)) {
            nextDataUri = null;
        }
        JSONArray values = data.getJSONArray("objects");
        return new Pair<>(nextDataUri, values);
    }


    private void fetchAndSyncApps(Pair<String, Integer> domainData) throws ProcessingException, InvalidConfigException {
        String guid = domainData.first;
        String template = "https://commcarehq.org/a/" + guid + "/apps/api/list_apps/";

        JSONObject data = fetch(template);

        try {
            JSONArray appList = data.getJSONArray("applications");
            for (int i = 0; i < appList.length(); ++i) {
                processApplication(appList.getJSONObject(i), domainData.second);
            }
        } catch (JSONException e) {
            throw new ProcessingException(e.getMessage());
        }


        ContentValues cv = new ContentValues();
        cv.put("pending_sync_request", "users");
        db.update(TABLE_DOMAIN_LIST, cv, "domain_guid = ?", new String[]{guid});
        System.out.println("Synced domain: " + guid);
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
