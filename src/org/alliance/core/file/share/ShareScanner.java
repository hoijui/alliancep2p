package org.alliance.core.file.share;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.GB;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.launchers.OSInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:48:44
 * To change this template use File | Settings | File Templates.
 */
public class ShareScanner extends Thread {
    private boolean alive = true;
    private ShareManager manager;
    private long bytesScanned;
    private CoreSubsystem core;
    private boolean shouldBeFastScan = false;
    private boolean scanInProgress = false;
    private boolean scannerHasBeenStarted = false;

    public ShareScanner(CoreSubsystem core, ShareManager manager) {
        this.core = core;
        this.manager = manager;
        setName("ShareScanner -- "+core.getSettings().getMy().getNickname());
        setPriority(MIN_PRIORITY);
    }

    public void run() {
        try { Thread.sleep(6*1000); } catch (InterruptedException e) {} //wait a while before starting first scan
        scannerHasBeenStarted = true;
        while(alive) {
            scanInProgress = true;
            filesScannedCounter = 0;
            manager.getFileDatabase().cleanupDuplicates();

            cleanup();

            ArrayList<ShareBase> al = new ArrayList<ShareBase>(manager.shareBases());
            for(ShareBase base : al) {
                if (!alive) break;
                try {
                    scanPath(base);
                } catch(Exception e) {
                    if(T.t)T.error("Could not scan "+base+": "+e);
                }
            }

            try {
                manager.getFileDatabase().flush();
            } catch(IOException e) {
                e.printStackTrace();
            }

            shouldBeFastScan = false;
            scanInProgress = false;
            if (!alive) break;
            try {
                if(T.t)T.info("Wating for next share scan.");
                if (OSInfo.isWindows())
                    Thread.sleep(1000*60*manager.getSettings().getInternal().getSharemanagercyclewithfilesystemeventsactive());
                else
                    Thread.sleep(1000*60*manager.getSettings().getInternal().getSharemanagercycle());
            } catch(Exception e) {}
        }
    }

    private void cleanup() {
        if(T.t)T.info("Cleaning up index...");
        FileDatabase fd = manager.getFileDatabase();

        int n = fd.getNumberOfFiles();
        for(int i = 0; i < n; i++) {
            if(!alive) return;
            try {
                // If file is missing the descriptor will automatically be removed from the index
                fd.getFd(i, false);

                int sleepEveryXFiles = shouldBeFastScan ? 500 : 50;
                if(i % sleepEveryXFiles == 0) {
                    manager.getCore().getUICallback().statusMessage("Checking share for removed files ("+(i*100/n)+"%)...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }
            } catch (IOException e) {
                if(T.t)T.warn("Unable to retrieve file descriptor: "+e);
            }
        }
    }

    private void scanPath(ShareBase base) throws IOException {
        if(T.t)T.info("Scanning "+base.getPath()+"...");
        scanPathRecursive(base.getPath(),base,1);
    }

    private int filesScannedCounter=0;
    private void scanPathRecursive(String dir, ShareBase base, int level) throws IOException {
        if (!alive) return;

        if (shouldSkip(dir)) return;

        File top = new File(dir);

        File files[] = top.listFiles();
        if (files != null) for(int i=0;i<files.length;i++) {
            File file = files[i];
            file = file.getCanonicalFile();
            if (!shouldBeFastScan && (filesScannedCounter % 50) == 0) try {Thread.sleep(100);} catch (InterruptedException e) {}
            filesScannedCounter++;
            if (file.isDirectory()) {
                if(T.t)T.trace("Scanning "+file.getPath()+"...");
                manager.getCore().getUICallback().statusMessage("Scanning "+file.getPath()+"...");
                scanPathRecursive(file.getPath(),base,level+1);
            } else {
                try {
                    if (!manager.getFileDatabase().contains(file.toString())) {
                        if (!file.isHidden() && file.length() != 0) {
                            hash(base, file);
                        } else {
                            if(T.t)T.debug("Skipping hidden file "+file);
                        }
                    }
                } catch(IOException e) {
                    if(T.t)T.warn("Could not hash file "+file+": "+e);
                }
            }
        }
    }

    private void hash(ShareBase base, File file) throws IOException {
        if (manager.getFileDatabase().isDuplicate(file.getCanonicalPath())) return;

        SimpleTimer st = new SimpleTimer();
        FileDescriptor fd = new FileDescriptor(base.getPath(), file, shouldBeFastScan ? 0 : core.getSettings().getInternal().getHashspeedinmbpersecond(), manager.getCore().getUICallback());
        manager.getCore().getUICallback().statusMessage("Hashed "+fd.getFilename()+" in "+st.getTime()+" ("+ TextUtils.formatByteSize((long)(fd.getSize()/(st.getTimeInMs()/1000.)))+"/s)");
        manager.getFileDatabase().add(fd);

        bytesScanned += fd.getSize();
        if (bytesScanned > manager.getCore().getSettings().getInternal().getPolitehashingintervalingigabytes()*GB) {
            bytesScanned = 0;
            try {
                if(T.t)T.info("Polite scanning in progress. Sleeping for "+manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes()+" minutes for harddrive to cool down.");
                Thread.sleep(manager.getCore().getSettings().getInternal().getPolitehashingwaittimeinminutes()*60*1000);
            } catch(InterruptedException e) {}
        }
    }

    private boolean shouldSkip(String dir) {
        String s = TextUtils.makeSurePathIsMultiplatform(dir);
        if (s.endsWith("/")) s = s.substring(0,s.length()-1);
        return s.endsWith("_incomplete_");
    }

    public void kill() {
        alive = false;
        interrupt();
    }

    public void startScan(boolean fastScan) {
        shouldBeFastScan = fastScan;
        interrupt();
    }

    public ShareManager getManager() {
        return manager;
    }

    /**
     * Invoked by the win32 file watcher when a share change has been detected. This can be called quite often when a file is written to.
     */
    public void signalShareHasChanged() {
        if(T.t)T.trace("Share changed - waking up scan!");
        if (!scanInProgress && scannerHasBeenStarted) startScan(true);
    }
}