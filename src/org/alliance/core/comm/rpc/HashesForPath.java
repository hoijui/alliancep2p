package org.alliance.core.comm.rpc;

import org.alliance.core.comm.T;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class HashesForPath extends CompressedRPC {
    private String path;
    private Hash[] hashes;
    private String[] paths;

    public HashesForPath() {
    }

    public HashesForPath(String path, Collection<FileDescriptor> c) {
        this.path = path;
        hashes = new Hash[c.size()];
        paths = new String[c.size()];
        int i = 0;
        for(FileDescriptor fd : c) {
            hashes[i] = fd.getRootHash();
            paths[i] = fd.getSubpath();
            i++;
        }
    }

    public void serializeCompressed(DataOutputStream out) throws IOException {
        out.writeUTF(path);
        out.writeInt(hashes.length);
        for(int i=0;i<hashes.length;i++) {
            out.write(hashes[i].array());
            out.writeUTF(paths[i]);
        }
    }

    public void executeCompressed(DataInputStream in) throws IOException {
        String path = in.readUTF();
        int len = in.readInt();
        hashes = new Hash[len];
        paths = new String[len];
        for(int i=0;i<hashes.length;i++) {
            hashes[i] = new Hash();
            in.readFully(hashes[i].array());
            paths[i] = in.readUTF();
            if(T.t)T.trace("Read path: "+paths[i]);
        }
        if(T.t)T.info("Loaded "+hashes.length+" hashes for path "+path);

        //@todo: this is soo messed up - client starts downloading without any state

        ArrayList<Integer> guid = new ArrayList<Integer>();
        guid.add(con.getRemoteUserGUID());
        for(int i=0;i<hashes.length;i++) {
            if (core.getFileManager().containsComplete(hashes[i])) {
                core.getUICallback().statusMessage("You already have the file "+paths[i]+"!");
            } else if (core.getNetworkManager().getDownloadManager().getDownload(hashes[i]) != null) {
                core.getUICallback().statusMessage("You are already downloading "+paths[i]+"!");
            } else {
                core.getNetworkManager().getDownloadManager().queDownload(hashes[i], paths[i], guid);
            }
        }
    }
}
