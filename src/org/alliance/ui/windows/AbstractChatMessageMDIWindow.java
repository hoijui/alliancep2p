package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
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
public abstract class AbstractChatMessageMDIWindow extends AllianceMDIWindow {
    protected final static DateFormat FORMAT = new SimpleDateFormat("HH:mm");
    protected final static Color COLORS[] = {
            new Color(0x0068a7),
            new Color(0x009606),
            new Color(0xa13eaa),
            new Color(0x008b76),
            new Color(0xb77e24),
            new Color(0xef0000),
            new Color(0xb224b7),
            new Color(0xb77e24)
    };

    protected JEditorPane textarea;
    protected JTextField chat;
    protected String html = "";


    protected AbstractChatMessageMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui) throws Exception {
        super(manager, mdiWindowIdentifier, ui);
    }

    protected abstract void send(final String text) throws IOException, Exception;
    public abstract String getIdentifier();

    protected void postInit() {
        textarea = new JEditorPane("text/html", "");

        JScrollPane sp = (JScrollPane)xui.getComponent("scrollpanel");
        sp.setViewportView(textarea);

        chat = (JTextField)xui.getComponent("chat1");

        super.postInit();
    }

    public void EVENT_chat1(ActionEvent e) throws Exception {
        send(chat.getText());
    }

    public void EVENT_chat2(ActionEvent e) throws Exception {
        send(chat.getText());
    }

    public void addMessage(String from, String message, long tick) {
        int n = from.hashCode();
        if (n<0)n=-n;
        n%= COLORS.length;
        Color c = COLORS[n];

        html += "<font color=\"#9f9f9f\">["+ FORMAT.format(new Date(tick))+"] <font color=\""+toHexColor(c)+"\">"+from+":</font> <font color=\""+toHexColor(c.darker())+"\">"+message+"</font><br>";
        textarea.setText(html);
    }

    protected String toHexColor(Color color) {
        return "#"+Integer.toHexString(color.getRGB()&0xffffff);
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }
}
