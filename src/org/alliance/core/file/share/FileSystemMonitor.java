package org.alliance.core.file.share;

import qzhang.io.FileSystemWatcher;
import qzhang.io.FileSystemEventListener;
import qzhang.io.NotifyFilters;

import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.alliance.core.T;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.launchers.OSInfo;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-apr-02
 * Time: 21:16:47
 * To change this template use File | Settings | File Templates.
 */
public class FileSystemMonitor {
    private ShareManager manager;
    private ArrayList<Integer> watchers = new ArrayList<Integer>();

    public FileSystemMonitor(ShareManager manager) throws IOException {
        this.manager = manager;
        if (OSInfo.isWindows()) extractNativeLibs();
    }

    public void kill() throws IOException {
        if (!OSInfo.isWindows()) return;
        while(watchers.size() > 0) {
            if(T.t)T.info("Should stop win32 watcher: "+watchers.get(0)+" but removing is buggy so just let it run in background.");
/*            if(T.t)T.info("Stopping win32 watcher: "+watchers.get(0));
            if (!JNotify.removeWatch(watchers.get(0))) {
                if(T.t)T.error("Could not stop watch with id: "+watchers.get(0));
            }*/
            watchers.remove(0);
        }
    }

    public void launch() throws IOException {
        if (!OSInfo.isWindows()) return;
        if (watchers.size() > 0) kill();

        int mask =  JNotify.FILE_CREATED |
                    JNotify.FILE_DELETED |
                    JNotify.FILE_RENAMED;

        for(final ShareBase sb : manager.shareBases()) {
            if(T.t)T.info("Launching file system watcher for "+sb);
            int watchID = JNotify.addWatch(sb.getPath(), mask, true, new JNotifyListener() {
                public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
                    if (!watchers.contains(wd)) return;
                    if(T.t)T.trace("JNotifyTest.fileRenamed() : wd #" + wd + " root = " + rootPath
                            + ", " + oldName + " -> " + newName);
                    rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                    oldName = TextUtils.makeSurePathIsMultiplatform(oldName);
                    newName = TextUtils.makeSurePathIsMultiplatform(newName);
                    if (!rootPath.endsWith("/")) rootPath += '/';
                    manager.getShareScanner().signalFileRenamed(rootPath+oldName, rootPath+newName);
                }

                public void fileModified(int wd, String rootPath, String name) {
                    if (!watchers.contains(wd)) return;
                    if(T.t)T.trace("JNotifyTest.fileModified() : wd #" + wd + " root = " + rootPath
                            + ", " + name);
                }

                public void fileDeleted(int wd, String rootPath, String name) {
                    if (!watchers.contains(wd)) return;
                    if(T.t)T.trace("JNotifyTest.fileDeleted() : wd #" + wd + " root = " + rootPath
                            + ", " + name);
                    rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                    name = TextUtils.makeSurePathIsMultiplatform(name);
                    if (!rootPath.endsWith("/")) rootPath += '/';
                    manager.getShareScanner().signalFileDeleted(rootPath+name);
                }

                public void fileCreated(int wd, String rootPath, String name) {
                    if (!watchers.contains(wd)) return;
                    if(T.t)T.trace("JNotifyTest.fileCreated() : wd #" + wd + " root = " + rootPath
                            + ", " + name);
                    rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                    name = TextUtils.makeSurePathIsMultiplatform(name);
                    if (!rootPath.endsWith("/")) rootPath += '/';
                    manager.getShareScanner().signalFileCreated(rootPath+name);
                }
            });
            if(T.t)T.info("Got watch id: "+watchID);
            watchers.add(watchID);
        }
    }

    private void extractNativeLibs() throws IOException {
        String name = "jnotify.dll";
        File f = new File(name);
        if (!f.exists()) {
            if(org.alliance.core.T.t) T.info("Extracting lib: "+name);
            FileOutputStream out = new FileOutputStream(f);
            InputStream in = manager.getCore().getRl().getResourceStream(name);
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
}
