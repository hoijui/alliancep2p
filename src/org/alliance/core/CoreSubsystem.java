package org.alliance.core;

import com.stendahls.XUI.XUIException;
import com.stendahls.nif.util.Log;
import com.stendahls.nif.util.SXML;
import com.stendahls.nif.util.xmlserializer.XMLSerializer;
import com.stendahls.resourceloader.ResourceLoader;
import com.stendahls.trace.Trace;
import com.stendahls.trace.TraceHandler;
import com.stendahls.ui.ErrorDialog;
import org.alliance.Subsystem;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.rpc.GetUserInfo;
import org.alliance.core.comm.upnp.UPnPManager;
import org.alliance.core.crypto.CryptoManager;
import org.alliance.core.file.FileManager;
import org.alliance.core.file.share.ShareManager;
import org.alliance.core.interactions.ForwardedInvitationInteraction;
import org.alliance.core.interactions.NeedsToRestartBecauseOfUpgradeInteraction;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;
import org.alliance.core.node.InvitaitonManager;
import org.alliance.core.settings.Settings;
import org.alliance.launchers.ui.Main;
import org.w3c.dom.Document;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the core of the entire Alliance system. Theres not too much code here, it's more of a hub for the entire
 * Core subsystem. Has a instance of FriendManager, FileManager, NetworkManager, InvitatationManager and the UICallback.
 * <p>
 * This class contains the oh-so-important invokeLater method that HAS to be used when code in the Core subsystem need
 * to be run from another thread than the Core thread. Very much like SwingUtilities.invokeLater().
 * <p>
 * There's also a que of NeedsUserInteractions. This is an interface that is used when something happens in the Core
 * subsystem that the user need to interact to. Examples are: chat message received, invitation code received etc..
 * These things are queued by the Core subsystem and fethed by the UI subsystem
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:38:25
 */
public class CoreSubsystem implements Subsystem {
    public final static boolean ALLOW_TO_SEND_UPGRADE_TO_FRIENDS = true;
    private static final int STATE_FILE_VERSION = 4;

    public final static int KB = 1024;
    public final static int MB = 1024*KB;
    public final static long GB = 1024*MB;

    public final static int BLOCK_SIZE = MB;
    public final static String ERROR_URL = "http://maciek.tv/alliance/errorreporter/";

    private ResourceLoader rl;

    private FriendManager friendManager;
    private FileManager fileManager;
    private NetworkManager networkManager;
    private InvitaitonManager invitaitonManager;
    private UPnPManager upnpManager;
    private CryptoManager cryptoManager;
    private PublicChatHistory publicChatHistory;

    private UICallback uiCallback = new NonWindowUICallback();

    private Settings settings;
    private String settingsFile;

    private Log errorLog, traceLog;

    ArrayList<NeedsUserInteraction> userInternactionQue = new ArrayList<NeedsUserInteraction>();

    public CoreSubsystem() {
    }

