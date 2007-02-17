package org.alliance.ui;

import com.stendahls.XUI.MenuItemDescriptionListener;
import com.stendahls.XUI.XUIFrame;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIManagerEventListener;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.mdi.infonodemdi.InfoNodeMDIManager;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.nif.util.SXML;
import com.stendahls.ui.util.RecursiveBackgroundSetter;
import com.stendahls.util.TextUtils;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
import org.alliance.Version;
import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.PublicChatHistory;
import org.alliance.core.comm.BandwidthAnalyzer;
import org.alliance.core.interactions.*;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.ui.addfriendwizard.AddFriendWizard;
import org.alliance.ui.addfriendwizard.ForwardInvitationNodesList;
import org.alliance.ui.windows.*;
import org.alliance.ui.windows.search.SearchMDIWindow;
import org.alliance.ui.windows.viewshare.ViewShareMDIWindow;

import javax.swing.*;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:25:12
 */
public class MainWindow extends XUIFrame implements MenuItemDescriptionListener, MDIManagerEventListener, Runnable {
    private UISubsystem ui;
    private JLabel statusMessage, shareMessage, uploadMessage, downloadMessage;
    private JProgressBar bandwidthIn, bandwidthOut;
    private ToolbarActionManager toolbarActionManager;
    protected MDIManager mdiManager;

    private AddFriendWizard lastAddFriendWizard;

    private int userInteractionsInProgress = 0;
    private PublicChatMessageMDIWindow publicChat;

    public MainWindow() throws Exception {
    }

    public void init(final UISubsystem ui, StartupProgressListener pml, final boolean shutdownOnClose) throws Exception {
        this.ui = ui;

        pml.updateProgress("Loading main window");
        init(ui.getRl(), "xui/mainwindow.xui.xml");

        bandwidthIn = (JProgressBar)xui.getComponent("bandwidthin");
        bandwidthOut = (JProgressBar)xui.getComponent("bandwidthout");

        ((JButton)xui.getComponent("rescan")).setUI(new MetalButtonUI());

        xui.setEventHandler(this);
        xui.setMenuItemDescriptionListener(this);
        statusMessage = (JLabel)xui.getComponent("statusbar");
        shareMessage = (JLabel)xui.getComponent("sharing");
//        uploadMessage = (JLabel)xui.getComponent("totalup");
//        downloadMessage = (JLabel)xui.getComponent("totaldown");

        setupToolbar();

        setupWindowEvents(shutdownOnClose);

        setupMDIManager();

        pml.updateProgress("Loading main window (friend list)");
        mdiManager.addWindow(new FriendListMDIWindow(mdiManager, ui));
        pml.updateProgress("Loading main window (publc chat)");
        mdiManager.addWindow(publicChat = new PublicChatMessageMDIWindow(ui));
        pml.updateProgress("Loading main window (search)");
        mdiManager.addWindow(new SearchMDIWindow(ui));
        pml.updateProgress("Loading main window (download)");
        mdiManager.addWindow(new DownloadsMDIWindow(ui));

        pml.updateProgress("Loading main window");
        for(PublicChatHistory.Entry e : ui.getCore().getPublicChatHistory().allMessages()) {
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(e.fromGuid), e.message, e.tick);
        }

        mdiManager.selectWindow(publicChat);

        RecursiveBackgroundSetter.setBackground(xui.getComponent("bottompanel"), new Color(0xE3E2E6), false);

        setTitle(ui.getCore().getFriendManager().getMe().getNickname()+" - Alliance v"+ Version.VERSION+" build "+Version.BUILD_NUMBER);

        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        showWindow();

