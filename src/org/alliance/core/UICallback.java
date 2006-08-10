package org.alliance.core;

import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 21:08:08
 * To change this template use File | Settings | File Templates.
 */
public interface UICallback {
    void nodeOrSubnodesUpdated(Node node);
    void noRouteToHost(Node node);
    void searchHits(int srcGuid, int hops, List<SearchHit> hits);
    void trace(int level, String message, Exception stackTrace);
    void handleError(Throwable e, Object Source);
    void statusMessage(String s);
    void toFront();
    void signalFriendAdded(Friend friend);
    boolean isUIVisible();
    void logNetworkEvent(String event);
    void receivedShareBaseList(Friend friend, String[] shareBaseNames);
    void receivedDirectoryListing(Friend friend, int shareBaseIndex, String path, String[] files);
}
