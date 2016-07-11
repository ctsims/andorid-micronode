package org.commcare.hub.util;

import android.content.Context;
import android.util.Base64;

import org.commcare.hub.mirror.SyncableUser;
import org.commcare.hub.monitor.ServicesMonitor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Created by ctsims on 2/11/2016.
 */
public class WebUtil {
    public static void setupGetConnection(HttpURLConnection con) throws IOException {
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setReadTimeout(CONNECTION_SO_TIMEOUT);
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setInstanceFollowRedirects(true);
    }

    /**
     * How long to wait when opening network connection in milliseconds
     */
    public static final int CONNECTION_TIMEOUT = 2 * 60 * 1000;

    /**
     * How long to wait when receiving data (in milliseconds)
     */
    public static final int CONNECTION_SO_TIMEOUT = 1 * 60 * 1000;


    public static JSONObject readJsonResponse(HttpURLConnection conn) throws JSONException, IOException {
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
            throw e;
        }
        return data;
    }

}
