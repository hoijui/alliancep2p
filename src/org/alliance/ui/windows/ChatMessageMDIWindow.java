package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class ChatMessageMDIWindow extends AllianceMDIWindow {
    private int guid;
    private JTextArea textarea;
    private JTextField chat;

    public ChatMessageMDIWindow(UISubsystem ui, int guid) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "chatmessage", ui);
        this.guid = guid;

        textarea = (JTextArea)xui.getComponent("textarea");
        chat = (JTextField)xui.getComponent("chat1");

        setTitle("Chat with "+ui.getCore().getFriendManager().nickname(guid));

        postInit();
    }

    public void EVENT_chat1(ActionEvent e) throws IOException {
        send(chat.getText());
    }

    public void EVENT_chat2(ActionEvent e) throws IOException {
        send(chat.getText());
    }

    private void send(final String text) throws IOException {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendPersistantly(new ChatMessage(text), ui.getCore().getFriendManager().getFriend(guid));
                } catch(IOException e) {
                    ui.getCore().reportError(e, this);
                }
            }
        });
        chat.setText("");
        addMessage(ui.getCore().getFriendManager().getMe().getNickname(), text, System.currentTimeMillis());
    }

    public void addMessage(String message, long tick) {
        addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick);
    }

    public void addMessage(String from, String message, long tick) {
        textarea.append("("+from+") "+message+" (at "+new Date(tick)+")\n");
    }

    public String getIdentifier() {
        return "msg"+guid;
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }
}
