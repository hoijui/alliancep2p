package org.alliance.core.node;

import org.alliance.core.T;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 17:31:19
 */
public class FriendConnector extends Thread {
    private FriendManager manager;
    private boolean alive = true;

    public FriendConnector(FriendManager manager) {
        this.manager = manager;
        setDaemon(true);
        setName("FriendConnector -- "+manager.getMe().getNickname());
        setPriority(MIN_PRIORITY);
    }

    public void run() {
        while(alive) {
            ArrayList<Friend> al = new ArrayList<Friend>(manager.friends());
            for(Friend f : al) {
                while(manager.getCore().getNetworkManager().getNetworkLayer().getNumberOfPendingConnections() > manager.getSettings().getInternal().getMaxpendingconnections()) {
                    try { Thread.sleep(1000); } catch(InterruptedException e) {}
                }
                if (!f.isConnected()) {
                    try {
                        if(T.t)T.trace("Friendconnector trying to connect to "+f+" "+f.getFriendConnection());
                        try { Thread.sleep(100); } catch(InterruptedException e) {}
                        manager.connect(f);
                    } catch(IOException e) {
                        if(T.t)T.trace("Friend unreachable: "+e);
                    }
                }
            }
            try { Thread.sleep(manager.getSettings().getInternal().getReconnectinterval()*1000); } catch(InterruptedException e) {}
        }
    }

    public void kill() {
        alive = false;
    }

    public void wakeup() {
        interrupt();
    }

    public void wakeupIn(final int ms) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(ms); } catch(InterruptedException e) {}
                wakeup();
            }
        });
        t.setName("Wakeup thread for FriendConnector. Should wait: "+ms);
        t.setDaemon(true);
        t.start();
    }
}
