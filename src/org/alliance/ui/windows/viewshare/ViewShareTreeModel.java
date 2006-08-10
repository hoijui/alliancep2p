package org.alliance.ui.windows.viewshare;

import com.stendahls.ui.T;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.rpc.GetShareBaseList;
import org.alliance.core.node.Friend;
import org.alliance.ui.UISubsystem;

import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:14:33
 */
public class ViewShareTreeModel extends DefaultTreeModel {
    private Friend friend;
    private CoreSubsystem core;
    private UISubsystem ui;

    public ViewShareTreeModel(final Friend friend, final UISubsystem ui) throws IOException {
        super(new ViewShareRootNode());
        this.friend = friend;
        this.ui = ui;
        core = ui.getCore();

        getRoot().setModel(this);

        //send get share base list to remote - answer will come asynchronously
        core.invokeLater(new Runnable() {
            public void run() {
                if (friend.isConnected()) {
                    try {
                        friend.getFriendConnection().send(new GetShareBaseList());
                    } catch (IOException e) {
                        core.reportError(e, this);
                    }
                } else {
                    if(T.t)T.error("Friend is not connected! "+friend);
                }
            }
        });
    }

    public void shareBaseNamesRevieved(String[] shareBaseNames) {
        getRoot().fill(shareBaseNames);
        nodeStructureChanged(getRoot());
    }

    public ViewShareRootNode getRoot() {
        return (ViewShareRootNode)super.getRoot();
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public Friend getFriend() {
        return friend;
    }

    public UISubsystem getUi() {
        return ui;
    }
}
