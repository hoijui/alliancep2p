package org.alliance.core.node;

import com.stendahls.util.TextUtils;
import org.alliance.core.T;
import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.rpc.GracefulClose;
import org.alliance.core.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:30:21
 */
public class Friend extends Node {
    private ArrayList<Connection> connections = new ArrayList<Connection>();
    private String lastKnownHost;
    private int lastKnownPort;
    private FriendManager manager;
    private FriendConnection friendConnection;
    private boolean newlyDiscoveredFriend; //true when friend was recently found using invitation

    public Friend(FriendManager manager, org.alliance.core.settings.Friend f) {
        nickname = f.getNickname();
        guid = f.getGuid();
        lastKnownHost = f.getHost();
        lastKnownPort = f.getPort();
        this.manager = manager;
    }

    public Friend(FriendManager manager, String nickname, int guid) {
        this.nickname = nickname;
        this.guid = guid;
        this.manager = manager;
    }

    public FriendConnection getFriendConnection() {
        return friendConnection;
    }

    public void addConnection(AuthenticatedConnection c) throws IOException {
        connections.add(c);
        if (c instanceof FriendConnection) {
            friendConnection = (FriendConnection)c;
            manager.getCore().getNetworkManager().getDownloadManager().signalFriendWentOnline(this);
        }
    }

    public void updateLastKnownHostInfo(String host, int port) throws IOException {
        if(T.t)T.info("Updating host info for "+this+": "+host+":"+port);
        lastKnownHost = host;
        lastKnownPort = port;
        try {
            saveLastKnownInfo();
        } catch(Exception e) {
            if(T.t)T.error("Could not save settings! "+e);
            e.printStackTrace();
        }
    }

    private void saveLastKnownInfo() throws Exception {
        Settings s = manager.getSettings();

        if (TextUtils.isIpNumber(s.getFriend(guid).getHost()) || !TextUtils.isIpNumber(lastKnownHost)) {
            s.getFriend(guid).setHost(lastKnownHost);
        }
        s.getFriend(guid).setPort(lastKnownPort);
    }

    public boolean isConnected() {
        return friendConnection != null;
    }

    public void removeConnection(AuthenticatedConnection ac) {
        connections.remove(ac);
        if (ac == friendConnection) {
            friendConnection = null;
            for(Connection c : connections) if (c instanceof FriendConnection) friendConnection = (FriendConnection)c;
            if (friendConnection == null) {
                if(T.t)T.info("Lost connection to "+this+". Closing all other connections too. Have to do this in order to be able to start a new donwload connnection to this friend (if he reconnects)");
                ArrayList<Connection> al = new ArrayList<Connection>(connections);
                for(Connection c: al) {
                    try {
                        if(T.t)T.info("Closing: "+c);
                        c.close();
                    } catch(IOException e) {
                        manager.getCore().reportError(e, this);
                    }
                }
            }
        }
    }

    public String getLastKnownHost() {
        return lastKnownHost;
    }

    public int getLastKnownPort() {
        return lastKnownPort;
    }

    public boolean hasMultipleFriendConnections() {
        int n = 0;
        for(Connection c : connections) if (c instanceof FriendConnection) n++;
        return n > 1;
    }

    public boolean isNewlyDiscoveredFriend() {
        return newlyDiscoveredFriend;
    }

    public void setNewlyDiscoveredFriend(boolean newlyDiscoveredFriend) {
        this.newlyDiscoveredFriend = newlyDiscoveredFriend;
    }

    public void disconnect() throws IOException {
        if (friendConnection != null) friendConnection.send(new GracefulClose(GracefulClose.DELETED));
    }
}
