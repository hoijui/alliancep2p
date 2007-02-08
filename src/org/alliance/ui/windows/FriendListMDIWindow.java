package org.alliance.ui.windows;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.node.Friend;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
public class FriendListMDIWindow extends AllianceMDIWindow {
    private UISubsystem ui;
    private JList list;

    private ImageIcon iconFriend, iconFriendCool, iconFriendLame, iconFriendDimmed, iconFriendOld;

    private JLabel statusleft, statusright;

    private String[] LEVEL_NAMES = {"Rookie", "True Member", "Experienced"};
    private String[] LEVEL_ICONS = {"friend_lame", "friend", "friend_cool"};

    public FriendListMDIWindow() {
    }

    public FriendListMDIWindow(MDIManager manager, UISubsystem ui) throws Exception {
        super(manager, "friendlist", ui);
        this.ui = ui;

        iconFriend = new ImageIcon(ui.getRl().getResource("gfx/icons/friend.png"));
        iconFriendCool = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_cool.png"));
        iconFriendLame = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_lame.png"));
        iconFriendDimmed = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_dimmed.png"));
        iconFriendOld = new ImageIcon(ui.getRl().getResource("gfx/icons/friend_old.png"));

        setWindowType(WINDOWTYPE_NAVIGATION);

        statusleft = (JLabel) xui.getComponent("statusleft");
        statusright = (JLabel) xui.getComponent("statusright");

        createUI();
        setTitle("My  Network");
    }

    public void update() {
        statusright.setText("Online: " + ui.getCore().getFriendManager().getNUsersConnected() + "/" + ui.getCore().getFriendManager().getNUsers() + " (" + TextUtils.formatByteSize(ui.getCore().getFriendManager().getTotalBytesShared()) + ")");
    }

