package org.alliance.core.settings;

import static org.alliance.core.CoreSubsystem.KB;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 17:40:27
 */
public class Internal extends SettingClass {
    private static final String CURRENT_DIRECTORY;
    static {
        String s = new File(".").getAbsoluteFile().toString();
        if (s.endsWith(".")) s = s.substring(0, s.length()-1);
        CURRENT_DIRECTORY = s;
    }

    private Integer tempcleareddups=0;

    private Integer reconnectinterval = 60*10;
    private Integer connectFriendInterval = 1;
    private Integer sharemanagercycle = 60;
    private String filedatabasefile = "share.dat";
    private String filedatabaseindexfile = "share.idx";
    private String downloadquefile = "downloads.dat";
    private String corestatefile = "core.dat";
    private String downloadfolder = CURRENT_DIRECTORY+"downloads";
    private String cachefolder = CURRENT_DIRECTORY+"cache";
    private Integer maxdownloadconnections=5;
    private Integer recordoutspeed=0, recordinspeed=0;
    private Integer connectionkeepaliveinterval=60;
    private Integer numberofblockstopipeline=2;

    private Integer alwaysallowfriendstoconnect=0;

    private Integer minimumtimebetweeninvitations = 60*24*2; //in minutes

    /** Used to be polite when running on XP sp2 wich only allows 10 pending tcp/ip connections
     * before ståpping the network stack. Set to 8 to be on the safe side. */
    private Integer maxpendingconnections = 8;

    private Integer hashspeedinmbpersecond=10;

    private Integer sosendbuf=-1, soreceivebuf=-1;
    private Integer discwritebuffer=256*KB, //one instance of this one per download
            socketsendbuffer=256*KB; //one instance per download
    private Integer socketreadbuffer=256*KB; //only one instance of this one - at the network layer
    private Integer maximumAlliancePacketSize=32*KB;

    private Integer politehashingwaittimeinminutes=30;
    private Integer politehashingintervalingigabytes=50;

    private Integer maxfileexpandinblocks=50; //don't exceed file system size of file we're downloading to by more than this number
    //This is here because if we download the last block of a 4Gb file we seek to 4Gb into
    //an empry file. This makes XP grind to a halt. 100 means expand 100mb per block at most

    private Integer uploadthrottle=0; //zero to disable

    public Internal() {
    }

    public Internal(Integer reconnectinterval) {
        this.reconnectinterval = reconnectinterval;
    }

    public Integer getReconnectinterval() {
        return reconnectinterval;
    }

    public void setReconnectinterval(Integer reconnectinterval) {
        this.reconnectinterval = reconnectinterval;
    }

    public Integer getSharemanagercycle() {
        return sharemanagercycle;
    }

    public void setSharemanagercycle(Integer sharemanagercycle) {
        this.sharemanagercycle = sharemanagercycle;
    }

    public String getFiledatabasefile() {
        return filedatabasefile;
    }

    public void setFiledatabasefile(String filedatabasefile) {
        this.filedatabasefile = filedatabasefile;
    }

    public String getFiledatabaseindexfile() {
        return filedatabaseindexfile;
    }

    public void setFiledatabaseindexfile(String filedatabaseindexfile) {
        this.filedatabaseindexfile = filedatabaseindexfile;
    }

    public String getDownloadfolder() {
        return downloadfolder;
    }

    public void setDownloadfolder(String downloadfolder) {
        this.downloadfolder = downloadfolder;
    }

    public String getCachefolder() {
        return cachefolder;
    }

    public void setCachefolder(String cachefolder) {
        this.cachefolder = cachefolder;
    }

    public Integer getMaxdownloadconnections() {
        return maxdownloadconnections;
    }

    public void setMaxdownloadconnections(Integer maxdownloadconnections) {
        this.maxdownloadconnections = maxdownloadconnections;
    }

    public Integer getRecordoutspeed() {
        return recordoutspeed;
    }

    public void setRecordoutspeed(Integer recordoutspeed) {
        this.recordoutspeed = recordoutspeed;
    }

    public Integer getRecordinspeed() {
        return recordinspeed;
    }

    public void setRecordinspeed(Integer recordinspeed) {
        this.recordinspeed = recordinspeed;
    }

    public Integer getConnectionkeepaliveinterval() {
        return connectionkeepaliveinterval;
    }

    public void setConnectionkeepaliveinterval(Integer connectionkeepaliveinterval) {
        this.connectionkeepaliveinterval = connectionkeepaliveinterval;
    }

    public Integer getSosendbuf() {
        return sosendbuf;
    }

