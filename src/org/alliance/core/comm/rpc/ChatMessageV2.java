package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;

import java.io.IOException;

/**
 *
 * version 2 of chat message - the old class (ChatMessage) will only be used in the transition to the next version (0.9.6)
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class ChatMessageV2 extends PersistantRPC {
    private String message;
    private boolean messageToAll;

    public ChatMessageV2() {
        routable = true;
    }

    public ChatMessageV2(String message, boolean messageToAll) {
        this.message = message;
        this.messageToAll = messageToAll;
    }

    public void execute(Packet in) throws IOException {
        message = in.readUTF();
        messageToAll = in.readBoolean();
        if (messageToAll)
            manager.getCore().queNeedsUserInteraction(new PostMessageToAllInteraction(message, fromGuid));
        else
            manager.getCore().queNeedsUserInteraction(new PostMessageInteraction(message, fromGuid));
    }

    public Packet serializeTo(Packet p) {
        p.writeUTF(message);
        p.writeBoolean(messageToAll);
        return p;
    }
}
