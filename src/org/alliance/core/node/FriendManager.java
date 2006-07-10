package org.alliance.core.node;

import org.alliance.core.BroadcastManager;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.Manager;
import org.alliance.core.comm.*;
import org.alliance.core.comm.rpc.*;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.settings.My;
import org.alliance.core.settings.Settings;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * The FriendManager keeps track of all nodes. Contains a list of friends and a list of all nodes
 * (Friend extends Node so friends are nodes too).
 * <p>
 * Launches the FriendConnector that tries to connect to disconnected friends reguraly (using a separate thread).
 * <p>
 * Use the FriendManager to manage information about nodes.
 * <p>
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:30:09
 */
public class FriendManager extends Manager {
    private Settings settings;
    private FriendConnector friendConnector;
    private BroadcastManager broadcastManager = new BroadcastManager();

    private HashMap<Integer, Friend> friends = new HashMap<Integer, Friend>();
    private HashMap<Integer, UntrustedNode> untrustedNodes = new HashMap<Integer, UntrustedNode>();

    private CoreSubsystem core;

    private MyNode me;
    private NetworkManager netMan;

    public FriendManager(CoreSubsystem core, Settings settings) throws Exception {
        this.settings = settings;
        this.core = core;
    }

    public void init() throws Exception {
        netMan = core.getNetworkManager();
        setupGUID();
        setupFriends();
        setupFriendConnector();
    }

    private void setupFriendConnector() {
        friendConnector = new FriendConnector(this);
        friendConnector.start();
    }

    public int getMyGUID() {
        return settings.getMy().getGuid();
    }

    private void setupGUID() throws Exception {
        if (settings.getMy() == null || settings.getMy().getGuid() == null) {
            if(org.alliance.core.T.t)org.alliance.core.T.info("Generating GUID for user.");
            if (settings.getMy() == null) settings.setMy(new My());
            Random r = new Random(8682522807148012L+System.nanoTime());
            settings.getMy().setGuid(r.nextInt());
            core.saveSettings();
        } else {
            if(org.alliance.core.T.t)org.alliance.core.T.info("User: "+settings.getMy().getNickname()+" - "+settings.getMy().getGuid());
        }
    }

    private void setupFriends() throws Exception {
        if(org.alliance.core.T.t)org.alliance.core.T.info("Setting up friends...");
        me = new MyNode(settings.getMy().getNickname(), settings.getMy().getGuid());
        me.setShareSize(core.getFileManager().getTotalBytesShared());

        for(org.alliance.core.settings.Friend f : settings.getFriendlist()) addFriend(f, false);
    }

    public void addFriend(org.alliance.core.settings.Friend f, boolean foundFriendUsingInvitation) throws Exception {
        if (f.getGuid() == me.getGuid()) {
            if(org.alliance.core.T.t)org.alliance.core.T.warn("You have yourself in your friendlist!");
        } else if (f.getNickname() == null) {
            throw new Exception("No nickname for guid: "+f.getGuid());
        } else {
            if(org.alliance.core.T.t)org.alliance.core.T.info("Found "+f.getNickname()+". GUID: "+f.getGuid());
            org.alliance.core.node.Friend friend = new org.alliance.core.node.Friend(this, f);
            if (friend.getGuid() == getMyGUID()) throw new Exception("You have configured a friend that has your own GUID.");
            friends.put(f.getGuid(), friend);
            friend.setNewlyDiscoveredFriend(foundFriendUsingInvitation);
            core.getUICallback().signalFriendAdded(friend);
            if (foundFriendUsingInvitation) sendMyInfoToAllMyFriends();
        }
    }

    private void sendMyInfoToAllMyFriends() throws IOException {
        netMan.sendToAllFriends(new UserInfo());
        netMan.sendToAllFriends(new UserList());
    }

    /**
     * Callback from netMan
     */
    public void connectionEstablished(AuthenticatedConnection c) throws IOException {
        if (getFriend(c.getRemoteUserGUID()) != null) {
            getFriend(c.getRemoteUserGUID()).addConnection(c);
            getNetMan().getPackageRouter().updateRouteTable(getFriend(c.getRemoteUserGUID()), c.getRemoteUserGUID(), 0);
        } else {
            if(org.alliance.core.T.t)org.alliance.core.T.ass(c instanceof InvitationConnection,"Not an invitation connection!");
        }
    }

    public Friend getFriend(int guid) {
        return friends.get(guid);
    }

    public Friend getFriend(String nickname) {
        for(Friend f : friends.values()) if (f.getNickname().equals(nickname)) return f;
        return null;
    }