    public void setSosendbuf(Integer sosendbuf) {
        this.sosendbuf = sosendbuf;
    }

    public Integer getSoreceivebuf() {
        return soreceivebuf;
    }

    public void setSoreceivebuf(Integer soreceivebuf) {
        this.soreceivebuf = soreceivebuf;
    }

    public Integer getNumberofblockstopipeline() {
        return numberofblockstopipeline;
    }

    public void setNumberofblockstopipeline(Integer numberofblockstopipeline) {
        this.numberofblockstopipeline = numberofblockstopipeline;
    }

    public Integer getDiscwritebuffer() {
        return discwritebuffer;
    }

    public void setDiscwritebuffer(Integer discwritebuffer) {
        this.discwritebuffer = discwritebuffer;
    }

    public Integer getSocketsendbuffer() {
        return socketsendbuffer;
    }

    public void setSocketsendbuffer(Integer socketsendbuffer) {
        this.socketsendbuffer = socketsendbuffer;
    }

    public Integer getSocketreadbuffer() {
        return socketreadbuffer;
    }

    public void setSocketreadbuffer(Integer socketreadbuffer) {
        this.socketreadbuffer = socketreadbuffer;
    }

    public Integer getMaximumAlliancePacketSize() {
        return maximumAlliancePacketSize;
    }

    public void setMaximumAlliancePacketSize(Integer maximumAlliancePacketSize) {
        this.maximumAlliancePacketSize = maximumAlliancePacketSize;
    }

    public String getDownloadquefile() {
        return downloadquefile;
    }

    public void setDownloadquefile(String downloadquefile) {
        this.downloadquefile = downloadquefile;
    }

    public Integer getPolitehashingwaittimeinminutes() {
        return politehashingwaittimeinminutes;
    }

    public void setPolitehashingwaittimeinminutes(Integer politehashingwaittimeinminutes) {
        this.politehashingwaittimeinminutes = politehashingwaittimeinminutes;
    }

    public Integer getPolitehashingintervalingigabytes() {
        return politehashingintervalingigabytes;
    }

    public void setPolitehashingintervalingigabytes(Integer politehashingintervalingigabytes) {
        this.politehashingintervalingigabytes = politehashingintervalingigabytes;
    }

    //don't exceed file system size of file we're downloading to by more than this number
    //This is here because if we downloads the last block of a 4Gb file we seek to 4Gb into
    //an empry file. This makes XP grind to a halt. 100 means expan 100mb per block at most
    public Integer getMaxfileexpandinblocks() {
        return maxfileexpandinblocks;
    }

    public void setMaxfileexpandinblocks(Integer maxfileexpandinblocks) {
        this.maxfileexpandinblocks = maxfileexpandinblocks;
    }

    public Integer getUploadthrottle() {
        return uploadthrottle;
    }

    public void setUploadthrottle(Integer uploadthrottle) {
        this.uploadthrottle = uploadthrottle;
    }

    public Integer getHashspeedinmbpersecond() {
        return hashspeedinmbpersecond;
    }

    public void setHashspeedinmbpersecond(Integer hashspeedinmbpersecond) {
        this.hashspeedinmbpersecond = hashspeedinmbpersecond;
    }

    public Integer getConnectFriendInterval() {
        return connectFriendInterval;
    }

    public void setConnectFriendInterval(Integer connectFriendInterval) {
        this.connectFriendInterval = connectFriendInterval;
    }

    public String getCorestatefile() {
        return corestatefile;
    }

    public void setCorestatefile(String corestatefile) {
        this.corestatefile = corestatefile;
    }

    public Integer getMaxpendingconnections() {
        return maxpendingconnections;
    }

    public void setMaxpendingconnections(Integer maxpendingconnections) {
        this.maxpendingconnections = maxpendingconnections;
    }

    public Integer getMinimumtimebetweeninvitations() {
        return minimumtimebetweeninvitations;
    }

    public void setMinimumtimebetweeninvitations(Integer minimumtimebetweeninvitations) {
        this.minimumtimebetweeninvitations = minimumtimebetweeninvitations;
    }

    public Integer getTempcleareddups() {
        return tempcleareddups;
    }

    public void setTempcleareddups(Integer tempcleareddups) {
        this.tempcleareddups = tempcleareddups;
    }

    public Integer getAlwaysallowfriendstoconnect() {
        return alwaysallowfriendstoconnect;
    }

    public void setAlwaysallowfriendstoconnect(Integer alwaysallowfriendstoconnect) {
        this.alwaysallowfriendstoconnect = alwaysallowfriendstoconnect;
    }
}
