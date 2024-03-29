package org.alliance.core.node;

/**
 *
 * ONE INSTENCE per FRIEND created right now. is this the right way to go?
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-29
 * Time: 13:51:42
 */
public class UntrustedNode extends Node {
    private boolean connected;

    private UntrustedNode(String nickname, int guid, boolean connected) {
        super(nickname, guid);
        this.connected = connected;
    }

    public static UntrustedNode loadOrCreate(FriendManager man, String nickname, int guid, boolean connected) {
        if (man.getUntrustedNode(guid) == null) {
            UntrustedNode n = new UntrustedNode(nickname, guid, connected);
            man.addUntrustedNode(n);
            return n;
        } else return man.getUntrustedNode(guid);
    }

    /**
     * @return If this node has a connection to its parent
     */
    public boolean isConnected() {
        return connected;
    }

    public int getNumberOfInvitedFriends() {
        return 0;
    }

    public boolean hasNotBeenOnlineForLongTime() {
        return false;
    }

    public long getLastSeenOnlineAt() {
        return -1; //unknown
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
