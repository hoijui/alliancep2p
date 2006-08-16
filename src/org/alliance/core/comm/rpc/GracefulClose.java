package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GracefulClose extends RPC {
    public static final byte DUPLICATE_CONNECTION = 10;
    public static final byte SHUTDOWN = 20;
    public static final byte DELETED = 30;

    private byte reason;

    public GracefulClose() {
    }

    public GracefulClose(byte reason) {
        this.reason = reason;
    }

    public void execute(Packet data) throws IOException {
        reason = data.readByte();
        if (reason == DUPLICATE_CONNECTION) {
            if (con.getRemoteFriend().hasMultipleFriendConnections()) {
                if(T.t)T.info("Ha! We have detected a double connection to one friend. Closing one down.");
            } else {
                if(T.t)T.info(con.getRemoteFriend()+" is closing us down even though we only have one connection to him! This is a little bit scketchy but probably ok. We just don't know that we'll get a new connection to him in an instant.");
            }
        } else if (reason == SHUTDOWN) {
            if(T.t)T.info("Remote computer shutting donw. Closing this connection.");
        } else if (reason == DELETED) {
            if(T.t)T.info("Buhuu! Remote removed me from it's userlist.");
        } else {
            if(T.t)T.warn("Unknown connection close reason: "+reason);
        }

        core.updateLastSeenOnlineForFriends();

        con.close();
    }

    public Packet serializeTo(Packet p) {
        p.writeByte(reason);
        return p;
    }
}
