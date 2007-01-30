package org.alliance.launchers;

import org.alliance.Version;
import org.alliance.core.ResourceSingelton;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * User: maciek
 * Date: 2004-sep-14
 * Time: 10:49:44
 */
public class SplashWindowOld extends Window implements Runnable {
    private Image image;
    private String statusMessage="";
    private long startTick;
    private int progressPercent=-1;

    private int lastWidth, lastHeight;
    private Image backBuffer;
    private Graphics2D backBufferGraphics;

    public SplashWindowOld() throws Exception {
        super(new Frame());
        setStatusMessage("Launching...");
        image = Toolkit.getDefaultToolkit().getImage(ResourceSingelton.getRl().getResource("gfx/splash.jpg"));
        MediaTracker mt = new MediaTracker(SplashWindowOld.this);
        mt.addImage(image,0);
        try { mt.waitForAll(); } catch(InterruptedException e) {}
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();

        setLocation(ss.width/2-image.getWidth(null)/2,
                ss.height/2-image.getHeight(null)/2);
        setSize(new Dimension(image.getWidth(null),image.getHeight(null)));

        init();

        startTick = System.currentTimeMillis();
        setVisible(true);
        toFront();
        requestFocus();
    }

    private int progressBarLength = 100, progressBarHeight=8;
    public void paint(Graphics frontG) {
        init();
        Graphics2D g = backBufferGraphics;

        g.drawImage(image,0,0,null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Arial Black, Arial", 0, 10));

        g.setColor(Color.white);
        int texty = image.getHeight(null)-10;
        g.drawString(statusMessage, 10, texty);
        String s = "Version "+ Version.VERSION+" "+Version.BUILD_NUMBER;
        g.drawString(s, image.getWidth(null)-10-g.getFontMetrics().stringWidth(s), texty);

        if (progressPercent >= 0) {
            int a = progressPercent*3/2;
            if (a > 70) a = 70;
            g.setColor(new Color(255,255,255,a));
            progressBarLength = image.getWidth(null)-15*4;
            progressBarHeight = 20;
            int x = image.getWidth(null)/2-progressBarLength/2;
            int y = image.getHeight(null)-51;
            g.drawRect(x,y, progressBarLength, progressBarHeight);
            g.fillRect(x+2, y+2, (progressBarLength-3)*progressPercent/100, progressBarHeight-3);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        frontG.drawImage(backBuffer, 0, 0, null);
    }

    private void init() {
        if (backBuffer == null || lastWidth != getWidth() || lastHeight != getHeight()) {
            lastHeight = getHeight();
            lastWidth = getWidth();
            backBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            backBufferGraphics = (Graphics2D)backBuffer.getGraphics();
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        repaint();
    }

    public void setProgressPercent(int i) {
        progressPercent = i;
    }

    public void run() {
        setVisible(false);
        dispose();
    }
}
