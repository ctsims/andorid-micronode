package org.commcare.hub.server;

import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.apps.AppUtils;
import org.commcare.hub.util.StreamsUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ctsims on 2/11/2016.
 */
public class AppDownloadApi {
    public static final String API_STEP = "/apps/download/";
    public static final Pattern API_STRUCTURE =
            Pattern.compile("\\/apps\\/download\\/([0-9]+)\\/(.+)$");

    public static boolean handles(String uri) {
        return uri.startsWith(API_STEP);
    }

    public static String dispatch(String uri) throws IOException {
        Matcher m = API_STRUCTURE.matcher(uri);
        m.find();
        int appId = Integer.valueOf(m.group(1));
        String relativePath = m.group(2);

        AppAssetModel app = AppUtils.getAppModel(appId);

        File appFile = new File(app.getSandboxPath(), relativePath);

        if(!appFile.exists()) {
            throw new IOException("App install file not found at " + relativePath);
        }

        return StreamsUtil.loadToString(new BufferedInputStream(new FileInputStream(appFile)));
    }

    public static String getProfileURI(String apiRoot, AppAssetModel app) {
        return apiRoot + API_STEP + app.getRowId() + "/profile.ccpr";
    }
}
