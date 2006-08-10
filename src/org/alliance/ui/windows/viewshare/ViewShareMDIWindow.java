package org.alliance.ui.windows.viewshare;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.node.Friend;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.windows.AllianceMDIWindow;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class ViewShareMDIWindow extends AllianceMDIWindow {
    private Friend remote;
    private JTree tree;
    private ViewShareTreeModel model;

    public ViewShareMDIWindow(UISubsystem ui, Friend remote) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "viewshare", ui);
        this.remote = remote;
        setTitle("Share of "+remote.getNickname());

        model = new ViewShareTreeModel(remote, ui);
        tree = new JTree(model);

        ((JScrollPane)xui.getComponent("treepanel")).setViewportView(tree);

        postInit();
    }

    public void shareBaseListReceived(String[] shareBaseNames) {
        if(T.t)T.info("Callback got back - filling with "+shareBaseNames.length+" share bases.");
        model.shareBaseNamesRevieved(shareBaseNames);
    }

    public void directoryListingReceived(int shareBaseIndex, String path, String[] files) {
        if (shareBaseIndex > model.getRoot().getChildCount()-1) {
            if(T.t)T.error("ShareBaseIndex mismatch - maybe the remote user added a share??");
        } else {
            ViewShareShareBaseNode n = model.getRoot().getChildAt(shareBaseIndex);
            if(T.t)T.info("Updating path for share base at index "+shareBaseIndex+": "+n);
            n.pathUpdated(path, files);
        }
    }

    public String getIdentifier() {
        return "viewshare"+remote.getGuid();
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }
}
