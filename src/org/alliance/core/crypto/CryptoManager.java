package org.alliance.core.crypto;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.networklayers.tcpnio.TCPNIONetworkLayer;

import java.io.IOException;
import java.security.KeyStore;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 19:30:23
 * To change this template use File | Settings | File Templates.
 */
public class CryptoManager  {
    private CryptoLayer cryptoLayer;
    private CoreSubsystem core;
    private TCPNIONetworkLayer networkLayer;
    private KeyStore keystore;

    public CryptoManager(CoreSubsystem core) throws Exception {
        this.cryptoLayer = new SSLCryptoLayer(core);
        this.core = core;
    }

    public CryptoLayer getCryptoLayer() {
        return cryptoLayer;
    }

    public void init() throws IOException, Exception {
/*        File file = new File(core.getSettings().getInternal().getKeystorefilename());
        if (!file.exists()) KeyStoreGenerator.generate("alliance", getKeystorePassword(), file.getPath());
        keystore = KeyStore.getInstance("JKS");
        if(T.t)T.info("Loading keystore...");
        keystore.load(new FileInputStream(file), getKeystorePassword());
        if(T.t)T.info("loaded.");*/

        this.networkLayer = core.getNetworkManager().getNetworkLayer();
        cryptoLayer.setNetworkLayer(networkLayer);
        cryptoLayer.init();
    }

    public KeyStore getKeystore() {
        return keystore;
    }

    public char[] getKeystorePassword() {
        return core.getSettings().getMy().getGuid().toString().toCharArray();
    }
}
