// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api.event;

import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.api.irc.IRCMessage;

/**
 * This event is published on the MinecraftForge.EVENTBUS bus whenever EiraIRC successfully connects to an IRC server.
 */
public class IRCConnectEvent extends IRCRawMessageEvent {

    /**
     * INTERNAL EVENT. YOU SHOULD NOT POST THIS YOURSELF.
     *
     * @param connection the connection that was created
     */
    public IRCConnectEvent(IRCConnection connection, IRCMessage rawMessage) {
        super(connection, rawMessage);
    }

}
