package org.commcare.hub.sync;

import android.content.Context;

import org.commcare.hub.services.HubThreadService;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ServerMetadataSyncService extends HubThreadService {
    public static enum ServiceStatus {
        FailedAuthentication,
        ConnectedAndSyncing
    }

    protected ServiceStatus syncStatus;

    public ServerMetadataSyncService(Context c) {
        super(new ServerMetadataSyncThread(c));
    }

    public void setSyncStatus(ServiceStatus status) {
        this.syncStatus = status;
    }

    public ServiceStatus getStatus() {
        return this.syncStatus;
    }
}
