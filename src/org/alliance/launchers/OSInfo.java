package org.alliance.launchers;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jun-07
 * Time: 10:35:53
 */
public class OSInfo {
    public static boolean supportsTrayIcon() {
        if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) return true;
        if (System.getProperty("os.name").toUpperCase().indexOf("LINUX") != -1) return true;
        return false;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }
}