        Thread t = new Thread(this, "Regular Interval UI Update Thread");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        if (ui.getCore().getSettings().getMy().hasUndefinedNickname()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    OptionDialog.showInformationDialog(MainWindow.this, "Welcome to Alliance![p]Before starting you need to enter your nickname.[p]The options window will now open.[p]");
                    try {
                        EVENT_options(null);
                    } catch (Exception e) {
                        ui.handleErrorInEventLoop(e);
                    }
                }
            });
        }

        if (T.t) T.info("done");
    }

    private void setupMDIManager() {
        mdiManager = new InfoNodeMDIManager(ui);
        mdiManager.setEventListener(this);
        ((InfoNodeMDIManager)mdiManager).setMaximumNumberOfWindowsByClass(10);
        ((JPanel)xui.getComponent("applicationArea")).add(mdiManager);
    }

    private void setupToolbar() throws Exception {
        toolbarActionManager = new ToolbarActionManager(SXML.loadXML(xui.getResourceLoader().getResourceStream("toolbaractions.xml")));
        toolbarActionManager.bindToFrame(this);
    }

    public synchronized void setStatusMessage(final String s) {
        setStatusMessage(s, false);
    }

    private Thread messageFadeThread;
    public synchronized void setStatusMessage(final String s, final boolean important) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (s == null || s.length() == 0)
                    statusMessage.setText(" ");
                else
                    statusMessage.setText(s);
            }
        });

        messageFadeThread = new Thread(new Runnable() {
            public void run() {
                Thread myThread = messageFadeThread;
                try {
//                    int c;
//                    if (important) {
//                        changeStatusMessageColor(0xffffff);
//                        Thread.sleep(60);
//
//                        c = 255;
//                        for (int i = 0; i < 25 && messageFadeThread == myThread; i++) {
//                            changeStatusMessageColor(c | c << 8 | c << 16);
//                            c -= 10;
//                            Thread.sleep(30);
//                        }
//                    } else {
//                        c = 200;
//                        for (int i = 0; i < 20 && messageFadeThread == myThread; i++) {
//                            changeStatusMessageColor(c | c << 8 | c << 16);
//                            c -= 10;
//                            Thread.sleep(30);
//                        }
//                    }
                    changeStatusMessageColor(0);
                    if (myThread != messageFadeThread) return;
                    Thread.sleep(5500);
                    if (myThread != messageFadeThread) return;
                    int c = 0;
                    for (int i = 0; i < 23 && messageFadeThread == myThread; i++) {
                        changeStatusMessageColor(c | c << 8 | c << 16);
                        c += 10;
                        Thread.sleep(80);
                    }
                    if (myThread != messageFadeThread) return;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            statusMessage.setText(" ");
                        }
                    });
                } catch (InterruptedException e) {
                    if (T.t) T.error("Problem in progressMessage fade loop: " + e);
                }
            }
        });
        messageFadeThread.setName("MessageFadeThread");
        messageFadeThread.start();
    }

    private synchronized void changeStatusMessageColor(final int color) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusMessage.setForeground(new Color(color));
                statusMessage.repaint();
            }
        });
    }

    private void setupWindowEvents(final boolean shutdown) {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (shutdown) {
                    if(T.t)T.info("Shutting down.");
                    ui.shutdown();
                    ui.getCore().shutdown();
                    System.exit(0);
                } else {
                    if (!ui.getCore().isRunningAsTestSuite()) {
                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(100);
                                    ui.getCore().restartProgram(false);
                                } catch (Exception e1) {
                                    ui.handleErrorInEventLoop(e1);
                                }
                            }
                        });
                        t.start();
                    }
                }
            }
        });
    }

    public void tryQuit() {
        shutdown();
    }

    public void showMenuItemDescription(String description) {
        setStatusMessage(description);
    }

    boolean shuttingDown=false;
    public boolean shutdown() {
        if (shuttingDown) return true;
        shuttingDown = true;

        saveWindowState();
        setVisible(false);
        dispose();
        ui.shutdown();
        return true;
    }

    public void saveWindowState() {
        try {
            if(T.t) T.info("Serializing window state");
            FileOutputStream out = new FileOutputStream(System.getProperty("user.home")+"/mainwindow.state"+System.getProperty("tracewindow.id"));
            ObjectOutputStream obj = new ObjectOutputStream(out);

            obj.writeObject(getLocation());
            obj.writeObject(getSize());
            obj.writeInt(getExtendedState());

            obj.flush();
            obj.close();
        } catch(Exception e) {
            if(T.t)T.error("Could not save window state "+e);
        }
    }

    public void showWindow() {
        if(T.t) T.info("Deserializing window state");
        try {
            FileInputStream in = new FileInputStream(System.getProperty("user.home")+"/mainwindow.state"+System.getProperty("tracewindow.id"));
            ObjectInputStream obj = new ObjectInputStream(in);

            setLocation((Point)obj.readObject());
            setSize((Dimension)obj.readObject());
            if (getRootPane().getUI() instanceof SyntheticaRootPaneUI)
                ((SyntheticaRootPaneUI)getRootPane().getUI()).setMaximizedBounds(this);
            setExtendedState(obj.readInt());
            obj.close();
            setVisible(true);
        } catch(Exception e) {
            display();
            Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
            if (ss.width <= 1024) setExtendedState(MAXIMIZED_BOTH);
        }
        toFront();
    }

    public void titleChanged(MDIWindow source, String newTitle) {
    }

    public void windowSelected(MDIWindow source) {
    }

    public ToolbarActionManager getToolbarActionManager() {
        return toolbarActionManager;
    }

    public MDIManager getMDIManager() {
        return mdiManager;
    }

    public void chatMessage(int guid, String message, long tick) throws Exception {
        PrivateChatMessageMDIWindow w = (PrivateChatMessageMDIWindow)mdiManager.getWindow("msg"+guid);
        if (w == null) {
            w = new PrivateChatMessageMDIWindow(ui, guid);
            mdiManager.addWindow(w);
        }
        if (message != null) w.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick);
    }

    public void publicChatMessage(int guid, String message, long tick) throws Exception {
        if (message != null) {
            if(T.t)T.info("Received public chat message: "+message);
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick);
            ui.getCore().getPublicChatHistory().addMessage(tick,  guid, message);
        }
    }

    public void viewShare(Node f) throws Exception {
        ArrayList<MDIWindow> al = new ArrayList<MDIWindow>();
        for(MDIWindow w : mdiManager) al.add(w);
        for(MDIWindow w : al) {
            if (w instanceof ViewShareMDIWindow) mdiManager.removeWindow(w);
        }
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare" + f.getGuid());
        if (w == null) {
            w = new ViewShareMDIWindow(ui, f);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_myshare(ActionEvent e) throws Exception {
        viewShare(ui.getCore().getFriendManager().getMe());
    }

    public SearchMDIWindow getSearchWindow() {
        return (SearchMDIWindow)mdiManager.getWindow("Search");
    }

    public TraceMDIWindow getTraceWindow() {
        if (mdiManager != null)
            return (TraceMDIWindow)mdiManager.getWindow("trace");
        else
            return null;
    }

    public void createTraceWindow() throws Exception {
        TraceMDIWindow w = (TraceMDIWindow)mdiManager.getWindow("trace");
        if (w == null) {
            w = new TraceMDIWindow(ui);
            mdiManager.addWindow(w);
        }
    }

    public ConnectionsMDIWindow getConnectionsWindow() {
        return (ConnectionsMDIWindow)mdiManager.getWindow("connections");
    }

    public DownloadsMDIWindow getDownloadsWindow() {
        return (DownloadsMDIWindow)mdiManager.getWindow("downloads");
    }

    public UploadsMDIWindow getUploadsWindow() {
        return (UploadsMDIWindow)mdiManager.getWindow("uploads");
    }

    public MDIWindow getFriendMDIWindow() {
        return (FriendsTreeMDIWindow)mdiManager.getWindow("friends");
    }

    public FriendListMDIWindow getFriendListMDIWindow() {
        return (FriendListMDIWindow)mdiManager.getWindow("friendlist");
    }

    public ConsoleMDIWindow getConsoleMDIWindow() {
        return (ConsoleMDIWindow)mdiManager.getWindow("console");
    }

    public void run() {
        while(!shuttingDown) {
            if (isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (mdiManager != null && getConnectionsWindow() != null) getConnectionsWindow().updateConnectionData();
                        if (mdiManager != null && getDownloadsWindow() != null) getDownloadsWindow().update();
                        if (mdiManager != null && getUploadsWindow() != null) getUploadsWindow().update();
                        if (mdiManager != null && getFriendListMDIWindow() != null) getFriendListMDIWindow().update();

                        shareMessage.setText("Share: "+TextUtils.formatByteSize(ui.getCore().getShareManager().getFileDatabase().getTotalSize())+" in "+ui.getCore().getShareManager().getFileDatabase().getNumberOfFiles()+" files");
//                        uploadMessage.setText("Up: "+TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthOut().getTotalBytes()));
//                        downloadMessage.setText("Down: "+TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthIn().getTotalBytes()));

                        updateBandwidth("Downloading", "downloaded", bandwidthIn, ui.getCore().getNetworkManager().getBandwidthIn());
                        updateBandwidth("Uploading", "uploaded", bandwidthOut, ui.getCore().getNetworkManager().getBandwidthOut());

                        ((JSpeedDiagram)xui.getComponent("speeddiagram")).update(ui.getCore());
                    }

                    private void updateBandwidth(String s, String s2, JProgressBar pb, BandwidthAnalyzer a) {
                        double curr = a.getCPS();
                        double max = a.getHighestCPS();
                        pb.setString(a.getCPSHumanReadable());
                        pb.setStringPainted(true);
                        if (max == 0)
                            pb.setValue(0);
                        else
                            pb.setValue((int)(curr*100/max));
                        pb.setToolTipText("<html>"+s+" at "+a.getCPSHumanReadable()+"<br>Speed record: "+a.getHighestCPSHumanReadable()+"<br>Total bytes "+s2+": "+TextUtils.formatByteSize(a.getTotalBytes())+"</html>");
                    }
                });
            }

//            NeedsUserInteraction nui;
//            while((nui = ui.getCore().fetchUserInteraction()) != null) {
//                final NeedsUserInteraction nui1 = nui;
//                try {
//                    SwingUtilities.invokeAndWait(new Runnable() {
//                        public void run() {
//                            handleNeedsUserInteraction(nui1);
//                        }
//                    });
//                } catch (InterruptedException e) {
//                } catch (InvocationTargetException e) {
//                }
//            }

//            if(T.t)T.trace("userInteractionsInProgress: "+userInteractionsInProgress+", in que: "+ui.getCore().getAllUserInteractionsInQue().size());
            final boolean[] removedAUserInteraction = new boolean[]{false};
            for(NeedsUserInteraction nui : ui.getCore().getAllUserInteractionsInQue()) {
                if (userInteractionsInProgress == 0 || nui.canRunInParallelWithOtherInteractions()) {
                    if(T.t)T.info("running user interaction: "+nui);
                    ui.getCore().removeUserInteraction(nui);
                    final NeedsUserInteraction nui1 = nui;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            handleNeedsUserInteraction(nui1);
                        }
                    });
                    removedAUserInteraction[0] = true;
                    break;
                }
            }

            if (!removedAUserInteraction[0]) {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            }
        }
    }

    // @todo: move this logic in separate class - it's getting too big
    private void handleNeedsUserInteraction(NeedsUserInteraction nui) {
        userInteractionsInProgress++;
        try {
            if (nui instanceof PostMessageInteraction) {
                PostMessageInteraction pmi = (PostMessageInteraction)nui;
                try {
                    if (pmi instanceof PostMessageToAllInteraction)
                        publicChatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getTick());
                    else
                        chatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getTick());
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof PleaseForwardInvitationInteraction) {
                final PleaseForwardInvitationInteraction pmi = (PleaseForwardInvitationInteraction)nui;
                try {
                    if (ui.getCore().getSettings().getInternal().getAlwaysallowfriendstoconnect() > 0) {
                        forwardInvitation(pmi);
                    } else {
                        ForwardInvitationDialog d = new ForwardInvitationDialog(ui, pmi); //blocks
                        if (d.hasPressedYes()) forwardInvitation(pmi);
                        if (d.alwaysAllowInvite()) ui.getCore().getSettings().getInternal().setAlwaysallowfriendstoconnect(1);
                    }
                } catch(Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof NeedsToRestartBecauseOfUpgradeInteraction) {
                if (ui.getCore().getAwayManager().isAway() || OptionDialog.showQuestionDialog(this, "A new version of Alliance has been downloaded and installed in the background (the upgrade was verified using a 2048 bit RSA certificate).[p] You need to restart Alliance to use the new version. Note that it will take about two minutes before Alliance starts again. Would you like to do this now?[p]")) {
                    try {
                        ui.getCore().restartProgram(true);
                    } catch (IOException e) {
                        ui.handleErrorInEventLoop(e);
                    }
                } else {
                    //wait for 5 minutes and then ask again.
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try { Thread.sleep(1000*60*5); } catch(InterruptedException e) {}
                            ui.getCore().invokeLater(new Runnable() {
                                public void run() {
                                    ui.getCore().queNeedsUserInteraction(new NeedsToRestartBecauseOfUpgradeInteraction());
                                }
                            });
                        }
                    });
                    t.start();
                }
            } else if (nui instanceof ForwardedInvitationInteraction) {
                ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction)nui;
                if (ui.getCore().getFriendManager().getFriend(fii.getFromGuid()) != null && ui.getCore().getFriendManager().getFriend(fii.getFromGuid()).isConnected()) {
                    if(T.t)T.error("Already was connected to this friend!!");
                } else {
                    if (OptionDialog.showQuestionDialog(this, fii.getRemoteName()+" wants to connect to you. "+fii.getRemoteName()+" has a connection to "+fii.getMiddleman(ui.getCore()).getNickname()+". [p]Do you want to connect to "+fii.getRemoteName()+"?[p]")) {
                        try {
                            ui.getCore().getInvitaitonManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(ui.getCore()));
                            openWizardAt(AddFriendWizard.STEP_ATTEMPT_CONNECT, fii.getFromGuid());
                        } catch(Exception e) {
                            ui.handleErrorInEventLoop(e);
                        }
                    } else {
                        //nothing
                    }
                }
            } else if (nui instanceof NewFriendConnectedUserInteraction) {
                NewFriendConnectedUserInteraction i = (NewFriendConnectedUserInteraction)nui;
                String name = ui.getCore().getFriendManager().nickname(i.getGuid());
                if (lastAddFriendWizard != null) lastAddFriendWizard.connectionWasSuccessful();

                if (ui.getCore().doesInterationQueContain(ForwardedInvitationInteraction.class) || new ForwardInvitationNodesList.ForwardInvitationListModel(ui).getSize() == 0) {
                    if (lastAddFriendWizard != null) lastAddFriendWizard.getOuterDialog().dispose();
                    OptionDialog.showInformationDialog(this, "You have successfully connected to "+name+"!");
                    //after this method completes the next pending interaction will be processed.
                } else {
//                    OptionDialog.showInformationDialog(this, "You have successfully connected to "+name+". Congratulations![p] You will now be shown a list of all connections "+name+" has. This way you can connect to even more people.[p]");
                    OptionDialog.showInformationDialog(this, "You have successfully connected to "+name+"!");
                    try {
                        openWizardAt(AddFriendWizard.STEP_FORWARD_INVITATIONS);
                    } catch (Exception e) {
                        ui.handleErrorInEventLoop(e);
                    }
                }
                try {
                    getFriendListMDIWindow().updateMyLevelInformation();
                } catch (IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof FriendAlreadyInListUserInteraction) {
                FriendAlreadyInListUserInteraction i = (FriendAlreadyInListUserInteraction)nui;
                String name = ui.getCore().getFriendManager().nickname(i.getGuid());
                if(T.t)T.trace("Last wizard: "+lastAddFriendWizard);
                if (lastAddFriendWizard != null) {
                    lastAddFriendWizard.getOuterDialog().dispose();
                    if(T.t)T.trace("Wizard disposed");
                }
                // no need to display the below. The user should not mind about this.
                //OptionDialog.showInformationDialog(this, "You already have a connection to "+name+". IP-Adress information was updated for this connection.");
            } else {
                System.out.println("unknown: "+nui);
            }
        } finally {
            userInteractionsInProgress--;
        }
    }

    public void forwardInvitation(final PleaseForwardInvitationInteraction pmi) {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().forwardInvitation(pmi);
                } catch(final IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ui.handleErrorInEventLoop(e);
                        }
                    });
                }
            }
        });
    }

    public void EVENT_options(ActionEvent e) throws Exception {
        new OptionsWindow(ui);
//        String s = JOptionPane.showInputDialog("Enter upload throttle, in kb/s (kilobyte per second).\n0 means no limit.\nIf you have 1mbit upstream a value of 70 is recommended.\nThis value will be saved in your configuration file.", ui.getCore().getSettings().getInternal().getUploadthrottle()/KB);
//        if (s != null) {
//            int limit = Integer.parseInt(s.trim());
//            ui.getCore().getSettings().getInternal().setUploadthrottle(limit);
//            ui.getCore().saveSettings();
//            ui.getCore().getNetworkManager().getUploadThrottle().setRate(limit*KB);
//        }
    }

    public void EVENT_addshare(ActionEvent e) throws Exception {
        new OptionsWindow(ui, true);
    }

    public void EVENT_trace(ActionEvent e) throws Exception {
        createTraceWindow();
    }

    public void EVENT_hide(ActionEvent e) throws Exception {
        setVisible(false);
    }

    public void EVENT_addally(ActionEvent e) throws IOException {
        String invitation = JOptionPane.showInputDialog(ui.getMainWindow(), "Enter the connection code you got from your friend: ");
        try {
            if (invitation != null) ui.getCore().getInvitaitonManager().attemptToBecomeFriendWith(invitation.trim(), null, 0);
        } catch(EOFException ex) {
            OptionDialog.showErrorDialog(this, "Your connection code is corrupt. It seems to be too short. Maybe you did not enter all characters? Please try again. If that doesn't help try with a new code.");
        }
    }

    public void EVENT_rescan(ActionEvent e) {
        ui.getCore().getShareManager().getShareMonitor().startScan(true);
    }

    public void EVENT_console(ActionEvent e) throws Exception {
        mdiManager.addWindow(new ConsoleMDIWindow(ui));
    }

    public void EVENT_connections(ActionEvent e) throws Exception {
        mdiManager.addWindow(new ConnectionsMDIWindow(ui));
    }

    public void EVENT_changelog(ActionEvent e) throws Exception {
        mdiManager.addWindow(new WelcomeMDIWindow(ui));
    }

    public void EVENT_uploads(ActionEvent e) throws Exception {
        mdiManager.addWindow(new UploadsMDIWindow(ui));
    }

    public void EVENT_dups(ActionEvent e) throws Exception {
        mdiManager.addWindow(new DuplicatesMDIWindow(ui));
    }

    public void EVENT_friendtree(ActionEvent e) throws Exception {
        mdiManager.addWindow(new FriendsTreeMDIWindow(mdiManager, ui));
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        openWizard();
    }

    public void openWizard() throws Exception {
        if(T.t)T.ass(lastAddFriendWizard == null || !lastAddFriendWizard.getOuterDialog().isVisible(), "Wizard already open!");
        lastAddFriendWizard = AddFriendWizard.open(ui, AddFriendWizard.STEP_INTRO);
        lastAddFriendWizard.getOuterDialog().display();
    }

    public void openWizardAt(int step, Integer invitationFromGuid) throws Exception {
        if (lastAddFriendWizard != null) if(T.t)T.trace("visible: "+lastAddFriendWizard.getOuterDialog().isVisible());
        if (lastAddFriendWizard != null && lastAddFriendWizard.getOuterDialog().isVisible()) {
            if(T.t)T.ass(step == AddFriendWizard.STEP_FORWARD_INVITATIONS || step == AddFriendWizard.STEP_ATTEMPT_CONNECT, "No support for starting at step "+step+" like this");
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            if (step == AddFriendWizard.STEP_FORWARD_INVITATIONS)
                lastAddFriendWizard.goToForwardInvitations();
            else
                lastAddFriendWizard.goToAttemptConnect();
        } else {
            lastAddFriendWizard = AddFriendWizard.open(ui, step);
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            lastAddFriendWizard.getOuterDialog().display();
        }
    }

    public void openWizardAt(int step) throws Exception {
        openWizardAt(step, null);
    }

    public void shareBaseListReceived(Friend friend, String[] shareBaseNames) {
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare"+friend.getGuid());
        if (w == null) {
            if(T.t)T.error("Could not find view share window for "+friend);
        } else {
            w.shareBaseListReceived(shareBaseNames);
        }
    }

    public void directoryListingReceived(Friend friend, int shareBaseIndex, String path, String[] files) {
        ViewShareMDIWindow w = (ViewShareMDIWindow)mdiManager.getWindow("viewshare"+friend.getGuid());
        if (w == null) {
            if(T.t)T.error("Could not find view share window for "+friend);
        } else {
            w.directoryListingReceived(shareBaseIndex, path, files);
        }
    }

    public PublicChatMessageMDIWindow getPublicChat() {
        return publicChat;
    }
}
