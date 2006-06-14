package org.alliance.core.file.share;

import com.stendahls.util.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:41:15
 * To change this template use File | Settings | File Templates.
 */
public class ShareBase {
    private String path;

    public ShareBase(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        try {
            path = new File(path).getCanonicalFile().getPath();
        } catch (IOException e) {
            if(T.t)T.error("Could not resolve canonical share path: "+e);
        }
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return "ShareBase "+path;
    }
}
