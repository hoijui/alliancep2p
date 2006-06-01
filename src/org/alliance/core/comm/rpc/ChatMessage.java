package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.PostMessageInteraction;

import java.io.IOException;

/**
 *
 * Recieved when we need info about a friend that we haven't got the correct ip/port to.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class ChatMessage extends PersistantRPC {
    private String message;

    public ChatMessage() {
        routable = true;
    }

    public ChatMessage(String message) {
        this.message = message;
    }

    public void execute(Packet in) throws IOException {
        message = in.readUTF();
        manager.getCore().queNeedsUserInteraction(new PostMessageInteraction(message, fromGuid));
    }

    public Packet serializeTo(Packet p) {
        p.writeUTF(message);
        return p;
    }
}
