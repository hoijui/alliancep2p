package org.alliance.ui.windows;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.node.Friend;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:22:07
 */
public class FriendListMDIWindow extends AllianceMDIWindow  {
    private UISubsystem ui;
    private JList list;

    private ImageIcon iconFriend, iconFriendDimmed;

    private JLabel statusleft, statusright;

    public FriendListMDIWindow() {
    }

    public FriendListMDIWindow(MDIManager manager, UISubsystem ui) throws Exception {
        super(manager, "friendlist", ui);
        this.ui = ui;

        iconFriend = new ImageIcon(ui.getRl().getResource("gfx/icons/friend.png"));
        iconFriendDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_dimmed.png"));

        setWindowType(WINDOWTYPE_NAVIGATION);

        statusleft = (JLabel)xui.getComponent("statusleft");
        statusright = (JLabel)xui.getComponent("statusright");

        createUI();
        setTitle("My  Network");
    }

    public void update() {
        statusright.setText("Online: "+ui.getCore().getFriendManager().getNUsersConnected()+"/"+ui.getCore().getFriendManager().getNUsers()+" ("+TextUtils.formatByteSize(ui.getCore().getFriendManager().getTotalBytesShared())+")");
    }

    private void createUI() throws Exception {
        list = new JList(ui.getFriendListModel());
        list.setCellRenderer(new FriendListRenderer());
        ((JScrollPane)xui.getComponent("scrollpanel")).setViewportView(list);

        postInit();
    }

    public void save() throws Exception {
    }

    public String  getIdentifier() {
        return "friendlist";
    }

    public void revert() throws Exception {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().refreshFriendInfo();
                } catch(IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public void serialize(ObjectOutputStream out) throws IOException {
    }

    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;
    }

    private class FriendListRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Friend f = (Friend)value;
            if (f.isConnected()) {
                setIcon(iconFriend);
                if (isSelected)
                    setForeground(Color.white);
                else
                    setForeground(Color.black);
                setText(f.getNickname()+" ("+ TextUtils.formatByteSize(f.getShareSize())+")");
            } else {
                setIcon(iconFriendDimmed);
                    setForeground(Color.lightGray);
                setText(f.getNickname());
            }

            return this;
        }
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        Friend f = (Friend)list.getSelectedValue();
        if (f != null) ui.getMainWindow().chatMessage(f.getGuid(), null, 0);
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        ui.getMainWindow().EVENT_addfriendwizard(e);
    }

    public void EVENT_removefriend(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        Friend f = (Friend)list.getSelectedValue();
        if (f != null) {
            if (OptionDialog.showConfirmDialog(ui.getMainWindow(), "Are you sure you want to permanently delete this connection?")) {
                ui.getCore().getFriendManager().permanentlyRemove(f);
                revert();
            }
        }
    }
}
