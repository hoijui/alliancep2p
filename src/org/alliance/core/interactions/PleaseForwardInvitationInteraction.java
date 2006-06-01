package org.alliance.core.interactions;

import org.alliance.core.NeedsUserInteraction;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:20:31
 * To change this template use File | Settings | File Templates.
 */
public class PleaseForwardInvitationInteraction implements NeedsUserInteraction {
    private String invitationCode;
    private int toGuid;
    private int fromGuid;

    public PleaseForwardInvitationInteraction(int fromGuid, int toGuid, String invitationCode) {
        this.invitationCode = invitationCode;
        this.toGuid = toGuid;
        this.fromGuid = fromGuid;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public int getToGuid() {
        return toGuid;
    }

    public int getFromGuid() {
        return fromGuid;
    }
}
