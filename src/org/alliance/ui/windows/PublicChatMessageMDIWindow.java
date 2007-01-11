package org.alliance.ui.windows;

import org.alliance.ui.UISubsystem;
import org.alliance.core.comm.rpc.ChatMessageV2;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.T;
import org.alliance.core.node.Friend;

import java.io.IOException;
import java.awt.*;
import java.util.Date;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class PublicChatMessageMDIWindow extends AbstractChatMessageMDIWindow {
    public PublicChatMessageMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "chatmessage", ui);

        setTitle("Chat");

        postInit();
    }

    protected void send(final String text) throws IOException {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    for(Friend f : ui.getCore().getFriendManager().friends()) {
                        ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessageV2(text, true), f);
                    }
                } catch(IOException e) {
                    ui.getCore().reportError(e, this);
                }
            }
        });
        chat.setText("");
        addMessage(ui.getCore().getFriendManager().getMe().getNickname(), text, System.currentTimeMillis());
    }

    public String getIdentifier() {
        return "publicchat";
    }
}
