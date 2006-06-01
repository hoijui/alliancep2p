package org.alliance.ui;

import com.stendahls.nif.ui.framework.GlobalExceptionHandler;
import com.stendahls.nif.ui.framework.SwingDeadlockWarningRepaintManager;
import com.stendahls.nif.ui.framework.UINexus;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.resourceloader.ResourceLoader;
import com.stendahls.ui.ErrorDialog;
import de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel;
import org.alliance.Subsystem;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.ERROR_URL;
import org.alliance.ui.nodetreemodel.NodeTreeModel;
import org.alliance.ui.nodetreemodel.NodeTreeNode;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:13:14
 */
public class UISubsystem implements UINexus, Subsystem {
    private MainWindow mainWindow;
    private ResourceLoader rl;
    private CoreSubsystem core;
    private NodeTreeModel nodeTreeModel;
    private FriendListModel friendListModel;

    public UISubsystem() {
    }

    public void init(ResourceLoader rl, final Object... params) throws Exception {
        this.rl = rl;
        core = (CoreSubsystem)params[0];

        if (SwingUtilities.isEventDispatchThread()) {
            realInit(params);
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    realInit(params);
                }
            });
        }
    }

    private void realInit(Object... params) {
        ErrorDialog.setErrorReportUrl(ERROR_URL);
        ErrorDialog.setExceptionTranslator(new ErrorDialog.ExceptionTranslator() {
            public String translate(Throwable t) {
                Throwable innerError = t;
//                    while(innerError.getCause() != null) innerError = innerError.getCause();

                if (innerError.getStackTrace()[0].toString().indexOf("de.javasoft.plaf.synthetica.StyleFactory$ComponentProperty.hashCode") != -1) {
                    return null; //some wicked blackstar bug probably
                }

                return innerError.toString();
            }
        });

        try {
            UIManager.setLookAndFeel(new SyntheticaBlackStarLookAndFeel());
        } catch(Exception e) {
            e.printStackTrace();
        }

        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        if (T.t) SwingDeadlockWarningRepaintManager.hookRepaints(true, new String[]{"NetworkIndicator"});

        try {
            mainWindow = new MainWindow();
            mainWindow.init(UISubsystem.this, null, params.length > 1 && (Boolean)params[1]);
        } catch(Exception e) {
            handleErrorInEventLoop(e);
        }

        core.setUiCallback(new UIBridge(this, core.getUICallback()));
    }

    private void runInCorrectThread(Runnable r) throws InvocationTargetException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);

    }

    public void handleErrorInEventLoop(Throwable t) {
        handleErrorInEventLoop(null, t, false);
    }

    public void handleErrorInEventLoop(Throwable t, boolean fatal) {
        handleErrorInEventLoop(null, t, fatal);
    }

    public void handleErrorInEventLoop(Window parent, Throwable t, boolean fatal) {
        core.reportError(t, null);
//        try {
//            if (parent == null) parent = getMainWindow();
//            if (parent instanceof JDialog) {
//                new ErrorDialog((JDialog) parent, t, fatal);
//            } else if (parent instanceof JFrame) {
//                new ErrorDialog((JFrame) parent, t, fatal);
//            } else {
//                new ErrorDialog(t, fatal);
//            }
//        } catch (XUIException e) {
//            if (T.t) T.error("Oh no! Could not open error dialog!");
//            e.printStackTrace();
//        }
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public ResourceLoader getRl() {
        return rl;
    }

    public ToolbarActionManager getToolbarActionManager() {
        return mainWindow.getToolbarActionManager();
    }

    public void shutdown() {
        mainWindow.shutdown();
        core.setUiCallback(null);
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    public CoreSubsystem getCore() {
        makeSureThreadNameIsCorrect();
        return core;
    }

    void makeSureThreadNameIsCorrect() {
        if (T.t) {
            //make sure we have a labaled thread name - for testsuite
            if (Thread.currentThread().getName().indexOf(core.getFriendManager().getMe().getNickname()) == -1) {
                String n = Thread.currentThread().getName();
                if (n.indexOf(' ') != -1) n = n.substring(0, n.indexOf(' '));
                Thread.currentThread().setName(n+" -- "+core.getFriendManager().getMe().getNickname());
            }
        }
    }

    public NodeTreeModel getNodeTreeModel(boolean loadIfNeeded) {
        if (nodeTreeModel == null && loadIfNeeded) {
            nodeTreeModel = new NodeTreeModel();
            nodeTreeModel.setRoot(new NodeTreeNode(core.getFriendManager().getMe(), null, this, nodeTreeModel));
        }
        return nodeTreeModel;
    }

    public void purgeNodeTreeModel() {
        nodeTreeModel = null;
    }

    public FriendListModel getFriendListModel() {
        if (friendListModel == null) friendListModel = new FriendListModel(core);
        return friendListModel;
    }
}
