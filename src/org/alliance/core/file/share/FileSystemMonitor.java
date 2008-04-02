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

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-apr-02
 * Time: 21:16:47
 * To change this template use File | Settings | File Templates.
 */
public class FileSystemMonitor {
    private ShareManager manager;
    private ArrayList<FileSystemWatcher> watchers = new ArrayList<FileSystemWatcher>();

    private long lastTimeIncompleteFolderChangedTick = 0;

    public FileSystemMonitor(ShareManager manager) throws IOException {
        this.manager = manager;
        if (OSInfo.isWindows()) extractNativeLibs();
    }

    public void kill() {
        if (!OSInfo.isWindows()) return;
        while(watchers.size() > 0) {
            if(T.t)T.info("Stopping win32 watcher: "+watchers.get(0));
            watchers.get(0).stop();
            watchers.remove(0);
        }
    }

    public void launch() throws IOException {
        if (!OSInfo.isWindows()) return;
        if (watchers.size() > 0) kill();

        addWatcher(manager.getCore().getFileManager().getDownloadStorage().getIncompleteFilesFilePath().toString(), new FileSystemEventListener() {
            public void fileSystemChanged(FileSystemWatcher fileSystemWatcher) {
                lastTimeIncompleteFolderChangedTick = System.currentTimeMillis();
            }
        });

        for(final ShareBase sb : manager.shareBases()) {
            addWatcher(sb.getPath(), new FileSystemEventListener() {
                public void fileSystemChanged(FileSystemWatcher sender) {
                    if (System.currentTimeMillis() - lastTimeIncompleteFolderChangedTick > 100) manager.getShareScanner().signalShareHasChanged();
                }
            });
        }
    }

    private void addWatcher(String path, FileSystemEventListener listener) throws IOException {
        FileSystemWatcher w = new FileSystemWatcher(path,
                NotifyFilters.FILE_NOTIFY_CHANGE_LAST_WRITE |
                        NotifyFilters.FILE_NOTIFY_CHANGE_FILE_NAME |
                        NotifyFilters.FILE_NOTIFY_CHANGE_DIR_NAME,
                true);
        w.addEventListener(listener);
        w.start();
        watchers.add(w);
    }

    private void extractNativeLibs() throws IOException {
        String name = "FileSystemWatcher.dll";
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
