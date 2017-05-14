// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api.event;

import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event is published on the MinecraftForge.EVENTBUS bus for every IRC user nick in a message EiraIRC has received.
 * Mods adding name badges can use this to make their magic happen and adjust the chat component as needed.
 */
public class FormatNick extends Event {

    public final IRCUser user;
    public final IRCContext context;
    public ITextComponent component;

    /**
     * @param component the chat component containing the formatted nick
     */
    public FormatNick(IRCUser user, IRCContext context, ITextComponent component) {
        this.user = user;
        this.context = context;
        this.component = component;
    }

}
