package org.alliance.launchers.ui;

import com.stendahls.nif.util.SimpleTimer;
import org.alliance.Subsystem;
import org.alliance.Version;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.T;
import org.alliance.launchers.OSInfo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:00:56
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    private static final int STARTED_SIGNAL_PORT = 56345;

    public static void main(String[] args) throws Exception {
        System.out.println("Launching Alliance v"+ Version.VERSION+" build "+Version.BUILD_NUMBER);

        checkIfAlreadyRunning();

        boolean runMinimized = args.length > 0 && "/min".equalsIgnoreCase(args[0]);

        Runnable r = null;
        if (!runMinimized) r = (Runnable)Class.forName("org.alliance.launchers.SplashWindow").newInstance();

//        if (T.t) {
//            try {
//                Class.forName("com.stendahls.trace.TraceWindow").newInstance();
//            } catch(Exception e) {
//                System.err.println("Could not open trace window: "+e);
//            }
//        }

        String s = "settings.xml";
        for(int i=0;i<args.length;i++) if (!"/min".equals(args[i])) s = args[i];
        Subsystem core = initCore(s);

        if (OSInfo.supportsTrayIcon()) {
            Subsystem tray = initTrayIcon(core);

            if (!runMinimized) {
                ((Runnable)tray).run(); //open ui
                if (r != null) r.run(); //close splashwindow
            }

            startStartSignalThread(tray);
        } else {
            initUI(core);
            if (r != null) r.run();
        }
    }

    private static void checkIfAlreadyRunning() {
        try {
            Socket s = new Socket("127.0.0.1", STARTED_SIGNAL_PORT);
            s.getInputStream();
            System.out.println("Program already running. Closing this program instance.");
            System.exit(0);
        } catch(IOException e) {
            System.out.println("Program does not seem to be running. Starting.");
        }
    }

    private static Thread signalThread;
    private static ServerSocket signalServerSocket;
    private static void startStartSignalThread(final Subsystem tray) {
        signalThread = new Thread(new Runnable() {
            public void run() {
                try {
                    signalServerSocket = new ServerSocket(STARTED_SIGNAL_PORT, 0, InetAddress.getByName("127.0.0.1"));
                    while(true) {
                        try {
                            Socket s = signalServerSocket.accept(); //connection is made on this port if user wants to open the ui
                            if (signalThread == null) return;
                            s.close();
                            ((Runnable)tray).run(); //open ui
                        } catch(IOException e) {
                            if (signalThread == null) return;
                            e.printStackTrace();
                            try { Thread.sleep(10000); } catch(InterruptedException e1) {}
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
        signalThread.setDaemon(true);
        signalThread.start();
    }

    public static void stopStartSignalThread() {
        if (signalThread != null) {
            Thread t = signalThread;
            signalThread = null;
            try { signalServerSocket.close(); } catch(IOException e) {}
            try { t.join(); } catch(InterruptedException e) {}
        }
    }

    private static Subsystem initTrayIcon(Subsystem core) {
        try {
            SimpleTimer s = new SimpleTimer();
            Subsystem tray = (Subsystem)Class.forName("org.alliance.launchers.ui.TrayIconSubsystem").newInstance();
            tray.init(ResourceSingelton.getRl(), core);
            if(T.t)T.trace("Subsystem TrayIcon started in "+s.getTime());
            return tray;
        } catch(Throwable t) {
            reportError(t);
            return null;
        }
    }

    private static Subsystem initCore(String settings) {
        try {
            SimpleTimer s = new SimpleTimer();
            Subsystem core = (Subsystem)Class.forName("org.alliance.core.CoreSubsystem").newInstance();
            core.init(ResourceSingelton.getRl(), settings);
            if(T.t)T.info("" +
                    "Subsystem CORE started in "+s.getTime());
            return core;
        } catch(Throwable t) {
            reportError(t);
            System.err.println(t);
            System.exit(0);
            return null;
        }
    }

    public static void reportError(Throwable t) {
        try {
            t.printStackTrace();
            //report error. Use reflection to init dialogs because we want NO references to UI stuff in this
            //class - we want this class to load fast (ie load minimal amount of classes)
            Object errorDialog = Class.forName("com.stendahls.ui.ErrorDialog").newInstance();
            Method m = errorDialog.getClass().getMethod("init", new Class[] {Throwable.class, boolean.class});
            m.invoke(errorDialog, new Object[]{t, Boolean.valueOf(true)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initUI(Subsystem core) {
        try {
            System.out.println("starting ui");
            SimpleTimer s = new SimpleTimer();
            Subsystem ui = (Subsystem)Class.forName("org.alliance.ui.UISubsystem").newInstance();
            ui.init(ResourceSingelton.getRl(), core, !OSInfo.supportsTrayIcon());
            if(T.t)T.trace("Subsystem UI started in "+s.getTime());
        } catch(Exception t) {
            reportError(t);
        }
    }
}
