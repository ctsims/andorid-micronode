package org.commcare.hub.server;

import org.commcare.hub.apps.AppModel;
import org.commcare.hub.apps.AppUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * Created by ctsims on 2/11/2016.
 */
public class ManifestApi {
    public static boolean handles(String uri) {
        return uri.startsWith("/apps/manifest");
    }

    public static String dispatch(String uri) throws IOException{
        String apiRoot = ServerService.getApiRoot();

        Map<String, AppModel> apps = AppUtils.getUniqueInstalledApps();

        try {
            String appsJson = appsToJson(apiRoot, apps);
            return appsJson;
        } catch(JSONException e ) {
            throw new RuntimeException("Invalid JSON while serializing manifest", e);
        }
    }

    private static String appsToJson(String apiRoot, Map<String, AppModel> apps) throws JSONException {
        JSONObject response = new JSONObject();
        JSONArray applications = new JSONArray();

        for (AppModel app : apps.values()) {
            JSONObject appJson = new JSONObject();
            appJson.put("version", app.getVersion());
            appJson.put("name", app.getAppName());
            appJson.put("app_id", app.getAppId());
            appJson.put("download_url", "");
            appJson.put("profile_url", AppDownloadApi.getProfileURI(apiRoot, app));

            applications.put(appJson);
        }


        response.put("applications", applications);
        return response.toString();
    }
}
