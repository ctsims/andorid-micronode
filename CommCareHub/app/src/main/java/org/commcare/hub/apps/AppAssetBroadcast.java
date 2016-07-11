package org.commcare.hub.apps;

import org.commcare.hub.events.HubEventBroadcast;

/**
 * Created by ctsims on 7/11/2016.
 */
public class AppAssetBroadcast implements HubEventBroadcast {

    private int appManifestId = -1;

    public AppAssetBroadcast(int appManifestId){
        this.appManifestId = appManifestId;
    }

    public int getAppManifestId() {
        return appManifestId;
    }
}
