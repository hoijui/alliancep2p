package org.alliance.ui;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.node.Friend;

import javax.swing.*;
import java.util.Collection;

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
        for(Friend f : c) {
            if (f.isConnected()) addElement(f);
        }
        for(Friend f : c) if (!f.isConnected()) addElement(f);
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
