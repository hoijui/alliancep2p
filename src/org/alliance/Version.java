package org.alliance;

import org.alliance.core.ResourceSingelton;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:05:08
 * To change this template use File | Settings | File Templates.
 */
public class Version {
    public static final String VERSION="1.0.6";
    public static final int BUILD_NUMBER;
    public static final int PROTOCOL_VERSION = 3;

    static {
        int n = 0;
        try {
            Properties p = new Properties();
            p.load(ResourceSingelton.getRl().getResourceStream("build.properties"));
            n = Integer.parseInt(p.get("build.number").toString());
        } catch (Exception e) {
            System.err.println("Could not load buildnumber: "+e);
        } finally {
            BUILD_NUMBER = n;
        }
    }
}