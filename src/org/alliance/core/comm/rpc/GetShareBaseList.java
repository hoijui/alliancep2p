package org.alliance.core.comm.rpc;

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
public class GetShareBaseList extends RPC {
    public GetShareBaseList() {
    }

    public void execute(Packet data) throws IOException {
        send(new ShareBaseList());
    }

    public Packet serializeTo(Packet p) {
        return p;
    }
}
