package org.alliance.core;

import java.io.IOException;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-17
 * Time: 16:35:28
 * To change this template use File | Settings | File Templates.
 */
public class AwayManager extends Manager implements Runnable {
    private Thread thread;
    private boolean away = false;
    private CoreSubsystem core;

    public AwayManager(CoreSubsystem core) {
        this.core = core;
    }

    public void init() throws IOException, Exception {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        try {
            long lastTimeMouseMoved = System.currentTimeMillis();
            while(true) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                Thread.sleep(1000);
                if (p.equals(MouseInfo.getPointerInfo().getLocation())) {
                    //mouse has not moved
                    if (System.currentTimeMillis()-lastTimeMouseMoved > core.getSettings().getInternal().getSecondstoaway()*1000) {
                        updateAway(true);
                    } 
                } else {
                    //mouse has moved
                    updateAway(false);
                    lastTimeMouseMoved = System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private void updateAway(boolean b) {
        if (b != away) {
            away = b;
            core.invokeLater(new Runnable() {
                public void run() {
                    try {
                        core.informFriendsOfAwayStatus(away);
                    } catch (IOException e) {
                        core.reportError(e, this);
                    }
                }
            });
        }
    }

    public void shutdown() {
        thread.interrupt();
    }
}
