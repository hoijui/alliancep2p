package org.alliance.ui.windows.search;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.filedatabase.FileType;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-24
 * Time: 14:20:28
 */
public class FileNode extends SearchTreeNode implements Comparable {
    private SearchTreeNode parent;
    private String filename;
    private double hits;
    private long size;
    private int daysAgo;
    ArrayList<Integer> userGuids = new ArrayList<Integer>();

    private SearchHit sh;

    private String extension;

    public FileNode(SearchTreeNode parent, String filename, SearchHit h, int guid) {
        this.parent = parent;
        this.sh = h;

        filename = filename.replace('_', ' ');
        
        int i = filename.lastIndexOf('.');
        if (i == -1 && FileType.getByFileName(filename) != FileType.EVERYTHING) {
            this.filename = filename;
        } else {
            if (i == -1) {
                this.filename = filename;
            } else {
                this.filename = filename.substring(0, i);
                extension = filename.substring(i+1).toUpperCase();
            }
        }

        size = h.getSize();
        hits =1;
        daysAgo = h.getHashedDaysAgo();
        userGuids.add(guid);
    }

    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    public int getChildCount() {
        return 0;
    }

    public TreeNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        return -1;
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public boolean isLeaf() {
        return true;
    }

    public Enumeration children() {
        return null;
    }

    public void addHit(int guid) {
        hits++;
        userGuids.add(guid);
    }

    public String toString() {
        return filename;
    }

    public String getName() {
        return filename;
    }

    public double getSources() {
        return hits;
    }

    public long getSize() {
        return size;
    }

    public int getDaysAgo() {
        return daysAgo;
    }

    public int compareTo(Object o) {
        FileNode n = (FileNode)o;
        return getName().compareToIgnoreCase(n.getName());
    }

    public double getHits() {
        return hits;
    }

    public SearchHit getSh() {
        return sh;
    }

    private int cachedUsers=-1;
    private String cachedListOfUsers;
    public String getListOfUsers(CoreSubsystem core) {
        if (cachedUsers != userGuids.size()) {
            StringBuffer sb = new StringBuffer();
            for(int i=0;i<userGuids.size();i++) {
                sb.append(core.getFriendManager().nickname(userGuids.get(i)));
                if (i < userGuids.size()-1) sb.append(", ");
            }
            cachedListOfUsers = sb.toString();
        }
        return cachedListOfUsers;
    }

    public ArrayList<Integer> getUserGuids() {
        return userGuids;
    }

    public String getExtension() {
        return extension;
    }
}
