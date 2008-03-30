package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.settings.My;
import org.alliance.core.settings.SettingClass;
import org.alliance.core.settings.Settings;
import org.alliance.core.settings.Share;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.javasoft.plaf.synthetica.filechooser.SyntheticaFileChooser;
import de.javasoft.synthetica.addons.ExtendedFileChooser;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-20
 * Time: 22:33:46
 * To change this template use File | Settings | File Templates.
 */
public class OptionsWindow extends XUIDialog {
    private final static String[] OPTIONS = new String[]{
            "internal.uploadthrottle",
            "internal.hashspeedinmbpersecond",
            "internal.politehashingwaittimeinminutes",
            "internal.politehashingintervalingigabytes",
            "my.nickname",
            "server.port",
            "internal.downloadfolder",
            "internal.alwaysallowfriendstoconnect",
            "internal.alwaysallowfriendsoffriendstoconnecttome",
            "internal.invitationmayonlybeusedonce",
            "internal.encryption",
            "internal.showpublicchatmessagesintray",
            "internal.showprivatechatmessagesintray",
            "internal.showsystemmessagesintray"
    };

    private UISubsystem ui;
    private HashMap<String, JComponent> components = new HashMap<String, JComponent>();

    private JList shareList;
    private DefaultListModel shareListModel;
    private boolean shareListHasBeenModified = false;

    private JTextField nickname, downloadFolder;

    private boolean openedWithUndefiniedNickname;
    private int uploadThrottleBefore;


    public OptionsWindow(UISubsystem ui) throws Exception {
        this(ui, false);
    }

    public OptionsWindow(UISubsystem ui, boolean startInShareTab) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionswindow.xui.xml"));

        xui.getComponent("server.port").setEnabled(false);

        nickname = (JTextField) xui.getComponent("my.nickname");
        downloadFolder = (JTextField)xui.getComponent("internal.downloadfolder");
        shareList = (JList) xui.getComponent("shareList");
        shareListModel = new DefaultListModel();
        for (Share share : ui.getCore().getSettings().getSharelist()) shareListModel.addElement(share.getPath());
        shareList.setModel(shareListModel);

