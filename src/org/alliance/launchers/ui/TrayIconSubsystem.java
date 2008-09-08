package org.alliance.launchers.ui;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.resourceloader.ResourceLoader;
import com.stendahls.util.TextUtils;
import org.alliance.Subsystem;
import org.alliance.Version;
import org.alliance.core.*;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-03
 * Time: 14:10:37
 */
public class TrayIconSubsystem implements Subsystem, Runnable {
    private CoreSubsystem core;
    private Subsystem ui;
    private ResourceLoader rl;
    private SystemTray tray;
    private TrayIcon ti;
    private Runnable balloonClickHandler;

    public void init(ResourceLoader rl, Object... params) throws Exception {
        this.rl = rl;
        core = (CoreSubsystem)params[0];
        extractNativeLibs();
        initTray();
        core.setUICallback(new UICallback() {
            public void firstDownloadEverFinished() {}
            public void callbackRemoved() {}
            public void signalFileDatabaseFlushStarting() {}
            public void signalFileDatabaseFlushComplete() {}
            public void nodeOrSubnodesUpdated(Node node) {}
            public void noRouteToHost(Node node) {}
            public void searchHits(int srcGuid, int hops, List<SearchHit> hits) {}
            public void trace(int level, String message, Exception stackTrace) {}
            public void statusMessage(String s) {}
            public void toFront() {}
            public void signalFriendAdded(Friend friend) {}
            public boolean isUIVisible() { return false; }
            public void logNetworkEvent(String event) {}
            public void receivedShareBaseList(Friend friend, String[] shareBaseNames) {}
            public void receivedDirectoryListing(Friend friend, int i, String s, String[] files) {}

            public void newUserInteractionQueued(NeedsUserInteraction ui) {
                if (ui instanceof PostMessageInteraction) {
                    PostMessageInteraction pmi = (PostMessageInteraction)ui;
                    String msg = pmi.getMessage().replaceAll("\\<.*?\\>", "");      // Strip html
                    if (pmi instanceof PostMessageToAllInteraction) {
                        if (core.getSettings().getInternal().getShowpublicchatmessagesintray() != 0)
                            ti.displayMessage("Chat message", core.getFriendManager().nickname(pmi.getFromGuid())+": "+msg, TrayIcon.MessageType.INFO);
                    } else {
                        if (core.getSettings().getInternal().getShowprivatechatmessagesintray() != 0)
                            ti.displayMessage("Private chat message", core.getFriendManager().nickname(pmi.getFromGuid())+": "+msg, TrayIcon.MessageType.INFO);
                    }
                } else {
                    if (core.getSettings().getInternal().getShowsystemmessagesintray() != 0)
                        ti.displayMessage("Alliance needs your attention.", "Click here to find out why.", TrayIcon.MessageType.INFO);
                }
                balloonClickHandler = new Runnable() {
                    public void run() {
                        openUI();
                    }
                };
            }

            public void handleError(final Throwable e, final Object source) {
                ti.displayMessage(e.getClass().getName(), e+"\n"+source+"\n\nClick here to view detailed error (and send error report)", TrayIcon.MessageType.ERROR);
                e.printStackTrace();
                balloonClickHandler = new Runnable() {
                    public void run() {
                        try {
                            e.printStackTrace();
                            //report error. Use reflection to init dialogs because we want NO references to UI stuff in this
                            //class - we want this class to load fast (ie load minimal amount of classes)
                            Object errorDialog = Class.forName("com.stendahls.ui.ErrorDialog").newInstance();
                            Method m = errorDialog.getClass().getMethod("init", Throwable.class, boolean.class);
                            m.invoke(errorDialog, e, false);
                        } catch(Throwable t) {
                            t.printStackTrace();
                        }
                    }
                };
            }
        });
    }

