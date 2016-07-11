package org.commcare.hub.events;

/**
 * Created by ctsims on 7/11/2016.
 */
public interface HubBroadcastListener {
    public void receiveBroadcast(HubEventBroadcast b);
}
