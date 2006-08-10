package org.alliance.core.file.filedatabase;

import com.stendahls.util.TextUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Can keep track of a lot of filepaths in a efficient (memory usage wise) way.
 * Used in the filedatabase to keep track of what files are indexed.
 *
 * Uhm. It's not efficient memory wise right now. THere's room for optimiziation here.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jul-12
 * Time: 14:04:19
 */

public class CompressedPathCollection implements Serializable {
    private static final long serialVersionUID = 7234254693355857212L;
    private HashSet<String> paths = new HashSet<String>();

    public void addPath(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        paths.add(path);
    }

    public void removePath(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        paths.remove(path);
    }

    /**
     * @param path
     * @return true if there is a possibility that this path is contained in this collections
     */
    public boolean contains(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        return paths.contains(path);
    }

    public String[] getDirectoryListing(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        if (!path.endsWith("/")) path = path+'/';

        HashSet<String> hs = new HashSet<String>();
        for(String s: paths) {
            if (s.startsWith(path)) {
                //add filename without path
                s = s.substring(path.length());
                if (s.indexOf('/') != -1) {
                    //show only files and folders that are in this directory, not in subdirectories
                    s = s.substring(0, s.indexOf('/')+1);
                }
                hs.add(s);
            }
        }
        String[] sa = new String[hs.size()];
        hs.toArray(sa);
        Arrays.sort(sa);
        return sa;
    }
}

//public class CompressedPathCollection implements Serializable {
//    private HashSet<Integer> paths = new HashSet<Integer>();
//
//    public void addPath(String path) {
//        path = TextUtils.makeSurePathIsMultiplatform(path);
//        paths.add(path.hashCode());
//    }
//
//    public void removePath(String path) {
//        //this is a bit stupid. Since this "collection" is serialized to disc it between restarts it can get quite big..
//    }
//
//    /**
//     * @param path
//     * @return true if there is a possibility that this path is contained in this collections
//     */
//    public boolean contains(String path) {
//        path = TextUtils.makeSurePathIsMultiplatform(path);
//        return paths.contains(path.hashCode());
//    }
//}
