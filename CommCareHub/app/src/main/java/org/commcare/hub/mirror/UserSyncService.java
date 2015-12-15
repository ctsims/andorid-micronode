package org.commcare.hub.mirror;

import android.content.Context;

import org.commcare.hub.services.HubThreadService;

/**
 * Created by ctsims on 12/14/2015.
 */
public class UserSyncService extends HubThreadService {
    public UserSyncService(Context c) {
        super(new UserSyncThread(c));
    }
}
