package org.alliance.core.file.filedatabase;

import com.stendahls.nif.util.WeakValueHashMap;
import com.stendahls.util.TextUtils;
import org.alliance.core.file.ChunkStorage;
import org.alliance.core.file.filedatabase.searchindex.KeywordIndex;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;
import org.alliance.core.file.share.T;

import java.io.*;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:49:43
 * To change this template use File | Settings | File Templates.
 */
public class FileDatabase {
    public static final int MINIMUM_TIME_BETWEEN_FLUSHES_IN_MS = 1000*60;
    public static final int VERSION=12;

    private ChunkStorage chunkStorage;

    private FileDescriptorAllocationTable allocationTable = new FileDescriptorAllocationTable();
    private KeywordIndex keywordIndex;
    private HashMap<Hash, Integer> baseHashTable = new HashMap<Hash, Integer>();
    private HashMap<String, Hash> duplicates = new HashMap<String, Hash>();
    private CompressedPathCollection indexedFilenames = new CompressedPathCollection();

    private WeakValueHashMap<Hash, FileDescriptor> fileDescriptorCache = new WeakValueHashMap<Hash, FileDescriptor>();

    private String indexFilePath;
    private long totalSize;
    private long lastFlushedAt;

    public FileDatabase(String indexFilePath, String databaseFilePath) throws IOException {
        this.indexFilePath = indexFilePath;

        keywordIndex = new KeywordIndex();
        chunkStorage = new ChunkStorage(databaseFilePath);

        try {
            loadIndices();
        } catch(Exception e) {
            if(T.t)T.error("Could not load indices: "+e);
            e.printStackTrace();
        }
    }

    public synchronized boolean isEmpty() throws IOException {
        return getNumberOfFiles() == 0;
    }

    /**
     *
     * @param fd
     * @return null if everything is ok. If the hash root is aldready in database the older FD is returned (the one in database, not the one sent to this method)
     * @throws IOException
     */
    public synchronized FileDescriptor add(FileDescriptor fd) throws IOException {
        if (contains(fd.getRootHash())) {
            if(T.t)T.info("Maybe found duplicate: "+fd);
            FileDescriptor old = getFd(fd.getRootHash());
            if (old != null && old.existsAndSeemsEqual()) {
                if (fd.getCanonicalPath().toLowerCase().indexOf("sample") != -1) {
                    remove(old);
                    addDuplicate(old.getCanonicalPath(), old.getRootHash());
                    //continue adding fd
                } else {
                    addDuplicate(fd.getCanonicalPath(), fd.getRootHash());
                    return old;
                }
            } else {
                if(T.t)T.info("Nope. Original did not exist.");
                if (old != null) remove(old);
            }
        }
        if(T.t)T.info("Adding new fd to db: "+fd);

        //save to file descriptor
        ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        fd.serializeTo(out);
        out.flush();
        out.close();
        int off = chunkStorage.appendChunk(out.toByteArray());

        //add to allocation table
        int index = allocationTable.addOffset(off);

        //add to baseHashTable
        baseHashTable.put(fd.getRootHash(), index);

        //index keywords
        keywordIndex.add(index, fd);

        indexedFilenames.addPath(fd.getFullPath());

        totalSize += fd.getSize();

        //save and flush database and indices
        if (System.currentTimeMillis() - lastFlushedAt > MINIMUM_TIME_BETWEEN_FLUSHES_IN_MS) flush();

        return null; //null means everything went ok.
    }

    private void addDuplicate(String fullPath, Hash rootHash) {
        if(T.t)T.info("Adding duplicate: "+fullPath);
        duplicates.put(fullPath, rootHash);
    }

    public synchronized FileDescriptor getFd(int index) throws IOException {
        int offset = allocationTable.getOffset(index);
        FileDescriptor fd = null;
        try {
            InputStream is = chunkStorage.getChunk(offset);
            if (is == null) {
                //chunk has been removed from storage
                return null;
            }
            fd = FileDescriptor.createFrom(is, true);
        } catch(FileHasBeenRemovedOrChanged fileHasBeenRemoved) {
            remove(fileHasBeenRemoved.getFd());
            return null;
        }
        fileDescriptorCache.put(fd.getRootHash(), fd);
        return fd;
    }

    private synchronized void remove(FileDescriptor fd) throws IOException {
        if(T.t)T.info("Remoiving: "+fd);
        if (baseHashTable == null) throw new IOException("Internal error 0 in filedatabase!");
        Integer index = baseHashTable.get(fd.getRootHash());
        if (index == null) throw new IOException("Internal error in remove in filedatabase!");
        int off = allocationTable.getOffset(index);
        keywordIndex.remove(index);
        chunkStorage.markAsRemoved(off);
        baseHashTable.remove(fd.getRootHash());
        fileDescriptorCache.remove(fd.getRootHash());
        indexedFilenames.removePath(fd.getFullPath());
        totalSize -= fd.getSize();
    }