        openedWithUndefiniedNickname = nickname.getText().equals(My.UNDEFINED_NICKNAME);

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel) xui.getComponent("portforward")).setText("Port successfully forwarded in your router using UPnP.");
        }

        if (startInShareTab) ((JTabbedPane)xui.getComponent("tab")).setSelectedIndex(1);

        uploadThrottleBefore = ui.getCore().getSettings().getInternal().getUploadthrottle();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();

        //we set this AFTER we pack the frame - so that the frame isnt made wider because of a long download folder path
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            if (c != null) {
                components.put(k, c);
                setComponentValue(c, getSettingValue(k));
            }
        }
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(ss.width/2-getWidth()/2,
                    ss.height/2-getHeight()/2);
        setVisible(true);
    }

    private String getSettingValue(String k) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        return String.valueOf(setting.getValue(k));
    }

    private SettingClass getSettingClass(String clazz) throws Exception {
        if (clazz.equals("internal"))
            return ui.getCore().getSettings().getInternal();
        else if (clazz.equals("my"))
            return ui.getCore().getSettings().getMy();
        else if (clazz.equals("server"))
            return ui.getCore().getSettings().getServer();
        else throw new Exception("Could not find class type: " + clazz);
    }

    private void setComponentValue(JComponent c, String settingValue) {
        if (c instanceof JTextField) {
            JTextField tf = (JTextField) c;
            tf.setText(settingValue);
        } else if (c instanceof JCheckBox) {
            JCheckBox b = (JCheckBox) c;
            if ("0".equals(settingValue) || "no".equalsIgnoreCase(settingValue) || "false".equalsIgnoreCase(settingValue))
            {
                b.setSelected(false);
            } else {
                b.setSelected(true);
            }
        } else if (c instanceof JComboBox) {
            JComboBox b = (JComboBox)c;
            b.setSelectedIndex(Integer.parseInt(settingValue));
        }
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        apply();
    }

    private boolean apply() throws Exception {
        if (!nicknameIsOk()) return false;

        //update primitive values
        for (String k : OPTIONS) {
            JComponent c = (JComponent) xui.getComponent(k);
            setSettingValue(k, getComponentValue(c));
        }

        //update shares
        Settings settings = ui.getCore().getSettings();
        settings.getSharelist().clear();

        //remove paths that are subdirectories of other shares
        while(removeDuplicateShare());

        for (String path : EnumerationIteratorConverter.iterable(shareListModel.elements(), String.class)) {
            settings.getSharelist().add(new Share(path));
        }
        ui.getCore().getShareManager().updateShareBases();
        if (shareListHasBeenModified) {
            ui.getCore().getShareManager().getShareMonitor().startScan(false);
        }

        ui.getCore().getFriendManager().getMe().setNickname(nickname.getText());
        if (ui.getNodeTreeModel(false) != null)
            ui.getNodeTreeModel(false).signalNodeChanged(ui.getCore().getFriendManager().getMe());

        ui.getCore().saveSettings();

        ui.getCore().getNetworkManager().getUploadThrottle().setRate(settings.getInternal().getUploadthrottle() * KB);
        if (uploadThrottleBefore != settings.getInternal().getUploadthrottle()) ui.getCore().getNetworkManager().getBandwidthOut().resetHighestCPS();
        return true;
    }

    /**
     * @return True if a duplicate share was removed - in this case this method needs to be called again in order to
     * check for more duplicated to remove. This is becase only one duplicate is removed at a time.
     */
    private boolean removeDuplicateShare() {
        for(Iterator<String> i=EnumerationIteratorConverter.iterable(shareListModel.elements(), String.class).iterator();i.hasNext();) {
            String path = i.next();
            ArrayList<String> al = new ArrayList<String>();
            for (String s : EnumerationIteratorConverter.iterable(shareListModel.elements(), String.class)) {
                al.add(s);
            }
            al.add(ui.getCore().getSettings().getInternal().getDownloadfolder());

            for (String s : al) {
                String pathA = TextUtils.makeSurePathIsMultiplatform(new File(path).getAbsolutePath());
                String sA = TextUtils.makeSurePathIsMultiplatform(new File(s).getAbsolutePath());
                if (!sA.equals(pathA) &&
                        pathContains(pathA,sA)) {
                    OptionDialog.showInformationDialog(ui.getMainWindow(), "The folder "+pathA+" is already shared as "+sA+". There is no need to add it in your shares.");
                    shareListModel.removeElement(path);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pathContains(String path, String file) {
        String s1[] = TextUtils.makeSurePathIsMultiplatform(path).split("/");
        String s2[] = TextUtils.makeSurePathIsMultiplatform(file).split("/");
        if (s1.length < s2.length) return false;

        for(int i=0;i<s2.length;i++) {
            if (!s1[i].equals(s2[i])) return false;
        }
        return true;
    }

    private boolean nicknameIsOk() {
        if (nickname.getText().equals(My.UNDEFINED_NICKNAME)) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), "You must enter a nickname before continuing.");
            return false;
        }
        return true;
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        if (!nicknameIsOk()) return;
        dispose();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        if (apply()) {
            dispose();
            if (openedWithUndefiniedNickname) {
                OptionDialog.showInformationDialog(ui.getMainWindow(), "Now that you have set up Alliance it is time to connect to other users.[p]The add connection wizard will now open.[p]");
                ui.getMainWindow().openWizard();
            }
        }
    }

    private Object getComponentValue(JComponent c) {
        if (c instanceof JTextField) return ((JTextField)c).getText();
        if (c instanceof JCheckBox) return ((JCheckBox)c).isSelected() ? 1 : 0;
        if (c instanceof JComboBox) return ""+((JComboBox)c).getSelectedIndex();
        return null;
    }

    private void setSettingValue(String k, Object val) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.') + 1);
        SettingClass setting = getSettingClass(clazz);
        setting.setValue(k, val);
    }

    public void EVENT_addfolder(ActionEvent e) {
        JFileChooser fc = new JFileChooser(shareListModel.getSize() > 0 ? shareListModel.getElementAt(shareListModel.getSize() - 1).toString() : ".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            if (T.t) T.trace("adding: " + path);
            if (!new File(path).exists()) path = new File(path).getParent();
            shareListModel.addElement(path);
            shareListHasBeenModified = true;
            shareList.revalidate();
        }
    }

    public void EVENT_browse(ActionEvent e) {
        JFileChooser fc = new JFileChooser(downloadFolder.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            downloadFolder.setText(path);
        }
    }

    /**
     * Triggered when "remove folder" button is pressed in the option->share menu
     * @param e
     */
    public void EVENT_removefolder(ActionEvent e) {
        if (shareList.getSelectedIndex() != -1) {
            shareListModel.remove(shareList.getSelectedIndex());
            shareListHasBeenModified = true;
            shareList.revalidate();
        }
    }
}
