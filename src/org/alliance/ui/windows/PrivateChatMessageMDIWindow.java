package org.alliance.ui.windows;

import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.ui.UISubsystem;

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
    }

    protected void send(final String text) throws IOException {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessage(text, false), ui.getCore().getFriendManager().getFriend(guid));
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

    public void addMessage(String from, String message, long tick) {
        int n = from.hashCode();
        if (n<0)n=-n;
        n%=COLORS.length;
        Color c = COLORS[n];

        html += "<font color=\"#9f9f9f\">["+FORMAT.format(new Date(tick))+"] <font color=\""+toHexColor(c)+"\">"+from+":</font> <font color=\""+toHexColor(c.darker())+"\">"+message+"</font><br>";
        textarea.setText(html);
    }
}
