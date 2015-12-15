package org.commcare.hub.services;

import org.commcare.hub.database.DatabaseUnavailableException;

/**
 * Created by ctsims on 12/14/2015.
 */
public abstract class HubRunnable implements Runnable {
    public static int POLL_DELAY = 500;

    private boolean killFlagSet = false;

    private int status;

    HubThreadService service;

    public void connect(HubThreadService service) {
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
    }

    public void kill() {
        killFlagSet = true;
    }

    public abstract void runInternal() throws DatabaseUnavailableException;
}
