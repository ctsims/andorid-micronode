package org.commcare.hub.services;

/**
 * Created by ctsims on 12/14/2015.
 */
public class HubThreadService implements HubService {
    Status lastStatus = Status.WAITING;
    Thread service;

    HubRunnable runnable;

    public HubThreadService(HubRunnable runnable) {
        this.runnable = runnable;
        runnable.connect(this);
    }

    @Override
    public Status getServiceStatus() {
        return lastStatus;
    }

    @Override
    public void startService() {
        Thread service = new Thread(runnable);
        service.start();
    }

    @Override
    public void stopService() {
        if(service.isAlive()) {
            this.lastStatus = Status.STOPPING;
            runnable.kill();
        }
    }

    public void updateStatus(Status status) {
        this.lastStatus = status;
    }
}
