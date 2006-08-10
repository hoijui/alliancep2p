package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryListing extends RPC {
    private String files[];
    private int shareBaseIndex;
    private String path;

    public DirectoryListing() {
    }

    public DirectoryListing(int shareBaseIndex, String path, String files[]) {
        this.files = files;
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    public void execute(Packet data) throws IOException {
        int len = data.readInt();
        byte arr[] = new byte[len];
        data.readArray(arr);
        ByteArrayInputStream bais = new ByteArrayInputStream(arr);
        DataInputStream in = new DataInputStream(new InflaterInputStream(bais));

        shareBaseIndex = in.readInt();
        path = in.readUTF();
        int nFiles = in.readInt();
        if(T.t)T.info("Decompressing "+nFiles+" files for share base "+shareBaseIndex+" and path "+path);

        files = new String[nFiles];
        for(int i=0;i<files.length;i++) files[i] = in.readUTF();

        if(T.t)T.info("Found the following files:");
        for(String s : files) {
            if(T.t)T.info("  "+s);
        }

        core.getUICallback().receivedDirectoryListing(con.getRemoteFriend(), shareBaseIndex, path, files);
    }

    public Packet serializeTo(Packet p) {
        if(T.t)T.info("compressing directory listing and sending..");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(buf, new Deflater(9)));

        try {
            out.writeInt(shareBaseIndex);
            out.writeUTF(path);

            out.writeInt(files.length);
            for(String s : files) out.writeUTF(s);
            out.flush();
            out.close();
        } catch(IOException e) {
            if(T.t)T.error("Sheet! Could not compress directory listing!");
        }

        byte arr[] = buf.toByteArray();
        if(T.t)T.info("Compressed packet size: "+arr.length);
        p.writeInt(arr.length);
        p.writeArray(arr);
        return p;
    }
}
