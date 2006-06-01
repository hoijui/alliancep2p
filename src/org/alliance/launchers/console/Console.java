package org.alliance.launchers.console;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.Subsystem;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-07
 * Time: 16:32:01
 * To change this template use File | Settings | File Templates.
 */
public class Console {
    public interface Printer {
        void println(String line);
    }

    private final static Printer PLAIN_PRINTER = new Printer() {
        public void println(String line) {
            System.out.println(line);
        }
    };

    private CoreSubsystem core;
    private FriendManager manager;
    private Subsystem ui;
    private Printer printer = PLAIN_PRINTER;
    private boolean netLog;

    public Console(CoreSubsystem core) {
        this.core = core;
        manager = core.getFriendManager();
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public void handleLine(String line) throws Exception {
        String command = line;

        ArrayList<String> params = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(line);
        if (st.hasMoreTokens()) {
            command = st.nextToken();
            while(st.hasMoreTokens()) params.add(st.nextToken());
        }

        if ("list".equals(command)) {
            list();
        } else if ("test".equals(command)) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    try {
                        core.softRestart();
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            });
            t.start();
        } else if ("connect".equals(command)) {
            connect(params.get(0));
        } else if ("Ping".equals(command)) {
            ping();
        } else if ("startnetlog".equals(command)) {
            startnetlog();
        } else if ("contains".equals(command)) {
            contains(params);
        } else if ("dups".equals(command)) {
            dups();
        } else if ("ui".equals(command)) {
            ui();
        } else if ("killui".equals(command)) {
            killUI();
        } else if ("share".equals(command)) {
            share(params);
        } else if ("gc".equals(command)) {
            gc();
        } else if ("cleardups".equals(command)) {
            cleardups();
        } else if ("threads".equals(command)) {
            threads();
        } else if ("sl".equals(command) || "searchLocal".equals(command)) {
            searchLocal(params);
        } else if ("Search".equals(command)) {
            search(params);
        } else if ("scan".equals(command)) {
            scan();
        } else if ("bye".equals(command)) {
            bye();
        } else if ("error".equals(command)) {
            throw new Exception("test error");
        } else {
            printer.println("Unknown command "+command);
        }
    }

    private void cleardups() {
        core.getFileManager().getFileDatabase().clearDuplicates();
    }

    private void startnetlog() {
        netLog = true;
        printer.println("Net log is now on.");
    }

    private final static DateFormat FORMAT = new SimpleDateFormat("HH:mm yyyyMMdd");
    public void logNetworkEvent(String event) {
        if (netLog) printer.println(FORMAT.format(new Date())+" "+event);
    }

    private void dups() throws IOException {
        Collection<String> dups = core.getFileManager().getFileDatabase().getDuplicates();
        printer.println("Duplicates: ");
        for(String s : dups) {
            Hash h = core.getFileManager().getFileDatabase().getHashForDuplicate(s);
            FileDescriptor fd = core.getFileManager().getFileDatabase().getFd(h);
            printer.println("  "+s+" -> "+fd.getSubpath());
        }
        printer.println(dups.size()+" duplicates in share.");
    }

    private void search(ArrayList<String> params) throws IOException {
        String q = params.get(0);
        manager.getNetMan().sendSearch(q, FileType.EVERYTHING);
    }

    private Thread[] getAllThreads() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while(g.getParent() != null) g = g.getParent();
        Thread threads[] = new Thread[1000];
        g.enumerate(threads);
        return threads;
    }

    private void threads() {
        printer.println("All threads: ");
        for(Thread t : getAllThreads()) {
            if (t == null) break;
            if (t.getThreadGroup() != null)
                printer.println("  "+t.getName()+" ("+t.getThreadGroup().getName()+")");
            else
                printer.println("  "+t.getName());


            StackTraceElement[] elems = t.getStackTrace();
            if (elems != null) {
                for(StackTraceElement e : elems)
                    printer.println("    "+e.toString());
            }
        }
    }

    private void scan() {
        core.getShareManager().getShareMonitor().startScan();
        printer.println("Scanning for new files in share directories (and cache (and downloads))");
    }

    private void searchLocal(ArrayList<String> params) throws IOException {
        FileType ft = FileType.EVERYTHING;
        if (Character.isDigit(params.get(0).charAt(0)) && params.get(0).length() == 1) {
            ft = FileType.getFileTypeById(Integer.parseInt(params.get(0)));
            params.remove(0);
        }

        String query = "";
        for(String s : params) query += s+" ";

        printer.println("Searching in "+ft.description()+"...");
        SimpleTimer st = new SimpleTimer();
        int indices[] = core.getShareManager().getFileDatabase().getKeywordIndex().search(query, 100, ft);
        printer.println("...completed in "+st.getTime()+".");
        for(int i : indices) {
            printer.println("  "+core.getShareManager().getFileDatabase().getFd(i));
        }
    }

    private void gc() {
        System.gc();
        System.gc();
        long used = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        printer.println("Garbage collected. Using "+ TextUtils.formatNumber(""+used)+" bytes of memory.");
    }

    private void contains(ArrayList<String> params) {
        printer.println("Result: "+core.getShareManager().getFileDatabase().contains(Hash.createFrom(params.get(0))));
    }

    private void share(ArrayList<String> params) throws IOException {
        printer.println("All complete files: ");
        ArrayList<Hash> al = new ArrayList<Hash>(core.getFileManager().getFileDatabase().getAllHashes());
        for(Hash h : al) {
            FileDescriptor fd = core.getFileManager().getFileDatabase().getFd(h);
            printer.println("  "+fd);
        }
        printer.println("");

        printer.println("Incomplete files in cache: ");
        for(Hash h : core.getFileManager().getCache().rootHashes()) {
            BlockFile bf = core.getFileManager().getCache().getBlockFile(h);
            printer.println("  "+bf);
        }
        printer.println("");

        printer.println("Incomplete files in downloads: ");
        for(Hash h : core.getFileManager().getDownloadStorage().rootHashes()) {
            BlockFile bf = core.getFileManager().getDownloadStorage().getBlockFile(h);
            printer.println("  "+bf);
        }
        printer.println("");

        printer.println("Sharing "+
                TextUtils.formatByteSize(core.getShareManager().getFileDatabase().getTotalSize())+ " in "+core.getShareManager().getFileDatabase().getNumberOfFiles()+" files.");
        printer.println("");
    }

    private void ui() throws Exception {
        Runnable r = (Runnable)Class.forName("org.alliance.launchers.SplashWindow").newInstance();
        ui = (Subsystem)Class.forName("org.alliance.ui.UISubsystem").newInstance();
        ui.init(ResourceSingelton.getRl(), core);
        r.run(); //closes splashwindow
    }

    private void killUI() {
        if (ui!= null) ui.shutdown();
        printer = PLAIN_PRINTER;
        ui = null;
//        for(Thread t : getAllThreads()) {
//            if (t == null) continue;
//            if (t.getName().indexOf("Thread-5") != -1 || t.getName().indexOf("Thread-6") != -1 ||t.getName().indexOf("AWT") != -1) {
//                printer.println("Killing "+t);
//                t.stop();
//            }
//        }
        printer.println("UI Shutdown.");
    }

    private void runMethod(String className, String methodName) {
        try {
            Class c = Class.forName(className);
            c.getMethod(methodName).invoke(null);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void bye() {
        printer.println("goodbye!");
        core.shutdown();
        System.exit(0);
    }

    private void ping() throws IOException {
        manager.ping();
    }

    private void connect(String nickname) throws IOException {
        printer.println("Connecting to "+nickname+"...");
        Friend f = manager.getFriend(nickname);
        manager.getNetMan().connect(f.getLastKnownHost(), f.getLastKnownPort(), new FriendConnection(manager.getNetMan(), Connection.Direction.OUT, f.getGuid()));
    }

    private void list() {
        printer.println("Friends: ");
        for(Friend f : manager.friends()) {
            printer.println("  "+f.getNickname()+" "+
                    (f.getFriendConnection() == null ? "disconnected" : f.getFriendConnection().getSocketAddress()));
        }
    }
}
