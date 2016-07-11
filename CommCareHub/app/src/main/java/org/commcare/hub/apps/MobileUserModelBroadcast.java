package org.commcare.hub.apps;

import org.commcare.hub.events.HubEventBroadcast;

/**
 * Created by ctsims on 7/11/2016.
 */
public class MobileUserModelBroadcast implements HubEventBroadcast {

    private int domainId = -1;
    private int userId = -1;

    EventType eventType;

    public MobileUserModelBroadcast(int domainId, long userId, EventType type){
        this.domainId = domainId;
        this.userId = (int)userId;
        this.eventType = type;

    }

    public int getDomainId() {
        return domainId;
    }

    public int getUserId() {
        return userId;
    }

    public EventType getEventType() {
        return eventType;
    }

}