    public synchronized FileDescriptor getFd(Hash hash) throws IOException {
        if (contains(hash)) {
            FileDescriptor fd = fileDescriptorCache.get(hash);
            if (fd != null) return fd;
        }
        Integer i = baseHashTable.get(hash);
        if (i == null) return null;
        return getFd(i);
    }

    public synchronized int getNumberOfFiles() {
        return allocationTable.getNumberOfFiles();
    }

    public synchronized void printToSout() throws IOException {
        System.out.println(TextUtils.formatByteSize(totalSize)+" in "+TextUtils.formatNumber(getNumberOfFiles())+" files.");
    }

    public synchronized void flush() throws IOException {
        chunkStorage.flush();
        saveIndices();
        lastFlushedAt = System.currentTimeMillis();
    }

    private void saveIndices() throws IOException {
        String fn = indexFilePath;
        File file = new File(fn);
        File bak = new File(fn+".bak");
        bak.delete();
        file.renameTo(bak);
        file = new File(fn);

        FileOutputStream fout = new FileOutputStream(file);
        ObjectOutputStream out = new ObjectOutputStream(new DeflaterOutputStream(fout, new Deflater(9)));

        out.writeInt(VERSION);
        out.writeLong(totalSize);
        out.writeObject(baseHashTable);
        allocationTable.save(out);
        keywordIndex.save(out);
        out.writeObject(indexedFilenames);
        out.writeObject(duplicates);

        out.flush();
        out.close();
    }

    private void loadIndices() throws IOException {
        loadIndices(indexFilePath);
    }

    private void loadIndices(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            if(T.t)T.warn("Assuming we're starting for the first time with no index.");
            return;
        }

        try {
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(new FileInputStream(file)));
            try {
                if (in.readInt() != VERSION) {
                    if(T.t)T.warn("Incorrect database version! Starting from scratch!");
                    return;
                }
                totalSize = in.readLong();
                baseHashTable = (HashMap<Hash, Integer>)in.readObject();
                allocationTable.load(in);
                keywordIndex.load(in);
                indexedFilenames = (CompressedPathCollection)in.readObject();
                duplicates = (HashMap<String, Hash>)in.readObject();
            } catch(ClassNotFoundException e) {
                throw new IOException("Could not load indices: "+e);
            }
            in.close();
        } catch(IOException e) {
            if (path.endsWith(".bak")) {
                throw e;
            } else {
                if(T.t)T.error("Error when loading index. Trying with backup index. "+e);
                loadIndices(indexFilePath+".bak");
            }
        }
    }

    public synchronized void cleanupDuplicates() {
        for(Iterator<String> i = duplicates.keySet().iterator();i.hasNext();) {
            if (!new File(i.next()).exists()) i.remove();
        }
    }

    public synchronized boolean contains(String path) throws IOException {
        return indexedFilenames.contains(path);
    }

    public synchronized boolean contains(Hash rootHash) {
        return baseHashTable.containsKey(rootHash);
    }

    public KeywordIndex getKeywordIndex() {
        return keywordIndex;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public synchronized Set<Hash> getAllHashes() {
        return baseHashTable.keySet();
    }

    /**
     * Returns up to 500 FDs that are withing the supplied path. Subdirectories included.
     * @param path
     * @return
     * @throws IOException
     */

    public Collection<FileDescriptor> getFDsByPath(String path) throws IOException {
        FileDescriptor fd[] = search(path, 500, FileType.EVERYTHING);
        ArrayList<FileDescriptor> al = new ArrayList<FileDescriptor>();
        for(FileDescriptor f : fd) {
            if (f != null) {
                if(T.t)T.trace("found: "+f.getFullPath()+" - "+path);
                if (f.getFullPath().startsWith(path)) al.add(f);
            }
        }
        return al;
    }

    public synchronized FileDescriptor[] search(String query, int maxHits, FileType ft) throws IOException {
        int indices[] = keywordIndex.search(query, maxHits, ft);
        FileDescriptor fd[] = new FileDescriptor[indices.length];
        for(int i=0;i<indices.length;i++) {
            fd[i] = getFd(indices[i]);
        }
        return fd;
    }

    public boolean isDuplicate(String fullPath) throws IOException {
        if (duplicates.containsKey(fullPath)) {
            FileDescriptor fd = getFd(duplicates.get(fullPath));
            if (fd != null) {
                if (fd.existsAndSeemsEqual()) {
                    return true;
                } else {
                    remove(fd);
                }
            }
            duplicates.remove(fullPath);
        }
        return false;
    }

    public String[] getDirectoryListing(ShareBase base, String path) {
        return indexedFilenames.getDirectoryListing(base.getPath()+"/"+path);
    }

    public Collection<String> getDuplicates() {
        return duplicates.keySet();
    }

    public Hash getHashForDuplicate(String path) {
        return duplicates.get(path);
    }

    public void clearDuplicates() {
        if(T.t)T.info("Removing all duplicate entries.");
        duplicates.clear();
    }
}
