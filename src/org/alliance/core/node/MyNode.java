package org.alliance.core.node;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.T;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-03
 * Time: 14:38:28
 * To change this template use File | Settings | File Templates.
 */
public class MyNode extends Node {
    private static final String WHATSMYIPURL = "http://www.whatsmyip.org";
    private static final String IPPATTERN = "Whats My IP Address? Your IP is ";

    private String externalIp;

    public MyNode(String nickname, int guid) {
        super(nickname, guid);
    }

    public boolean isConnected() {
        return true;
    }

    public String getExternalIp(CoreSubsystem core) throws IOException {
        autodetectExternalIp(core);
        return getExternalIp();
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }

    private boolean alreadyTriedAutodetect = false;
    public void autodetectExternalIp(CoreSubsystem core) throws IOException {
        if (externalIp == null && !alreadyTriedAutodetect) {
            //superhack. Hope the guy at whatsmyip.org doesn't mind. This will only happen when a new, separate, cloud is created.
            try {
                URLConnection c = new URL(WHATSMYIPURL).openConnection();
                InputStream in = c.getInputStream();
                StringBuffer result = new StringBuffer();
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = r.readLine()) != null) result.append(line);

                line = result.toString();
                int i = line.indexOf(IPPATTERN);
                if (i == -1) throw new Exception("Could not detect your external IP number. Can't find pattern in page.");

                externalIp = line.substring(i+IPPATTERN.length(), line.indexOf('<', i));

                if(T.t)T.info("Detected external ip: "+externalIp);
            } catch(Exception e) {
                throw new IOException("Could not detect your external IP: "+e);
            } finally{
                alreadyTriedAutodetect = true;
            }
        }
    }
}
