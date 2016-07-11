package org.commcare.hub.services;

import org.commcare.hub.database.DatabaseUnavailableException;

/**
 * Created by ctsims on 12/14/2015.
 */
public abstract class HubRunnable<T extends HubThreadService> implements Runnable {
    public static int POLL_DELAY = 500;

    private boolean killFlagSet = false;

    private int status;

    T service;

    protected T getServiceConnector() {
        return service;
    }

    public void connect(T service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.updateStatus(HubService.Status.RUNNING);
        while(!killFlagSet) {
            try {
                runInternal();
            } catch (Exception e) {

            }
            try {
                Thread.sleep(POLL_DELAY);
            } catch (InterruptedException e) {
            }
        }
        service.updateStatus(HubService.Status.STOPPED);
    }

    public void kill() {
        service.updateStatus(HubService.Status.STOPPED);
        killFlagSet = true;
    }

    public abstract void runInternal() throws DatabaseUnavailableException;
}
