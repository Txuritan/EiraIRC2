// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api.event;

import net.blay09.mods.eirairc.api.irc.IRCConnection;

/**
 * This event is published on the MinecraftForge.EVENTBUS bus whenever EiraIRC attempts to reconnect to IRC after the connection was lost.
 */
public class IRCReconnectEvent extends IRCEvent {

    /**
     * the amount of milliseconds EiraIRC will wait before attempting to reconnect
     */
    public final int waitingTime;

    /**
     * INTERNAL EVENT. YOU SHOULD NOT POST THIS YOURSELF.
     *
     * @param connection  the connection that is being reconnected to
     * @param waitingTime the amount of milliseconds EiraIRC will wait before attempting to reconnect
     */
    public IRCReconnectEvent(IRCConnection connection, int waitingTime) {
        super(connection);
        this.waitingTime = waitingTime;
    }

}
