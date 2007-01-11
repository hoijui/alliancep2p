package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.comm.rpc.ChatMessage;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class ChatMessageMDIWindow extends AllianceMDIWindow {
    private final static DateFormat FORMAT = new SimpleDateFormat("HH:mm");
    private final static Color COLORS[] = {
            new Color(0x0068a7),
            new Color(0x009606),
            new Color(0xa13eaa),
            new Color(0x008b76),
            new Color(0xb77e24),
            new Color(0xef0000),
            new Color(0xb224b7),
            new Color(0xb77e24)
    };

    private int guid;
    private JEditorPane textarea;
    private JTextField chat;
    private String html = "";

    public ChatMessageMDIWindow(UISubsystem ui, int guid) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "chatmessage", ui);
        this.guid = guid;

        textarea = new JEditorPane("text/html", "");
/*
        textarea.setCaret(new Caret() {
            public void install(JTextComponent c){}
            public void deinstall(JTextComponent c){}
            public void paint(Graphics g){}
            public void addChangeListener(ChangeListener l){}
            public void removeChangeListener(ChangeListener l){}
            public boolean isVisible(){return false;}
            public void setVisible(boolean v){}
            public boolean isSelectionVisible(){return false;}
            public void setSelectionVisible(boolean v){}
            public void setMagicCaretPosition(Point p){}
            public Point getMagicCaretPosition(){return new Point(0,0);}
            public void setBlinkRate(int rate){}
            public int getBlinkRate(){return 10000;}
            public int getDot(){return 0;}
            public int getMark(){return 0;}
            public void setDot(int dot){}
            public void moveDot(int dot){}
        });
*/

        JScrollPane sp = (JScrollPane)xui.getComponent("scrollpanel");
        sp.setViewportView(textarea);

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
        int n = from.hashCode();
        if (n<0)n=-n;
        n%=COLORS.length;
        Color c = COLORS[n];

//        String color = from.equals(ui.getCore().getFriendManager().getMe().getNickname()) ? "7a7a7a" : "000000";
//        html += "<font color=\"#"+color+"\">["+FORMAT.format(new Date(tick))+"] "+from+": "+message+"</font><br>";
        html += "<font color=\"#9f9f9f\">["+FORMAT.format(new Date(tick))+"] <font color=\""+toHexColor(c)+"\">"+from+":</font> <font color=\""+toHexColor(c.darker())+"\">"+message+"</font><br>";
        textarea.setText(html);
    }

    private String toHexColor(Color color) {
        return "#"+Integer.toHexString(color.getRGB()&0xffffff);
    }

    public String getIdentifier() {
        return "msg"+guid;
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }
}
