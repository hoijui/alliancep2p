package org.alliance.core.comm;

import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;

import java.util.HashMap;


/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 21:22:29
 * To change this template use File | Settings | File Templates.
 */
public class Router {
    private class Route {
        int friendGuid, hops;

        public Route(int friendGuid, int hops) {
            this.friendGuid = friendGuid;
            this.hops = hops;
        }
    }

    private FriendManager manager;
    private HashMap<Integer, Route[]> routeTable = new HashMap<Integer, Route[]>();

    public Router(FriendManager manager) {
        this.manager = manager;
    }

    public void updateRouteTable(Friend friend, int nodeGuid, int hops) {
        Route routes[] = routeTable.get(nodeGuid);
        if (routes == null) {
            if(T.netTrace)T.trace("Adding new Route to "+nodeGuid+" via "+friend);
            routeTable.put(nodeGuid, new Route[] {new Route(friend.getGuid(), hops)});
        } else {
            if(T.netTrace)T.trace("Updating existing Route array (Route to "+nodeGuid+" via "+friend+")");
            for(Route r : routes) {
                if (r.friendGuid == friend.getGuid()) {
                    if (r.hops > hops) {
                        if(T.netTrace)T.trace("Updating number of hops for existing Route to"+nodeGuid+". Old: "+r.hops+" new: "+hops);
                        r.hops = hops;
                        return;
                    }
                }
            }
            if(T.netTrace)T.trace("Adding new Route");
            Route r[] = new Route[routes.length+1];
            r[0] = new Route(friend.getGuid(), hops);
            for(int i=0;i<routes.length;i++) r[i+1] = routes[i];
        }
    }

    public Friend findClosestFriend(int nodeGuid) {
        if (manager.getMyGUID() == nodeGuid) if(T.t)T.warn("Trying to Route to myself");

        if (manager.getFriend(nodeGuid) != null) {
            Friend f = manager.getFriend(nodeGuid);
            if (!f.isConnected()) return null;
            return f;
        }

        Route routes[] = routeTable.get(nodeGuid);
        if (routes == null) return null;
        int minHops=0, index=-1;
        for(int i=0;i<routes.length;i++) {
            if (i==0 || minHops > routes[i].hops && manager.getFriend(routes[i].friendGuid).isConnected()) {
                index = i;
                minHops = routes[i].hops;
            }
        }
        if (index == -1) return null;
        return manager.getFriend(routes[index].friendGuid);
    }
}
