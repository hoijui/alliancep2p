package org.alliance.ui.windows;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.file.hash.Hash;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.util.CutCopyPastePopup;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        new CutCopyPastePopup(textarea);

        JScrollPane sp = (JScrollPane)xui.getComponent("scrollpanel");
        sp.setViewportView(textarea);

        chat = (JTextField)xui.getComponent("chat1");
        new CutCopyPastePopup(chat);

        textarea.setEditable(false);
        textarea.setBackground(Color.white);
        textarea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        String link = e.getDescription();
                        if (link.startsWith("http://")) {
                            String allowedChars ="abcdefghijklmnopqrstuvwxyzåäö0123456789-.;/?:@&=+$_.!~*'()#";
                            for(int i=0;i<link.length();i++) {
                                if (allowedChars.indexOf(link.toLowerCase().charAt(i)) == -1) {
                                    OptionDialog.showInformationDialog(ui.getMainWindow(), "Character "+link.charAt(i)+" is not allowed in link.");
                                    return;
                                }
                            }
                            ui.openURL(link);
                        } else {
                            String[] hashes = link.split("\\|");
                            for(String s : hashes) if(T.t) T.trace("Part: "+s);
                            int guid = Integer.parseInt(hashes[0]);
                            if (OptionDialog.showQuestionDialog(ui.getMainWindow(), "Add "+(hashes.length-1)+" files to downloads?")) {
                                ArrayList<Integer> al = new ArrayList<Integer>();
                                al.add(guid);
                                for(int i=1;i<hashes.length;i++) {
                                    ui.getCore().getNetworkManager().getDownloadManager().queDownload(new Hash(hashes[i]), "Link from chat", al);
                                }
                                ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
                            }
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
        send(escapeHTML(chat.getText()));
    }

    public void EVENT_chat2(ActionEvent e) throws Exception {
        send(escapeHTML(chat.getText()));
    }

    private String escapeHTML(String text) {
        text = text.replace("<", "&lt;");
        String pattern = "http://";
        int i=0;
        while((i = text.indexOf(pattern, i)) != -1) {
            int end = text.indexOf(' ', i);
            if (end == -1) end = text.length();
            String s = text.substring(0, i);
            s += "<a href=\""+text.substring(i, end)+"\">"+text.substring(i, end)+"</a>";
            i = s.length();
            if (end < text.length()-1) s += text.substring(end);
            text = s;
        }
        return text;
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
