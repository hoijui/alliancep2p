package org.alliance.core.file.share;

import com.stendahls.util.TextUtils;

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
        this.path = TextUtils.makeSurePathIsMultiplatform(path);
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return "ShareBase "+path;
    }
}
