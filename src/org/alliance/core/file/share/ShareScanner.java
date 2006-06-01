package org.alliance.core.file.share;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.GB;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.file.filedatabase.FileDescriptor;

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

    public ShareScanner(CoreSubsystem core, ShareManager manager) {
        this.core = core;
        this.manager = manager;
        setName("ShareScanner -- "+core.getSettings().getMy().getNickname());
        setPriority(MIN_PRIORITY);
    }

    public void run() {
        while(alive) {
            manager.getFileDatabase().cleanupDuplicates();

            ArrayList<ShareBase> al = new ArrayList<ShareBase>(manager.shareBases());
            for(ShareBase base : al) {
                if (!alive) break;
                scanPath(base);
            }

//            cleanup();

            try {
                manager.getFileDatabase().flush();
            } catch(IOException e) {
                e.printStackTrace();
            }

            manager.getCore().getUICallback().statusMessage("Share scan complete.");
            if (!alive) break;

            try {
                if(T.t)T.info("Wating for next share scan.");
                Thread.sleep(1000*60*manager.getSettings().getInternal().getSharemanagercycle());
            } catch(Exception e) {}
        }
    }

    // Remove missing files from index
    private void cleanup() {
        try {
            if(T.t)T.info("Cleaning up index...");
            FileDatabase fd = manager.getFileDatabase();
            for(int i = 0; i < fd.getNumberOfFiles(); i++) {
                if(!alive) return;
                try {
                    // If file is missing the descriptor will automatically be removed from the index
                    fd.getFd(i);

                    // Pause every 10 files
                    if(i % 10 == 0)
                        Thread.sleep(500);   // a bit concerened about this. If a user shares 70000 files (some users do) this will take 19 hours..  this is good but should be run is a separate thread. Not sure if there might be threading issues then though 


                } catch (IOException e) {
                    if(T.t)T.warn("Unable to retrieve file descriptor: "+e);
                }
            }
        } catch (InterruptedException e) {}
    }

    private void scanPath(ShareBase base) {
        if(T.t)T.info("Scanning "+base.getPath()+"...");
        scanPathRecursive(base.getPath(),base,1);
    }

    private void scanPathRecursive(String dir, ShareBase base, int level) {
        if (!alive) return;

        if (shouldSkip(dir)) return;

        File top = new File(dir);

        File files[] = top.listFiles();
        if (files != null) for(int i=0;i<files.length;i++) {
            File file = files[i];
            if (file.isDirectory()) {
                try {
                    Thread.sleep(150); //don't look for files too fast - takes 100% cpu on some machines
                } catch(InterruptedException e) {
                }
                if(T.t)T.trace("Scanning "+file.getPath()+"...");
                scanPathRecursive(file.getPath(),base,level+1);
            } else {
                try {
                    if (!manager.getFileDatabase().contains(file.toString())) {
                        if (!file.isHidden()) {
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
        FileDescriptor fd = new FileDescriptor(base.getPath(), file, core.getSettings().getInternal().getHashspeedinmbpersecond());
        manager.getCore().getUICallback().statusMessage("Hashed "+fd.getFilename()+" in "+st.getTime()+" ("+TextUtils.formatByteSize((long)(fd.getSize()/(st.getTimeInMs()/1000.)))+"/s)");
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

    public void startScan() {
        interrupt();
    }
}
