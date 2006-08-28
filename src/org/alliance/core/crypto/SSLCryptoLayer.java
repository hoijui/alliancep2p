package org.alliance.core.crypto;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 *
 *
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 14:22:13
 */
public class SSLCryptoLayer extends BufferedCryptoLayer {
    private SSLContext sslContext;
    //@todo: memory leak - this array is never cleared
    private HashMap<Object, SSLEngine> engines = new HashMap<Object, SSLEngine>();

    private HashMap<Object, ByteBuffer> bufferedIncomingEncryptedData = new HashMap<Object, ByteBuffer>();

//    can be used with "strong encryption" - with no US export regulations
    private static final String CHIPHER_SUITE = "TLS_DH_anon_WITH_AES_128_CBC_SHA";

//    can be used with "unlimited strength" encryption - subject to US export laws
//    private static final String CHIPHER_SUITE = "TLS_DH_anon_WITH_AES_256_CBC_SHA";

    public SSLCryptoLayer(CoreSubsystem core) throws Exception {
        super(core);
    }

    public void init() throws Exception {
        if(T.t)T.info("SSLCryptoLayer initializing!");
        //init sslengine

        sslContext = SSLContext.getInstance("TLSv1");

/*        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(core.getCryptoManager().getKeystore(), core.getCryptoManager().getKeystorePassword());
        sslContext.init(kmf.getKeyManagers(), null, null);*/

        sslContext.init(null, null, null);
    }

    public int encrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        if(T.t)T.debug("Encrypt");
        SSLEngine e = getSSLEngineFor(c);

        SSLEngineResult r;
        int read = 0;
        do {
            if(T.t)T.trace("Wrapping "+src.remaining()+" bytes");
            r = e.wrap(src, dst);
            read += r.bytesConsumed();
            if(T.t)T.trace("SSLResult: "+r.getStatus()+" "+r.getHandshakeStatus()+" produced: "+r.bytesProduced()+" consumed: "+r.bytesConsumed());
            if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                Runnable task;
                while((task = e.getDelegatedTask()) != null) {
                    if(T.t)T.trace("Executing delegated task: "+task);
                    task.run();
                    if(T.t)T.trace("Task complete.");
                }
            }
        } while(r.getStatus() == SSLEngineResult.Status.OK &&
                ((r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && src.remaining() > 0) ||
                r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP));

        if (r.getStatus() != SSLEngineResult.Status.OK) throw new IOException("Status not ok: "+r.getStatus()+"!");
        if (r.bytesProduced() > 0) {
            if(T.t)T.trace("SSLEngine produced data - add interest to sent it.");
            addSendInterest(c);
        }
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            if(T.t)T.debug("Hmm. SSLEngine needs wrap. This is recursive and a bit tricky.");
            encrypt(c, ByteBuffer.allocate(0), dst);
        }

        checkSSLEngineResult(e, c, r);

        return read;
    }

    public void decrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        if(T.t)T.debug("Decrypt");

        if (bufferedIncomingEncryptedData.get(c.getKey()) != null) {
            if(T.t)T.trace("Old, buffered, encryption data available - merge it with new data");
            ByteBuffer old = bufferedIncomingEncryptedData.get(c.getKey());
            ByteBuffer n3w = src;
            src = ByteBuffer.allocate(old.remaining()+n3w.remaining());
            src.put(old);
            src.put(n3w);
            src.flip();
            bufferedIncomingEncryptedData.remove(c.getKey());
        }

        SSLEngine e = getSSLEngineFor(c);
        SSLEngineResult r;
        do {
            r = e.unwrap(src, dst);
            if(T.t)T.trace("SSLResult: "+r.getStatus()+" "+r.getHandshakeStatus()+" produced: "+r.bytesProduced()+" consumed: "+r.bytesConsumed());
            if(T.t)T.trace("Left in src buffer: "+src.remaining());

            if (r.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                if(T.t)T.trace("SLLEngine reported BUFFER_UNDERFLOW - buffer the data and wait for more");
                ByteBuffer b = bufferedIncomingEncryptedData.get(c.getKey());
                if (b != null) {
                    if(T.t)T.trace("Appending new buffer with old one");
                    ByteBuffer old = b;
                    b = ByteBuffer.allocate(src.remaining()+b.position());
                    b.put(old);
                    b.put(src);
                } else {
                    if(T.t)T.trace("Creating new buffer");
                    b = ByteBuffer.allocate(src.remaining());
                    b.put(src);
                }
                b.flip();
                bufferedIncomingEncryptedData.put(c.getKey(), b);
                //nothing more to do. Wait for more data and then try to call unwrap again.
                return;
            }

            if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                Runnable task;
                while((task = e.getDelegatedTask()) != null) {
                    if(T.t)T.trace("Executing delegated task: "+task);
                    task.run();
                    if(T.t)T.trace("Task complete.");
                }
                if(T.t)T.trace("Doing another unwrap after task delegation");
                r = e.unwrap(src, dst);
                if(T.t)T.trace("SSLResult: "+r.getStatus()+" "+r.getHandshakeStatus()+" produced: "+r.bytesProduced()+" consumed: "+r.bytesConsumed());
            }
        } while (r.getStatus() == SSLEngineResult.Status.OK &&
                ((r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && r.bytesProduced() == 0) || //wants to unwrap more data
                ( r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && src.remaining() > 0 && r.bytesConsumed() > 0))); //has more data to decrypt

        checkSSLEngineResult(e, c, r);

        if (r.getStatus() != SSLEngineResult.Status.OK) throw new IOException("Status not ok: "+r.getStatus()+"!");
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            if(T.t)T.debug("Hmm. SSLEngine needs wrap in decrypt. This is a bit tricky.");
            send(c, ByteBuffer.allocate(0)); //bit shady but should be a safe way to call wrap in a controlled manner
        }
    }

    private void checkSSLEngineResult(SSLEngine e, final Connection c, SSLEngineResult r) throws IOException {
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            throw new IOException("Task should not be needed here.");
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
            if(T.t)T.trace("SSLEngine needs upwrap.");
            if (getConnectionDataFor(c).encryptionBuffer.position() == 0)  {
                if(T.t)T.trace("There is nothing in the encyptionBuffer - We're no longer interested in sending data. Wait for data to arrive");
                removeSendInterest(c);
            } else {
                if(T.t)T.trace("Still stuff in encryption buffer - even tho SSLEngine neews unwrap we're still interested in sending.");
            }
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
            if(T.t)T.trace("Handshaking finished!");
            addSendInterest(c);
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            if(T.t)T.trace("Not handshaking. We're all good.");
        }
    }

    private SSLEngine getSSLEngineFor(Connection c) throws SSLException {
        SSLEngine e = engines.get(c.getKey());
        if (e == null) {
            if(T.t)T.info("Creating new SSLEngine");
            e = setupSLLEngine(c);
            engines.put(c.getKey(), e);
        }
        return e;
    }

    private SSLEngine setupSLLEngine(Connection c) throws SSLException {
        SSLEngine e = sslContext.createSSLEngine();

        e.setEnabledCipherSuites(new String[] {CHIPHER_SUITE});
        e.setUseClientMode(c.getDirection() == Connection.Direction.OUT);
        e.beginHandshake();
        return e;
    }
}
