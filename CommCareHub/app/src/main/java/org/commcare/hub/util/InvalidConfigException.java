package org.commcare.hub.util;

/**
 * An operation won't and can't succeed due to an invalid configuration. Don't try it again.
 *
 * Created by ctsims on 7/11/2016.
 */
public class InvalidConfigException extends Exception {
    public InvalidConfigException(String message) {
        super(message);
    }
}
