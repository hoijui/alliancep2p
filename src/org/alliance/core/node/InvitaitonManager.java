package org.alliance.core.node;

import com.stendahls.util.HumanReadableEncoder;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.InvitationConnection;
import org.alliance.core.settings.Settings;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-16
 * Time: 14:20:39
 */
public class InvitaitonManager {
    public static final long INVITATION_TIMEOUT_IN_MINUTES = 60*24*31;
    public static final long INVITATION_TIMEOUT = 1000*60*60*INVITATION_TIMEOUT_IN_MINUTES;

    private CoreSubsystem core;

    private HashMap<Integer, Invitation> invitations = new HashMap<Integer, Invitation>();

    public InvitaitonManager(CoreSubsystem core, Settings settings) {
        this.core = core;
    }

    public Invitation createInvitation() throws Exception {
        return createInvitation(null);
    }

    public Invitation createInvitation(Integer destinationGuid) throws Exception {
        Invitation i = new Invitation(core, destinationGuid);
        invitations.put(i.getInvitationPassKey(), i);
        return i;
    }

    public boolean containsKey(int key) {
        return invitations.containsKey(key);
    }

    public boolean isValid(int key) {
        return System.currentTimeMillis()-invitations.get(key).getCreatedAt() < INVITATION_TIMEOUT;
    }

    public void attemptToBecomeFriendWith(String invitation) throws IOException {
        byte data[] = HumanReadableEncoder.fromBase64String(invitation);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        byte iparray[] = new byte[4];
        for(int i=0;i<iparray.length;i++) iparray[i] = in.readByte();

        InetAddress ip = InetAddress.getByAddress(iparray);
        int port = in.readUnsignedShort();
        int passkey = in.readInt();

        if(T.t)T.info("Deserialized invitation: "+ip+", "+port+", "+passkey);

        core.getNetworkManager().connect(
                ip.getHostAddress(), port, new InvitationConnection(core.getNetworkManager(), Connection.Direction.OUT, passkey));
    }

    public Invitation getInvitation(int key) {
        return invitations.get(key);
    }

    public void consume(int key) {
        invitations.remove(key);
    }

    public void save(ObjectOutputStream out) throws IOException {
        out.writeObject(invitations);
    }

    public void load(ObjectInputStream in) throws IOException {
        try {
            invitations = (HashMap<Integer, Invitation>)in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not instance class "+e);
        }
    }

    public boolean hasBeenRecentlyInvited(int guid) {
        if (getMostRecentByGuid(guid) == null) return false;
        if (System.currentTimeMillis() - getMostRecentByGuid(guid).getCreatedAt() < core.getSettings().getInternal().getMinimumtimebetweeninvitations()*1000*60) return true;
        return false;
    }

    private Invitation getMostRecentByGuid(int guid) {
        Invitation mostRecent = null;
        for(Invitation i : invitations.values()) if (i.getDestinationGuid() != null && i.getDestinationGuid() == guid) {
            if (mostRecent == null)
                mostRecent = i;
            else {
                if (mostRecent.getCreatedAt() < i.getCreatedAt())
                    mostRecent = i;
            }
        }
        return mostRecent;
    }
}
