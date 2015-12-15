package org.commcare.hub.services;

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
}
