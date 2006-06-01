package org.alliance.core.settings;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 15:02:20
 */
public class My extends SettingClass {
    public static final String UNDEFINED_NICKNAME = "undefined";

    private Integer guid = new Random().nextInt();
    private String nickname = UNDEFINED_NICKNAME;

    public My() {
    }

    public My(Integer guid, String nickname) {
        this.guid = guid;
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getGuid() {
        return guid;
    }

    public void setGuid(Integer guid) {
        this.guid = guid;
    }

    public boolean hasUndefinedNickname() {
        return UNDEFINED_NICKNAME.equals(nickname);
    }
}
