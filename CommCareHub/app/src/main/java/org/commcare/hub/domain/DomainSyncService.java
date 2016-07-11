package org.commcare.hub.domain;

import android.content.Context;

import org.commcare.hub.services.HubThreadService;
import org.commcare.hub.sync.ServerMetadataSyncThread;

/**
 * Created by ctsims on 12/14/2015.
 */
public class DomainSyncService extends HubThreadService {

    public DomainSyncService(Context c) {
        super(new DomainSyncThread(c));
    }
}
