package org.commcare.hub.monitor;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ServicesMonitor {
    private static final int MESSAGES_LENGTH = 100;

    static Queue<String> messages = new LinkedList<String>();

    public synchronized static void reportMessage(String message) {
        if(messages.size() > MESSAGES_LENGTH) {
            messages.remove();
        }
        messages.add(message);
    }

    public static String getMessages() {
        String ret = "";
        for(String message : messages) {
            ret += message + "\n";
        }
        return ret;
    }
}
