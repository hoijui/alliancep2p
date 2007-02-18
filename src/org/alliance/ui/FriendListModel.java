package org.alliance.ui;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.node.Friend;

import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-27
 * Time: 19:50:47
 * To change this template use File | Settings | File Templates.
 */
public class FriendListModel extends DefaultListModel {
    private CoreSubsystem core;

    public FriendListModel(CoreSubsystem core) {
        this.core = core;
        updateFriendList();
    }

    private void updateFriendList() {
        clear();
        Collection<Friend> c = core.getFriendManager().friends();

        TreeSet<Friend> ts = new TreeSet<Friend>(new Comparator<Friend>() {
            public int compare(Friend o1, Friend o2) {
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
        for(Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() > 0) addElement(f);
        }
        for(Friend f : ts) {
            if (f.isConnected() && f.getNumberOfInvitedFriends() == 0) addElement(f);
        }
        for(Friend f : ts) if (!f.isConnected() && !f.hasNotBeenOnlineForLongTime()) addElement(f);
        for(Friend f : ts) if (!f.isConnected() && f.hasNotBeenOnlineForLongTime()) addElement(f);
    }

    public void signalFriendChanged(Friend node) {
        updateFriendList();
//        fireContentsChanged(this, indexOf(node), indexOf(node));
    }

    public void signalFriendAdded(Friend friend) {
        updateFriendList();
//        if (friend.isConnected())
//            insertElementAt(friend, 0);
//        else
//            addElement(friend);
    }
}
