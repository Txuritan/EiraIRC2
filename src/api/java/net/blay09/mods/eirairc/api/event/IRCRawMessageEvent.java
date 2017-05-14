// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api.event;

import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.api.irc.IRCMessage;

/**
 * Base class for events based on a raw IRC message.
 */
public abstract class IRCRawMessageEvent extends IRCEvent {

    /**
     * the raw message
     */
    public final IRCMessage rawMessage;

    /**
     * INTERNAL EVENT. YOU SHOULD NOT POST THIS YOURSELF.
     *
     * @param connection the connection this event is based on
     */
    public IRCRawMessageEvent(IRCConnection connection, IRCMessage rawMessage) {
        super(connection);
        this.rawMessage = rawMessage;
    }

}
