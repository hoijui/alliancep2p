package org.alliance.core.settings;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 14:01:19
 */
public class Friend {
    private String nickname, host;
    private Integer guid, port;

    public Friend() {
    }

    public Friend(String nickname, String lasthost, Integer guid, Integer lastport) {
        this.nickname = nickname;
        this.host = lasthost;
        this.guid = guid;
        this.port = lastport;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getGuid() {
        return guid;
    }

    public void setGuid(Integer guid) {
        this.guid = guid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String toString() {
        return "Friend ["+nickname+"]";
    }
}