    private void createUI() throws Exception {
        list = new JList(ui.getFriendListModel());
        list.setCellRenderer(new FriendListRenderer());
        ((JScrollPane) xui.getComponent("scrollpanel")).setViewportView(list);

        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            private int selectedIndex = -1;
            public void valueChanged(ListSelectionEvent e) {
                try {
                    if (!e.getValueIsAdjusting() && selectedIndex != list.getSelectedIndex()) {
                        selectedIndex = list.getSelectedIndex();
                        EVENT_viewshare(null);
                    }
                } catch (Exception e1) {
                    ui.handleErrorInEventLoop(e1);
                }
            }
        });

        updateMyLevelInformation();

        postInit();
    }

    public void updateMyLevelInformation() throws IOException {
        ((JLabel)xui.getComponent("myname")).setText(ui.getCore().getFriendManager().getMe().getNickname());
        ((JLabel)xui.getComponent("mylevel")).setText(getLevelName(getMyLevel()));
        ((JLabel)xui.getComponent("myicon")).setIcon(new ImageIcon(ui.getRl().getResource(getLevelIcon(getMyLevel(), true))));
        String s = "";
        switch(getMyNumberOfInvites()) {
            case 0: s = "Invite 2 friends to become "; break;
            case 1: s = "Invite 1 friend to become "; break;
            case 2: s = "Invite 2 friends to become "; break;
            case 3: s = "Invite 1 friend to become "; break;
        }
        if (getMyLevel() < LEVEL_NAMES.length-1) {
            s += "'"+getLevelName(getMyLevel()+1)+"' (";
            ((JLabel)xui.getComponent("nextLevelText")).setText(s);
            ((JLabel)xui.getComponent("nextLevelIcon")).setIcon(new ImageIcon(ui.getRl().getResource(getLevelIcon(getMyLevel()+1, false))));
            ((JLabel)xui.getComponent("levelEnding")).setText(")");
        } else {
            ((JLabel)xui.getComponent("nextLevelText")).setText("");
            ((JLabel)xui.getComponent("nextLevelIcon")).setText("");
            ((JLabel)xui.getComponent("nextLevelIcon")).setIcon(null);
            ((JLabel)xui.getComponent("levelEnding")).setText("");
        }
    }

    private String getLevelIcon(int myLevel, boolean big) {
        if (myLevel < 0) myLevel = 0;
        if (myLevel >= LEVEL_ICONS.length) myLevel = LEVEL_ICONS.length-1;
        return "gfx/icons/"+LEVEL_ICONS[myLevel]+(big ? "_big" : "")+".png";
    }

    private String getLevelName(int myLevel) {
        if (myLevel < 0) myLevel = 0;
        if (myLevel >= LEVEL_NAMES.length) myLevel = LEVEL_NAMES.length-1;
        return LEVEL_NAMES[myLevel];
    }

    private int getMyLevel() {
        return getMyNumberOfInvites()/2;
    }

    private int getMyNumberOfInvites() {
        return ui.getCore().getSettings().getMy().getInvitations();
    }

    public void save() throws Exception {
    }

    public String getIdentifier() {
        return "friendlist";
    }

    public void revert() throws Exception {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().refreshFriendInfo();
                } catch (IOException e) {
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

            Friend f = (Friend) value;
            if (f.isConnected()) {

                if (f.getNumberOfInvitedFriends() <= 1) {
                    setIcon(iconFriendLame);
                } else if (f.getNumberOfInvitedFriends() >= 4) {
                    setIcon(iconFriendCool);
                } else {
                    setIcon(iconFriend);
                }
                if (isSelected)
                    setForeground(Color.white);
                else
                    setForeground(Color.black);
//                setText(f.getNickname()+" ("+ TextUtils.formatByteSize(f.getShareSize())+")");
                setText("<html>" + nickname(f.getGuid()) +
                        "<font color=aaaaaa> " +
                        FriendListMDIWindow.this.ui.getCore().getFriendManager().contactPath(f.getGuid()) +
                        "</font> (" +
                        TextUtils.formatByteSize(f.getShareSize())
                        + ")</html>");
            } else if (f.hasNotBeenOnlineForLongTime()) {
                setIcon(iconFriendOld);
                setForeground(Color.lightGray);
                if (f.getLastSeenOnlineAt() != 0) {
                    setText(nickname(f.getGuid()) + " (offline for " +
                            ((System.currentTimeMillis() - f.getLastSeenOnlineAt()) / 1000 / 60 / 60 / 24)
                            + " days)");
                } else {
                    setText(nickname(f.getGuid()));
                }
            } else {
                setIcon(iconFriendDimmed);
                setForeground(Color.lightGray);
                setText(FriendListMDIWindow.this.ui.getCore().getFriendManager().nicknameWithContactPath(f.getGuid()));
            }


//            setToolTipText("Remote build number: "+f.getAllianceBuildNumber());

            setToolTipText("<html>Build number: "+f.getAllianceBuildNumber()+"<br>" +
                    "Share: "+TextUtils.formatByteSize(f.getShareSize())+" in "+f.getNumberOfFilesShared()+" files<br>" +
                    "Invited friends: "+f.getNumberOfInvitedFriends()+"<br>" +
                    "Upload speed record: "+TextUtils.formatByteSize((long)f.getHighestOutgoingCPS())+"/s<br>" +
                    "Download speed record: "+TextUtils.formatByteSize((long)f.getHighestIncomingCPS())+"/s<br>" +
                    "Bytes uploaded: "+TextUtils.formatByteSize(f.getTotalBytesSent())+"<br>" +
                    "Bytes downloaded: "+TextUtils.formatByteSize(f.getTotalBytesReceived())+"</html>");

            return this;
        }
    }

    private String nickname(int guid) {
        return ui.getCore().getFriendManager().nickname(guid);
    }

    public void EVENT_editname(ActionEvent e) {
        if (list.getSelectedValue() == null) return;
        Friend f = (Friend) list.getSelectedValue();
        if (f != null) {
            String pi = JOptionPane.showInputDialog("Enter nickname for friend: "+nickname(f.getGuid()), nickname(f.getGuid()));
            if (pi != null) ui.getCore().getFriendManager().setNicknameToShowInUI(f, pi);
            ui.getFriendListModel().signalFriendChanged(f);
        }
    }

    public void EVENT_chat(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        Friend f = (Friend) list.getSelectedValue();
        if (f != null) ui.getMainWindow().chatMessage(f.getGuid(), null, 0);
    }

    public void EVENT_reconnect(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        final Friend f = (Friend)list.getSelectedValue();
        if (f.isConnected()) f.reconnect();
    }

    public void EVENT_viewshare(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        Friend f = (Friend) list.getSelectedValue();
        if (f != null) {
            if (!f.isConnected()) {
//                  just ignore the request
//                OptionDialog.showErrorDialog(ui.getMainWindow(), "User must be online in order to view his share.");
            } else {
                ui.getMainWindow().viewShare(f);
            }
        }
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        ui.getMainWindow().EVENT_addfriendwizard(e);
    }

    public void EVENT_removefriend(ActionEvent e) throws Exception {
        if (list.getSelectedValue() == null) return;
        Object[] friends = list.getSelectedValues();
        if (friends != null && friends.length > 0) {
            Boolean delete = OptionDialog.showQuestionDialog(ui.getMainWindow(), "Are you sure you want to permanently delete these (" + friends.length + ") connections?");
            if (delete == null) return;
            if (delete) {
                for (Object friend : friends) {
                    Friend f = (Friend) friend;
                    if (f != null) {
                        ui.getCore().getFriendManager().permanentlyRemove(f);
                    }
                }
                revert();
            }
        }
    }
}
