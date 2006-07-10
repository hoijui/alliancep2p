
 package org.alliance.ui.windows;

 import com.stendahls.XUI.XUIDialog;
 import com.stendahls.nif.ui.OptionDialog;
 import com.stendahls.nif.util.EnumerationIteratorConverter;
 import com.stendahls.ui.JHtmlLabel;
 import static org.alliance.core.CoreSubsystem.KB;
 import org.alliance.core.settings.My;
 import org.alliance.core.settings.SettingClass;
 import org.alliance.core.settings.Settings;
 import org.alliance.core.settings.Share;
 import org.alliance.ui.T;
 import org.alliance.ui.UISubsystem;

 import javax.swing.*;
 import java.awt.event.ActionEvent;
 import java.io.File;
 import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-20
 * Time: 22:33:46
 * To change this template use File | Settings | File Templates.
 */
public class OptionsWindow extends XUIDialog {
    private final static String[] OPTIONS = new String[] {
            "internal.uploadthrottle",
            "internal.hashspeedinmbpersecond",
            "internal.politehashingwaittimeinminutes",
            "internal.politehashingintervalingigabytes",
            "my.nickname",
            "server.port",
            "internal.downloadfolder",
            "internal.alwaysallowfriendstoconnect"
    };

    private UISubsystem ui;
    private HashMap<String, JComponent> components = new HashMap<String, JComponent>();

    private JList shareList;
    private DefaultListModel shareListModel;

    private JTextField nickname;

    private boolean openedWithUndefiniedNickname;

    public OptionsWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionswindow.xui.xml"));

        for(String k : OPTIONS) {
            JComponent c = (JComponent)xui.getComponent(k);
            if (c != null) {
                components.put(k, c);
                setComponentValue(c, getSettingValue(k));
            }
        }

        xui.getComponent("server.port").setEnabled(false);

        nickname = (JTextField)xui.getComponent("my.nickname");

        shareList = (JList)xui.getComponent("shareList");
        shareListModel = new DefaultListModel();
        for(Share share : ui.getCore().getSettings().getSharelist()) shareListModel.addElement(share.getPath());
        shareList.setModel(shareListModel);

        openedWithUndefiniedNickname = nickname.getText().equals(My.UNDEFINED_NICKNAME);

        if (ui.getCore().getUpnpManager().isPortForwardSuccedeed()) {
            ((JHtmlLabel)xui.getComponent("portforward")).setText("Port successfully forwarded in your router using UPnP.");
        }

        display();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private String getSettingValue(String k) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.')+1);
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
        else throw new Exception("Could not find class type: "+clazz);
    }

    private void setComponentValue(JComponent c, String settingValue) {
        if (c instanceof JTextField) {
            JTextField tf = (JTextField)c;
            tf.setText(settingValue);
        } else if (c instanceof JCheckBox) {
            JCheckBox b = (JCheckBox)c;
            if ("0".equals(settingValue) || "no".equalsIgnoreCase(settingValue) || "false".equalsIgnoreCase(settingValue)) {
                b.setSelected(false);
            } else {
                b.setSelected(true);
            }
        }
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        apply();
    }

    private boolean apply() throws Exception {
        if (!nicknameIsOk()) return false;

        //update primitive values
        for(String k : OPTIONS) {
            JComponent c = (JComponent)xui.getComponent(k);
            setSettingValue(k, getComponentValue(c));
        }

        //update shares
        Settings settings = ui.getCore().getSettings();
        settings.getSharelist().clear();
        for(String path : EnumerationIteratorConverter.iterable(shareListModel.elements(), String.class)) {
            settings.getSharelist().add(new Share(path));
        }
        ui.getCore().getShareManager().updateShareBases();
        ui.getCore().getShareManager().getShareMonitor().startScan();

        ui.getCore().getFriendManager().getMe().setNickname(nickname.getText());
        if (ui.getNodeTreeModel(false) != null) ui.getNodeTreeModel(false).signalNodeChanged(ui.getCore().getFriendManager().getMe());

        ui.getCore().saveSettings();

        ui.getCore().getNetworkManager().getUploadThrottle().setRate(settings.getInternal().getUploadthrottle()*KB);

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
        return null;
    }

    private void setSettingValue(String k, Object val) throws Exception {
        String clazz = k.substring(0, k.indexOf('.'));
        k = k.substring(k.indexOf('.')+1);
        SettingClass setting = getSettingClass(clazz);
        setting.setValue(k, val);
    }

    public void EVENT_addfolder(ActionEvent e) {
        JFileChooser fc = new JFileChooser(shareListModel.getSize() > 0 ? shareListModel.getElementAt(shareListModel.getSize()-1).toString() : ".");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            if(T.t)T.trace("adding: "+path);
            if (!new File(path).exists()) path = new File(path).getParent();
            shareListModel.addElement(path);
            shareList.revalidate();
        }
    }

    public void EVENT_removefolder(ActionEvent e) {
        if (shareList.getSelectedIndex() != -1) {
            shareListModel.remove(shareList.getSelectedIndex());
            shareList.revalidate();
        }
    }
}
