package org.alliance.ui;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-27
 * Time: 19:50:47
 * To change this template use File | Settings | File Templates.
 */
public class FriendListModel extends DefaultListModel implements Runnable {
    private CoreSubsystem core;
    private boolean updateNeeded;
    private boolean ignoreFires;

    public FriendListModel(CoreSubsystem core) {
        this.core = core;
        updateFriendList();
        Thread t = new Thread(this, "Friend list update thread");
        t.start();
    }

    private void updateFriendList() {
        ignoreFires = true;
        clear();
        Collection<Friend> c = new ArrayList<Friend>(core.getFriendManager().friends());

        TreeSet<Node> ts = new TreeSet<Node>(new Comparator<Node>() {
            public int compare(Node o1, Node o2) {
                if (o1 == null || o2 == null) return 0;
                String s1 = o1.nickname();
                String s2 = o2.nickname();
                if (s1.equalsIgnoreCase(s2)) return o1.getGuid()-o2.getGuid();
                return o1.nickname().compareToIgnoreCase(o2.nickname());
            }
        });
        for(Friend f : c) {
            ts.add(f);
        }
        ts.add(core.getFriendManager().getMe());
        int max = core.getFriendManager().getNumberOfInvitesNeededToBeKing();
        for(Node f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() >= max) addElement(f);
        }
        for(Node f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() >= 3 && f.getNumberOfInvitedFriends() < max) addElement(f);
        }
        for(Node f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() > 0 && f.getNumberOfInvitedFriends() < 3) addElement(f);
        }
        for(Node f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() <= 0) addElement(f);
        }
        for(Node f : ts) if (!f.isConnected() && !f.hasNotBeenOnlineForLongTime()) addElement(f);
        for(Node f : ts) if (!f.isConnected() && f.hasNotBeenOnlineForLongTime()) addElement(f);
        ignoreFires = false;
    }

    public void run() {
        while(true) {
            if (updateNeeded) {
                updateNeeded = false;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateFriendList();
                    }
                });
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }
    }

    public void signalFriendChanged(Friend node) {
        updateNeeded = true;
    }

    public void signalFriendAdded(Friend friend) {
        updateNeeded = true;
    }


    protected void fireContentsChanged(Object source, int index0, int index1) {
        if (ignoreFires) return;
        super.fireContentsChanged(source, index0, index1);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected void fireIntervalAdded(Object source, int index0, int index1) {
        if (ignoreFires) return;
        super.fireIntervalAdded(source, index0, index1);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        if (ignoreFires) return;
        super.fireIntervalRemoved(source, index0, index1);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
