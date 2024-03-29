package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.filedatabase.FileType;

import java.io.IOException;

/**
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class Search extends RPC {
    private String query;
    private byte fileTypeId;

    public Search() {
    }

    public Search(String query, byte fileTypeId) {
        this();
        this.query = query;
        this.fileTypeId = fileTypeId;
    }

    public void execute(Packet in) throws IOException {
        query = in.readUTF();

        core.logNetworkEvent("Search query "+query+" from "+con.getRemoteFriend());
        
        FileType ft = FileType.EVERYTHING;
        int ftid = in.readByte();
        if (FileType.getFileTypeById(ftid) != null) ft = FileType.getFileTypeById(ftid);

        FileDescriptor fd[] = manager.getCore().getFileManager().search(query, SearchHits.MAX_SEARCH_HITS, ft);
        SearchHitsV2 sh = new SearchHitsV2();
        for(FileDescriptor f : fd) if (f != null) sh.addHit(f);

        if(T.t)T.info("Found "+sh.getNHits()+" hits.");
        if (sh.getNHits() > 0) {
            //reply with Search hits
            send(fromGuid, sh);
        }
    }

    public Packet serializeTo(Packet p) {
        p.writeUTF(query);
        p.writeByte(fileTypeId);
        return p;
    }
}
