package org.commcare.hub.apps;

import android.content.Context;

import org.commcare.hub.services.HubThreadService;

/**
 * Created by ctsims on 12/14/2015.
 */
public class AppSyncService extends HubThreadService {
    public AppSyncService(Context c) {
        super(new AppSyncThread(c));
    }
}