    public void init(ResourceLoader rl, Object... params) throws Exception {
        errorLog = new Log("error.log");
        traceLog = new Log("trace.log");
        if (T.t && !isRunningAsTestSuite()) {
            final TraceHandler old = Trace.handler;
            Trace.handler = new TraceHandler() {
                public void print(int level, Object message, Exception error) {
                    logTrace(level, message);
                    if (old != null) old.print(level, message, error);
                    propagateTraceMessage(level, String.valueOf(message), error);
                }
            };
        }

        if(T.t)T.info("CoreSubsystem starting...");

        this.rl = rl;
        this.settingsFile = String.valueOf(params[0]);

        Thread.currentThread().setName("Booting Core");

        loadSettings();

        fileManager = new FileManager(this, settings);
        friendManager = new FriendManager(this, settings);
        cryptoManager = new CryptoManager(this);
        networkManager = new NetworkManager(this, settings);
        invitaitonManager = new InvitaitonManager(this, settings);
        upnpManager = new UPnPManager(this);
        publicChatHistory = new PublicChatHistory();

        loadState();

        cryptoManager.init();
        fileManager.init();
        friendManager.init();
        networkManager.init();
        if (!isRunningAsTestSuite()) upnpManager.init();

        Thread.currentThread().setName(friendManager.getMe()+" main");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });

        if(T.t)T.info("CoreSubsystem stated.");
    }

    public boolean isRunningAsTestSuite() {
        return System.getProperty("testsuite") != null;
    }

    public void logTrace(int level, Object message) {
        try {
            traceLog.log("^"+level+" "+message);
        } catch (IOException e) {
            reportError(e, null);
        }
    }

    public void logError(Object error) {
        try {
            errorLog.log(error);
        } catch (IOException e) {
            reportError(e, null);
        }
    }

    private void loadSettings() throws Exception {
        if(T.t)T.info("Loading settings...");
        XMLSerializer s = new XMLSerializer();
        try {
            settings = s.deserialize(SXML.loadXML(new File(settingsFile)), Settings.class);
        } catch(FileNotFoundException e) {
            if(T.t)T.info("No settings file - creating default settings.");
            settings = new Settings();
            saveSettings();
        }
    }

    public void saveState() throws IOException {
        if(T.t)T.info("Saving core state");
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(settings.getInternal().getCorestatefile()));
        out.writeInt(STATE_FILE_VERSION);
        invitaitonManager.save(out);
        networkManager.save(out);
        out.writeObject(userInternactionQue);
        out.writeObject(publicChatHistory);
        out.flush();
        out.close();
    }

    public void loadState() throws Exception {
        try {
            if(T.t)T.info("Loading core state");
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(settings.getInternal().getCorestatefile()));
            int ver = in.readInt();
            if (ver != STATE_FILE_VERSION) {
                if(T.t)T.error("Incorrect state file version. Ignoring old state.");
                in.close();
                return;
            }
            invitaitonManager.load(in);
            networkManager.load(in);
            userInternactionQue = (ArrayList<NeedsUserInteraction>)in.readObject();
            publicChatHistory = (PublicChatHistory)in.readObject();
            for(Iterator i = userInternactionQue.iterator();i.hasNext();) {
                if (i.next() instanceof NeedsToRestartBecauseOfUpgradeInteraction) i.remove(); //we don't need to restart if it's a interaction from the last time we ran alliance
            }
            in.close();
        } catch(FileNotFoundException e) {
            if(T.t)T.info("No core state found.");
        } catch(IOException e) {
            if(T.t)T.error("Could not load state: "+e);
        }
    }

    public void saveSettings() throws Exception {
        if(T.t)T.info("Saving settings");
        XMLSerializer s = new XMLSerializer();
        Document doc = s.serialize(settings);

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File(settingsFile)), "UTF-8");
        out.write(SXML.toString(doc));
        out.flush();
        out.close();
    }

    public ResourceLoader getRl() {
        return rl;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    private boolean shutdownInProgress;
    public synchronized void shutdown() {
        if (shutdownInProgress) return;
        shutdownInProgress = true;
        if(T.t)T.info("Shutting down core..");
        try { updateLastSeenOnlineForFriends(); } catch(Exception e) {
            if(T.t)T.error("Problem when saving friends: "+e);
        }
        try { fileManager.shutdown(); } catch(Exception e) {
            if(T.t)T.error("Problem when shutting down filemanager: "+e);
        }
        try { friendManager.shutdown(); } catch(Exception e) {
            if(T.t)T.error("Problem when shutting down friendmanager: "+e);
        }
        try { networkManager.shutdown(); } catch(Exception e) {
            if(T.t)T.error("Problem when shutting down networkmanager: "+e);
        }
        try { upnpManager.shutdown(); } catch(Exception e) {
            if(T.t)T.error("Problem when shutting down upnpmanager: "+e);
        }
        try { saveSettings(); } catch(Exception e) {
            if(T.t)T.error("Problem when saving settings: "+e);
        }
        try { saveState(); } catch(Exception e) {
            if(T.t)T.error("Problem when saving state: "+e);
        }
        try { Thread.sleep(1500); //wait for GracefulClose RPCs to be sent
        } catch (InterruptedException e) {}
    }

    public void updateLastSeenOnlineForFriends() {
        for(Friend f : friendManager.friends()) {
            if (f.isConnected()) {
                if (settings.getFriend(f.getGuid()) != null) {
                    settings.getFriend(f.getGuid()).setLastseenonlineat(System.currentTimeMillis());
                }
            }
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ShareManager getShareManager() {
        return fileManager.getShareManager();
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void propagateTraceMessage(int level, String message, Exception e) {
        uiCallback.trace(level, message, e);
    }

    public UICallback getUICallback() {
        return uiCallback;
    }

    public void setUiCallback(UICallback uiCallback) {
        if (uiCallback == null)
            this.uiCallback = new NonWindowUICallback();
        else
            this.uiCallback = uiCallback;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * The core package is not thread safe. It all runs in one thread. If another threads want to invoke something
     * in core it should use this method. Same design pattern as SwingUtilities.invokeLater
     * @param runnable
     */
    public void invokeLater(Runnable runnable) {
        networkManager.invokeLater(runnable);
    }

    public void reportError(Throwable e, Object source) {
        if (shutdownInProgress && e instanceof ClosedChannelException) return;
        logError("Error for "+source+": ");
        logError(e);

        if (e instanceof IOException) return;
        if (e instanceof SocketException) return;
        if (e instanceof ClosedChannelException) return;
        if (e instanceof UnresolvedAddressException) return;
        uiCallback.handleError(e, source);
    }

    public void restartProgram(boolean openWithUI) throws IOException {
        restartProgram(openWithUI, 2);
    }

    /**
     * @param openWithUI true if the UI should open once the program is restarted
     * @param restartDelay Wait this many minutes before actually starting the program. This is for "Shutdown for 30 minuters" etc..
     * @throws java.io.IOException if the program cant be restarted
     */
    public void restartProgram(boolean openWithUI, int restartDelay) throws IOException {
        shutdown();

        Main.stopStartSignalThread(); //such a fucking hack. When we run using the normal UI we need to signal the launcher that he needs to stop this startsignalthread
        String s = "."+System.getProperty("file.separator")+"alliance" + //must have exe/script/batch in current directory to start program
                (openWithUI ? "" : " /min") +
                (restartDelay == 0 ? "" : " /w"+restartDelay);

        Runtime.getRuntime().exec(s);
        System.exit(0);
    }

    public void uiToFront() {
        uiCallback.toFront();
    }

    public InvitaitonManager getInvitaitonManager() {
        return invitaitonManager;
    }

    public void queNeedsUserInteraction(NeedsUserInteraction ui) {
        if (ui instanceof PleaseForwardInvitationInteraction &&
                getSettings().getInternal().getAlwaysallowfriendstoconnect() > 0) {
            try {
                //automatically forward this user invitation - the settings are set to do this.
                if(T.t)T.info("Automatically forwarding invitation: "+ui);
                getFriendManager().forwardInvitation((PleaseForwardInvitationInteraction)ui);
                return;
            } catch (IOException e) {
                reportError(e, ui);
            }
        } else if (ui instanceof ForwardedInvitationInteraction &&
                getSettings().getInternal().getAlwaysallowfriendsoffriendstoconnecttome() > 0) {
            try {
                ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction)ui;
                getInvitaitonManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(this), fii.getFromGuid());
                return;
            } catch(Exception e) {
                reportError(e, ui);
            }
        }
        userInternactionQue.add(ui);
        uiCallback.newUserInteractionQueued(ui);
    }

    public NeedsUserInteraction fetchUserInteraction() {
        if (userInternactionQue.size() > 0) {
            NeedsUserInteraction ui = userInternactionQue.get(0);
            userInternactionQue.remove(ui);
            try {
                saveState();
            } catch (IOException e) {
                reportError(e, this);
            }
            return ui;
        }
        return null;
    }

    public NeedsUserInteraction peekUserInteraction() {
        if (userInternactionQue.size() > 0) {
            return userInternactionQue.get(0);
        }
        return null;
    }

    public void removeUserInteraction(NeedsUserInteraction nui) {
        userInternactionQue.remove(nui);
    }

    public boolean doesInterationQueContain(Class<? extends NeedsUserInteraction> c) {
        for(NeedsUserInteraction u : userInternactionQue) {
            if (u.getClass().equals(c)) return true;
        }
        return false;
    }

    public void refreshFriendInfo() throws IOException {
        networkManager.sendToAllFriends(new GetUserInfo());
    }

    public void softRestart() throws IOException {
        if (uiCallback.isUIVisible()) {
            queNeedsUserInteraction(new NeedsToRestartBecauseOfUpgradeInteraction());
        } else {
            restartProgram(false);
        }
    }

    private int GULCounter;
    private long GULTick = System.currentTimeMillis();
    /**
     * Temporary stuff needed to figure out a serious bug.
     */
    public void increaseGULCounter() {
        GULCounter++;
        if (System.currentTimeMillis() - GULTick > 1000*60) {
            if(T.t)T.trace("GUL counter: "+GULCounter);
            if (GULCounter > 300) {
                try {
                    new ErrorDialog(new Exception("UserList flood detected: "+GULCounter+"! <b>This is a fatal error. You need to restart Alliance.</b> Please send this error report by pressing 'send error'."), true);
                } catch(XUIException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            GULCounter = 0;
            GULTick = System.currentTimeMillis();
        }
    }

    /**
     * Logs network messages for debug purposes. An event can be for example "user x went online"
     */
    public void logNetworkEvent(String event) {
        uiCallback.logNetworkEvent(event);
    }

    public UPnPManager getUpnpManager() {
        return upnpManager;
    }

    public List<NeedsUserInteraction> getAllUserInteractionsInQue() {
        return (List<NeedsUserInteraction>)userInternactionQue.clone();
    }

    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }

    public ByteBuffer allocateBuffer(int size) {
        if (settings.getInternal().getUsedirectbuffers() > 0) {
            return ByteBuffer.allocateDirect(size);
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    public PublicChatHistory getPublicChatHistory() {
        return publicChatHistory;
    }
}
