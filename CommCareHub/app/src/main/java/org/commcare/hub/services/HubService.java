package org.commcare.hub.services;

import org.commcare.hub.events.HubEventBroadcast;

/**
 * Created by ctsims on 12/14/2015.
 */
public interface HubService {
    public static enum Status {
        WAITING,
        STOPPING,
        STOPPED,
        RUNNING,
        ERROR
    }


    public Status getServiceStatus();

    public void startService();

    public void stopService();

    public void restartService();
}
