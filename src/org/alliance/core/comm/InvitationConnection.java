package org.alliance.core.comm;

import org.alliance.Version;
import org.alliance.core.interactions.FriendAlreadyInListUserInteraction;
import org.alliance.core.node.MyNode;
import org.alliance.core.settings.Friend;
import org.alliance.core.settings.Server;

import java.io.IOException;

/**
 *
 * This connection swings both ways - it's used by invitor and invited
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 19:47:04
 * To change this template use File | Settings | File Templates.
 */
public class InvitationConnection extends AuthenticatedConnection {
    public static final int CONNECTION_ID=4;
    private int passkey;

    public InvitationConnection(NetworkManager netMan, Direction direction, int passkey) {
        super(netMan, direction);
        this.passkey = passkey;
    }

    public InvitationConnection(NetworkManager netMan, Direction direction, Object key,  int passkey) {
        super(netMan, direction, key);
        this.passkey = passkey;

        if(T.t)T.ass(direction == Direction.IN, "Only supports incoming connections!");
        sendMyInfoWrapped();
    }

    public void sendConnectionIdentifier() throws IOException {
        if(T.t)T.trace("Sending special authentication for InvitationConnection");
        Packet p = netMan.createPacketForSend();
        p.writeInt(Version.PROTOCOL_VERSION);
        p.writeByte((byte)getConnectionIdForRemote());
        p.writeInt(passkey);
        send(p);

        if(T.t)T.ass(direction == Direction.OUT, "Only supports outgoing connections!");
        sendMyInfoWrapped();
    }

    private void sendMyInfoWrapped() {
        try {
            sendMyInfo();
        } catch(IOException e) {
            core.reportError(e, this);
        }
    }
    private void sendMyInfo() throws IOException {

        if(T.t)T.info("Sending my info because remote had correct invitation passkey");
        Packet p = netMan.createPacketForSend();
        p.writeInt(core.getFriendManager().getMyGUID());

        Server server = core.getSettings().getServer();
        MyNode me = core.getFriendManager().getMe();

        if (server.getHostname() != null) {
            p.writeUTF(server.getHostname());
        } else {
            p.writeUTF(me.getExternalIp(core));
        }

        p.writeInt(server.getPort());

        p.writeUTF(me.getNickname());

        send(p);
    }

    public void packetReceived(Packet p) throws IOException {
        if(T.t)T.info("Received info of new friend!");
        int guid = p.readInt();
        String host = p.readUTF();
        int port = p.readInt();
        String name = p.readUTF();

        Friend newFriend = new Friend(name, host, guid, port);
        for(Friend f : core.getSettings().getFriendlist()) if (f.getGuid() == guid) {
            org.alliance.core.node.Friend friend = core.getFriendManager().getFriend(f.getGuid());
            if (friend != null && !friend.isConnected()) {
                friend.updateLastKnownHostInfo(host, port);
                core.getFriendManager().getFriendConnector().wakeup();
            }
            core.queNeedsUserInteraction(new FriendAlreadyInListUserInteraction(newFriend.getGuid()));
            return;
        }

        core.getSettings().getFriendlist().add(newFriend);
        try {
            core.saveSettings();
            core.getFriendManager().addFriend(newFriend, true);
            core.getFriendManager().runFriendConnectorIn((int)(Math.random()*1000+1000));
        } catch(Exception e) {
            core.reportError(e, this);
        }
    }

    protected int getConnectionId() {
        return CONNECTION_ID;
    }
}
