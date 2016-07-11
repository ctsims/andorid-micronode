package org.commcare.hub.events;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.commcare.hub.application.HubApplication;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by ctsims on 7/11/2016.
 */
public abstract class HubActivity extends AppCompatActivity implements HubBroadcastListener {

    public final Object queueLock = new Object();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HubApplication._().getServiceHost().setReceiver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        HubApplication._().getServiceHost().unsetReceiver(this);
    }

    public void clearBroadcastQueue() {
        synchronized (queueLock) {

        }
    }


    public void addToBroadcastQueue(HubEventBroadcast list) {
        synchronized (queueLock) {
            queue.add(list);
        }
        flushQueue();
    }

    public void receiveBroadcast(HubEventBroadcast b) {
        FragmentManager manager = this.getSupportFragmentManager();
        for(Fragment f : manager.getFragments()) {
            if(f instanceof HubBroadcastListener) {
                if(f.isVisible()) {
                    ((HubBroadcastListener)f).receiveBroadcast(b);
                }
            }
        }
    }

    ArrayList<HubEventBroadcast> queue = new ArrayList<>();

    public void flushQueue() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HubEventBroadcast> toClear;
                synchronized (queueLock) {
                    toClear = new ArrayList<>();
                    toClear.addAll(queue);
                }
                for(HubEventBroadcast b : toClear) {
                    receiveBroadcast(b);
                }
            }
        });
    }
}
