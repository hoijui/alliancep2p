package org.alliance.ui.windows;

import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.core.comm.rpc.ChatMessageV2;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class PrivateChatMessageMDIWindow extends AbstractChatMessageMDIWindow {
    private int guid;

    public PrivateChatMessageMDIWindow(UISubsystem ui, int guid) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "chatmessage", ui);
        this.guid = guid;

        setTitle("Private chat with "+ui.getCore().getFriendManager().nickname(guid));

        postInit();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chat.setText("Type here and then press 'send' to start chatting.");
                chat.requestFocus();
                chat.selectAll();
            }
        });
    }

    protected void send(final String text) throws IOException {
        if (text == null || text.trim().length() == 0) return;
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessageV2(text, false), ui.getCore().getFriendManager().getFriend(guid));
                } catch(IOException e) {
                    ui.getCore().reportError(e, this);
                }
            }
        });
        chat.setText("");
        addMessage(ui.getCore().getFriendManager().getMe().getNickname(), text, System.currentTimeMillis());
    }

    public String getIdentifier() {
        return "msg"+guid;
    }
}
