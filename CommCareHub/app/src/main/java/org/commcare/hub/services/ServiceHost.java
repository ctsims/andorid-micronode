package org.commcare.hub.services;

import org.commcare.hub.events.HubActivity;
import org.commcare.hub.events.HubBroadcastListener;
import org.commcare.hub.events.HubEventBroadcast;
import org.commcare.hub.sync.ServerMetadataSyncService;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ServiceHost {
    HubService[] services;

    private HubActivity broadcastReceiver;
    private final Object broadcastLock = new Object();

    public ServiceHost(HubService[] services) {
        this.services = services;
    }

    public void start() {
        for(HubService service : services) {
            service.startService();
        }
    }

    public void restart(Class<?> service) {
        for(HubService s : services) {
            if(service.isAssignableFrom(s.getClass())) {
                s.restartService();
            }
        }
    }

    public void setReceiver(HubActivity broadcastReceiver) {
        synchronized (broadcastLock) {
            this.broadcastReceiver = broadcastReceiver;
        }
    }

    public void unsetReceiver(HubActivity broadcastReceiver) {
        synchronized (broadcastLock) {
            if (this.broadcastReceiver == broadcastReceiver) {
                this.broadcastReceiver = null;
                broadcastReceiver.clearBroadcastQueue();
            }
        }
    }

    public void broadcast(HubEventBroadcast broadcast) {
        synchronized (broadcastLock) {
            if(broadcastReceiver!= null ) {
                broadcastReceiver.addToBroadcastQueue(broadcast);
            }
        }
    }
}
