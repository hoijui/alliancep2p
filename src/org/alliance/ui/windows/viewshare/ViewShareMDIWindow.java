package org.alliance.ui.windows.viewshare;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.core.comm.rpc.GetHashesForPath;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.node.Friend;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.windows.AllianceMDIWindow;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

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

    private JPopupMenu popup;

    private final Icon iconLoading;
    private ImageIcon[] fileTypeIcons;

    public ViewShareMDIWindow(UISubsystem ui, Friend remote) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "viewshare", ui);
        this.remote = remote;
        setTitle("Share of "+remote.getNickname());

        iconLoading = new ImageIcon(ui.getRl().getResource("gfx/icons/loadingsharenode.png"));
        //@todo: this is done in other places too. AND it's a waste of reasources to load these every time
        fileTypeIcons = new ImageIcon[8];
        for(int i=0;i<fileTypeIcons.length;i++) fileTypeIcons[i] = new ImageIcon(ui.getRl().getResource("gfx/filetypes/"+i+".png"));

        model = new ViewShareTreeModel(remote, ui);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setCellRenderer(new ViewShareTreeRenderer());

        tree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath underMouse = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
                    ViewShareTreeNode n = (ViewShareTreeNode)underMouse.getLastPathComponent();
                    boolean mouseClickedOnASelectedNode = false;
                    if (tree.getSelectionPaths() != null) for(TreePath p : tree.getSelectionPaths()) {
                        if (p.getLastPathComponent() == n) {
                            mouseClickedOnASelectedNode = true;
                            break;
                        }
                    }
                    if (!mouseClickedOnASelectedNode) tree.setSelectionPath(underMouse);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });



        ((JScrollPane)xui.getComponent("treepanel")).setViewportView(tree);

        popup = (JPopupMenu)xui.getComponent("popup");


        postInit();
    }

    public void shareBaseListReceived(String[] shareBaseNames) {
        if(T.t)T.info("Callback got back - filling with "+shareBaseNames.length+" share bases.");
        model.shareBaseNamesRevieved(shareBaseNames);
    }

    public void directoryListingReceived(int shareBaseIndex, String path, String[] files) {
        ViewShareShareBaseNode n = model.getRoot().getByShareBase(shareBaseIndex);
        if (n != null) {
            if(T.t)T.info("Updating path for share base at index "+shareBaseIndex+": "+n);
            n.pathUpdated(path, files);
        } else {
            if(T.t)T.error("Could not find share with sharebase "+shareBaseIndex);
        }
    }

    public void EVENT_download(ActionEvent e) {
        for(TreePath p : tree.getSelectionPaths()) {
            ViewShareTreeNode n = (ViewShareTreeNode)p.getLastPathComponent();
            if (!(n instanceof ViewShareFileNode)) {
                OptionDialog.showErrorDialog(ui.getMainWindow(), "You can not download entire root folders");
                return;
            }
        }

        final ArrayList<ViewShareFileNode> paths = new ArrayList<ViewShareFileNode>();
        for(TreePath p : tree.getSelectionPaths()) {
            ViewShareFileNode n = (ViewShareFileNode)p.getLastPathComponent();
            paths.add(n);
        }

        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                for(ViewShareFileNode p : paths) {
                    if (remote.isConnected()) {
                        try {
                            remote.getFriendConnection().send(new GetHashesForPath(p.getShareBaseIndex(), p.getFileItemPath()));
                        } catch (IOException e1) {
                            ui.getCore().reportError(e1, this);
                        }
                    } else {
                        if(T.t)T.error("User not connected!");
                    }
                }
            }
        });
        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
    }

    public String getIdentifier() {
        return "viewshare"+remote.getGuid();
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    private class ViewShareTreeRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);

            if (value instanceof ViewShareLoadingNode) {
                setIcon(iconLoading);
            } else if (value instanceof ViewShareFileNode) {
                ViewShareFileNode n = (ViewShareFileNode)value;
                if (!n.isFolder()) setIcon(fileTypeIcons[FileType.getByFileName(n.getName()).id()]);
            }

            return this;
        }
    }
}
