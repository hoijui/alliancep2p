package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.OptionDialog;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.T;
import org.alliance.launchers.OSInfo;
import org.alliance.core.file.hash.Hash;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

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

        textarea.setEditable(false);
        textarea.setBackground(Color.white);
        textarea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        String link = e.getDescription();
                        String[] hashes = link.split("\\|");
                        for(String s : hashes) if(T.t) T.trace("File: "+s);
                        if (OptionDialog.showQuestionDialog(ui.getMainWindow(), "Add "+hashes.length+" files to downloads?")) {
                            for(String hash : hashes) {
                                ui.getCore().getNetworkManager().getDownloadManager().queDownload(new Hash(hash), "Link from chat", new ArrayList<Integer>());
                            }
                            ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
                        }
                    } catch (IOException e1) {
                        ui.getCore().reportError(e1, this);
                    }
                }
            }
        });


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
