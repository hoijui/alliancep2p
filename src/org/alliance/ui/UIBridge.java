package org.alliance.ui;

import org.alliance.core.UICallback;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import javax.swing.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 22:05:42
 * To change this template use File | Settings | File Templates.
 */
public class UIBridge implements UICallback {
    private UISubsystem ui;
    private UICallback oldCallback;

    public UIBridge(UISubsystem ui, UICallback oldCallback) {
        this.ui = ui;
        this.oldCallback = oldCallback;
    }

    public void nodeOrSubnodesUpdated(final Node node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (ui.getNodeTreeModel(false) != null) ui.getNodeTreeModel(false).signalNodeChanged(node);
                if (node instanceof Friend) ui.getFriendListModel().signalFriendChanged((Friend)node);
            }
        });
    }

    public void signalFriendAdded(final Friend friend) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (ui.getMainWindow().getFriendMDIWindow() != null) ui.getMainWindow().getFriendMDIWindow().revert();
                    ui.getFriendListModel().signalFriendAdded(friend);
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public boolean isUIVisible() {
        return ui.getMainWindow().isVisible();
    }

    public void logNetworkEvent(String event) {
        if (ui.getMainWindow().getConsoleMDIWindow() != null) {
            ui.getMainWindow().getConsoleMDIWindow().getConsole().logNetworkEvent(event);
        }
    }

    public void noRouteToHost(final Node node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (ui.getNodeTreeModel(false) != null) ui.getNodeTreeModel(false).signalNoRouteToHost(node);
            }
        });
    }

    public void searchHits(final int fromGuid, final int hops, final List<SearchHit> hits) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getMainWindow().getSearchWindow().searchHits(fromGuid, hops, hits);
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public void trace(final int level, final String message, Exception stackTrace) {
        ui.makeSureThreadNameIsCorrect();

        if (ui.getMainWindow().getTraceWindow() == null) return;

        final Exception st;
        if (stackTrace == null)
            st = new Exception();
        else
            st = stackTrace;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getMainWindow().getTraceWindow().trace(level, message, st);
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public void handleError(final Throwable e, final Object source) {
        if (oldCallback != null) {
            oldCallback.handleError(e, source);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ui.handleErrorInEventLoop(new Exception(source+": "+e, e));
                }
            });
        }
    }

    public void statusMessage(final String s) {
        if(T.t)T.info("status message: "+s);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ui.getMainWindow().setStatusMessage(s);
            }
        });
    }

    public void toFront() {
        ui.getMainWindow().setVisible(true);
        ui.getMainWindow().toFront();
    }
}