    private void extractNativeLibs() throws IOException {
        String name = "tray.dll";
        File f = new File(name);
        if (!f.exists()) {
            if(T.t)T.info("Extracting lib: "+name);
            FileOutputStream out = new FileOutputStream(f);
            InputStream in = rl.getResourceStream(name);
            byte buf[] = new byte[10*KB];
            int read;
            while((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
            out.close();
            if(T.t)T.info("Done.");
        }
    }

    private void initTray() throws Exception {
        try {
            tray = SystemTray.getSystemTray();
        } catch(UnsatisfiedLinkError e) {
            System.err.println("If you are running on linux you might want to go to the forum at sourceforge and read how to run Alliance on linux. You need to download native libraries to start it.");
            throw new Exception("Native library for system tray missing. If you are running linux you need to download it manually. Look in the forum on sourceforge for more information.");
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        PopupMenu m = new PopupMenu();
        Font f = new Font("Tahoma", 0, 11);
        m.setFont(f);
        MenuItem mi = new MenuItem("Open Alliance");
        mi.setFont(f);
        mi.setFont(new Font(mi.getFont().getName(), mi.getFont().getStyle() | Font.BOLD, mi.getFont().getSize()));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openUI();
            }
        });
        m.add(mi);

        m.addSeparator();

/*
        disabled because there's a risk that port is still bound when starting up
        mi = new JMenuItem("Restart");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ti.displayMessage("", "Restarting Alliance...", TrayIcon.NONE_MESSAGE_TYPE);
                    balloonClickHandler = null;
                    core.restartProgram(false);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        m.add(mi);*/

        Menu shutdown = new Menu("Shutdown");
        shutdown.setFont(f);

        mi = new MenuItem("Forever (not recommended)");
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shutdown();
            }
        });
        shutdown.add(mi);
        shutdown.addSeparator();

        mi = new MenuItem("for 6 hours");
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60*6);
            }
        });
        shutdown.add(mi);

        mi = new MenuItem("for 3 hours");
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60*3);
            }
        });
        shutdown.add(mi);

        mi = new MenuItem("for 1 hour");
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60);
            }
        });
        shutdown.add(mi);

        mi = new MenuItem("for 30 minutes");
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(30);
            }
        });
        shutdown.add(mi);

        m.add(shutdown);
        Toolkit.getDefaultToolkit().getSystemEventQueue().push( new PopupFixQueue(m) );
        
        ti = new TrayIcon(new ImageIcon(rl.getResource("gfx/icons/alliance.png")).getImage(),
                "Alliance", m);
        ti.setImageAutoSize(false);
        
//        ti.addBalloonActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                if (balloonClickHandler != null) balloonClickHandler.run();
//            }
//        });
     
//        tray.addTrayIcon(ti);
        tray.add(ti);

        ti.addActionListener(new ActionListener() {
            private long lastClickAt;
            public void actionPerformed(ActionEvent e) {
                if (System.currentTimeMillis()-lastClickAt < 1000) openUI();
                lastClickAt = System.currentTimeMillis();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (tray != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                        	tray.remove(ti);
//                            tray.removeTrayIcon(ti);
                        }
                    });
                    tray = null;
                }
            }
        });

        // Update tooltip periodically with current transfer rates
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ti.setToolTip("Alliance v" + Version.VERSION + " build " + Version.BUILD_NUMBER + "\nDownload: " + core.getNetworkManager().getBandwidthIn().getCPSHumanReadable() + "\nUpload: " + core.getNetworkManager().getBandwidthOut().getCPSHumanReadable()+"\nOnline: " + core.getFriendManager().getNFriendsConnected() + "/" + core.getFriendManager().getNFriends() + " (" + TextUtils.formatByteSize(core.getFriendManager().getTotalBytesShared()) + ")");
                            }
                        });

                        Thread.sleep(5000);
                    }
                } catch(InterruptedException e) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void restart(int delay) {
        try {
            if (tray != null && ti != null) {
                ti.displayMessage("", "Shutting down...", TrayIcon.MessageType.NONE);
                balloonClickHandler = null;
            }
            core.restartProgram(false, delay);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public synchronized void shutdown() {
        if (tray != null && ti != null) {
            ti.displayMessage("", "Shutting down...", TrayIcon.MessageType.NONE);
            balloonClickHandler = null;
        }

        if (ui != null) {
            ui.shutdown();
            ui = null;
        }
        if (core != null) {
            core.shutdown();
            core = null;
        }
        if (tray != null) {
            //I don't think needed?
//        	tray.removeTrayIcon(ti);
//            tray = null;
        }
        System.exit(0);
    }

    private synchronized void openUI() {
        try {
            if (ui != null) {
                if(T.t)T.info("Subsystem already started.");
                core.uiToFront();
                return;
            }
            Runnable r = (Runnable)Class.forName("org.alliance.launchers.SplashWindow").newInstance();
            SimpleTimer s = new SimpleTimer();
            ui = (Subsystem)Class.forName("org.alliance.ui.UISubsystem").newInstance();
            ui.init(ResourceSingelton.getRl(), core, false, r);
            if(T.t)T.trace("Subsystem UI started in "+s.getTime());
            r.run();
        } catch(Exception t) {
            core.reportError(t, this);
        }
    }

    public void run() {
        openUI();
    }
}
