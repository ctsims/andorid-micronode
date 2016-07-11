package org.commcare.hub.sync;

import android.content.ContentValues;
import android.content.Context;
import android.support.v4.util.Pair;

import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
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
public class ServerMetadataSyncThread extends HubRunnable<ServerMetadataSyncService> {
    Context context;

    public ServerMetadataSyncThread(Context context) {
        this.context = context;
    }

    public void runInternal() throws DatabaseUnavailableException {
        fetchAndUpdateDomainList();
    }

    private void fetchAndUpdateDomainList() {
        final Pair<String, String> credentials = HubApplication._().getCredentials();

        String template = "https://www.commcarehq.org/hq/admin/api/global/web-user/?format=json&username=" + credentials.first;

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
                this.getServiceConnector().setSyncStatus(ServerMetadataSyncService.ServiceStatus.FailedAuthentication);
                this.getServiceConnector().stopService();
                return;
            } else if (response == 200) {
                this.getServiceConnector().setSyncStatus(ServerMetadataSyncService.ServiceStatus.ConnectedAndSyncing);

                BufferedReader streamReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                JSONObject data;
                try {
                    data = new JSONObject(responseStrBuilder.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    ServicesMonitor.reportMessage("Invalid JSON syncing domains: " + e.getMessage());
                    return;
                }


                try {
                    JSONArray domainList = data.getJSONArray("objects").getJSONObject(0).getJSONArray("domains");
                    ArrayList<String> domains = new ArrayList<>();
                    for(int i = 0 ; i < domainList.length(); ++i) {
                        String d = (String)domainList.get(i);
                     domains.add(d);
                    }
                    syncDomains(domains);
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

    public static final String STATUS_NEW ="new";
    public static final String STATUS_SYNCED = "synced";

    private void syncDomains(ArrayList<String> domains) {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();

        for(String s : domains) {
            ContentValues cv = new ContentValues();
            cv.put("domain_guid", s);
            cv.put("status", STATUS_NEW);
            database.insertWithOnConflict("DomainList", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }
}
