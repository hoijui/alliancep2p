package org.alliance.launchers.ui;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.resourceloader.ResourceLoader;
import org.alliance.Subsystem;
import org.alliance.Version;
import org.alliance.core.*;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;

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
        core.setUiCallback(new UICallback() {
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
                    ti.displayMessage("Chat message", core.getFriendManager().nickname(pmi.getFromGuid())+": "+pmi.getMessage(), TrayIcon.INFO_MESSAGE_TYPE);
                } else {
                    ti.displayMessage("Alliance needs your attention.", "Click here to find out why.", TrayIcon.INFO_MESSAGE_TYPE);
                }
                balloonClickHandler = new Runnable() {
                    public void run() {
                        openUI();
                    }
                };
            }

            public void handleError(final Throwable e, final Object source) {
                ti.displayMessage(e.getClass().getName(), e+"\n"+source+"\n\nClick here to view detailed error (and send error report)", TrayIcon.ERROR_MESSAGE_TYPE);
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
        tray = SystemTray.getDefaultSystemTray();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JPopupMenu m = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Open Alliance");
        mi.setFont(new Font(mi.getFont().getName(), mi.getFont().getStyle() | Font.BOLD, mi.getFont().getSize()));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openUI();
            }
        });
        m.add(mi);

        m.addSeparator();

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
        m.add(mi);

        JMenu shutdown = new JMenu("Shutdown");

        mi = new JMenuItem("Forever (not recommended)");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shutdown();
            }
        });
        shutdown.add(mi);
        shutdown.addSeparator();

        mi = new JMenuItem("for 6 hours");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60*6);
            }
        });
        shutdown.add(mi);

        mi = new JMenuItem("for 3 hours");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60*3);
            }
        });
        shutdown.add(mi);

        mi = new JMenuItem("for 1 hour");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(60);
            }
        });
        shutdown.add(mi);

        mi = new JMenuItem("for 30 minutes");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restart(30);
            }
        });
        shutdown.add(mi);

        m.add(shutdown);

        ti = new TrayIcon(new ImageIcon(rl.getResource("gfx/icons/alliance.png")),
                "Alliance v"+Version.VERSION+" build "+Version.BUILD_NUMBER+"\n" +
                "Download: "+core.getNetworkManager().getBandwidthIn().getHumanReadable()+"\n" +
                "Upload: "+core.getNetworkManager().getBandwidthOut().getHumanReadable(), m);

        ti.setIconAutoSize(false);
        ti.addBalloonActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (balloonClickHandler != null) balloonClickHandler.run();
            }
        });

        tray.addTrayIcon(ti);

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
                            tray.removeTrayIcon(ti);
                        }
                    });
                    tray = null;
                }
            }
        });
    }

    private void restart(int delay) {
        try {
            if (tray != null && ti != null) {
                ti.displayMessage("", "Shutting down...", TrayIcon.NONE_MESSAGE_TYPE);
                balloonClickHandler = null;
            }
            core.restartProgram(false, delay);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public synchronized void shutdown() {
        if (tray != null && ti != null) {
            ti.displayMessage("", "Shutting down...", TrayIcon.NONE_MESSAGE_TYPE);
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
            tray.removeTrayIcon(ti);
            tray = null;
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
            ui.init(ResourceSingelton.getRl(), core, false);
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