    /**
     * Callback from netMan
     */
    public void connectionClosed(Connection connection) {
        if (connection instanceof AuthenticatedConnection) {
            AuthenticatedConnection c = (AuthenticatedConnection)connection;
            if (getFriend(c.getRemoteUserGUID()) != null) {
                getFriend(c.getRemoteUserGUID()).removeConnection(c);
                core.getUICallback().nodeOrSubnodesUpdated(getFriend(c.getRemoteUserGUID()));
            }
        }
    }

    public void ping() throws IOException {
        netMan.sendToAllFriends(new Ping());
    }

    public NetworkManager getNetMan() {
        return netMan;
    }

    public Collection<Friend> friends() {
        return friends.values();
    }

    public Settings getSettings() {
        return settings;
    }

    public void connect(Friend f) throws IOException {
        if (f.isConnected()) {
            if(org.alliance.core.T.t)org.alliance.core.T.warn("Already connected!");
            return;
        }
        netMan.connect(f.getLastKnownHost(), f.getLastKnownPort(),new FriendConnection(netMan, Connection.Direction.OUT, f.getGuid()));
    }

    public void runFriendConnectorIn(int ms) {
        friendConnector.wakeupIn(ms);
    }

    public UntrustedNode getUntrustedNode(int guid) {
        return untrustedNodes.get(guid);
    }

    public void addUntrustedNode(UntrustedNode n) {
        untrustedNodes.put(n.getGuid(), n);
    }

    public Node getNode(int guid) {
        Node n = friends.get(guid);
        if (n == null) n = untrustedNodes.get(guid);
        if (n == null && guid == me.getGuid()) n = me;
        return n;
    }

    public void loadSubnodesFor(Node node) throws IOException {
        netMan.route(node.getGuid(), new GetUserList());
    }

    public MyNode getMe() {
        return me;
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public String nickname(int guid) {
        Node n = getNode(guid);
        if (n == null) return "unknown ("+Integer.toHexString(guid).toUpperCase()+")";
        return n.getNickname();
    }

    public void shutdown() throws IOException {
        netMan.sendToAllFriends(new GracefulClose(GracefulClose.SHUTDOWN));
    }

    public long getTotalBytesShared() {
        long n=0;
        for(Friend f: friends.values()) {
            if (f.isConnected()) n+=f.getShareSize();
        }
        n+=me.getShareSize();
        return n;
    }

    public void forwardInvitation(PleaseForwardInvitationInteraction fi) throws IOException {
        forwardInvitation(fi.getFromGuid(), fi.getToGuid(), fi.getInvitationCode());
    }

    public void forwardInvitation(int fromGuid, int toGuid, String invitationCode) throws IOException {
        Node from = getNode(fromGuid);
        Friend to = getFriend(toGuid);
        if(org.alliance.core.T.t)org.alliance.core.T.ass(from!=null,"From is null");
        if(org.alliance.core.T.t)org.alliance.core.T.ass(to!=null,"To is null");
        netMan.sendPersistantly(new ForwardedInvitation(from, invitationCode), to);
    }

    public int getNUsersConnected() {
        int n=0;
        for(Friend f : friends.values()) if (f.isConnected()) n++;
        return n;
    }

    public FriendConnector getFriendConnector() {
        return friendConnector;
    }

    public void forwardInvitationTo(final int guid) throws Exception {
        if(T.t)T.trace("Forwarding invitaiton to "+nickname(guid));
        Friend route = null;
        for(Friend f : friends.values()) {
            if (f.getFriendsFriend(guid) != null && f.isConnected()) {
                route = f;
                break;
            }
        }

        if (route == null) {
            for(Friend f : friends.values()) {
                if (f.getFriendsFriend(guid) != null) {
                    route = f;
                    break;
                }
            }
        }

        if (route == null) throw new Exception("Could not find friend that is connected to guid "+guid+"!");
        core.getNetworkManager().sendPersistantly(new PleaseForwardInvitation(getNode(guid)), route);
    }

    public void permanentlyRemove(Friend f) {
        try {
            if (f.isConnected()) f.disconnect();
        } catch(IOException e) {
            if(T.t)T.warn("Nonfatal: "+e);
        }
        friends.remove(f.getGuid());
        for(Iterator i = settings.getFriendlist().iterator();i.hasNext();) if (((org.alliance.core.settings.Friend)i.next()).getGuid() == f.getGuid()) i.remove();
    }

    public int getNUsers() {
        return friends.size();
    }
}
