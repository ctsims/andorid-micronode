package org.commcare.hub.services;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.events.HubActivity;
import org.commcare.hub.events.HubEventBroadcast;

import java.util.ArrayList;

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
        service = new Thread(runnable);
        service.start();
    }

    @Override
    public void stopService() {
        if(service.isAlive()) {
            this.lastStatus = Status.STOPPING;
            runnable.kill();
        }
    }

    @Override
    public void restartService() {
        if(service.isAlive()) {
            this.lastStatus = Status.STOPPING;
            runnable.kill();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startService();
            }
        }).start();
    }

    public void updateStatus(Status status) {
        this.lastStatus = status;
    }

    public void queueBroadcast(HubEventBroadcast b) {
        HubApplication._().getServiceHost().broadcast(b);
    }
}
