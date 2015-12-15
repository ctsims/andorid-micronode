package org.commcare.hub.services;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ServiceHost {
    HubService[] services;

    public ServiceHost(HubService[] services) {
        this.services = services;
    }

    public void start() {
        for(HubService service : services) {
            service.startService();
        }
    }
}
