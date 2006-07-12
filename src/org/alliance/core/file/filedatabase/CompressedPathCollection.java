package org.alliance.core.file.filedatabase;

import com.stendahls.util.TextUtils;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Can keep track of a lot of filepaths in a efficient (memory usage wise) way.
 * Used in the filedatabase to keep track of what files are indexed. It actually
 * doesn't give a 100% exact result but it is used as a first test to improve performance
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jul-12
 * Time: 14:04:19
 */

public class CompressedPathCollection implements Serializable {
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
