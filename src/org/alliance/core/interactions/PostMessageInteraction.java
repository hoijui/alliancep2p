package org.alliance.core.interactions;

import org.alliance.core.SynchronizedNeedsUserInteraction;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:20:31
 * To change this template use File | Settings | File Templates.
 */
public class PostMessageInteraction extends SynchronizedNeedsUserInteraction {
    private String message;
    private long tick;
    private int fromGuid;

    public PostMessageInteraction(String message, int fromGuid) {
        this.message = message;
        this.fromGuid = fromGuid;
        this.tick = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public long getTick() {
        return tick;
    }

    public int getFromGuid() {
        return fromGuid;
    }
}
