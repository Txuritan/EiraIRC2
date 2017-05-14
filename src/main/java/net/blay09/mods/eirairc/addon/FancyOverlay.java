package net.blay09.mods.eirairc.addon;

import net.blay09.mods.eirairc.api.config.IConfigProperty;
import net.blay09.mods.eirairc.api.event.*;
import net.blay09.mods.eirairc.client.gui.overlay.OverlayJoinLeave;
import net.blay09.mods.eirairc.util.ConfigHelper;
import net.blay09.mods.eirairc.util.MessageFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FancyOverlay {

    private final OverlayJoinLeave overlay;
    private IConfigProperty<Boolean> enabled;
    private IConfigProperty<Integer> visibleTime;
    private IConfigProperty<Float> scale;

    public FancyOverlay() {
        overlay = new OverlayJoinLeave(Minecraft.getMinecraft(), Minecraft.getMinecraft().fontRenderer);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInitConfig(InitConfigEvent.ClientGlobalSettings event) {
        enabled = event.config.registerProperty("eirairc", "enableFancyOverlay", "eirairc:config.property.eirairc_enableFancyOverlay", true);
        visibleTime = event.config.registerProperty("eirairc", "fancyOverlayLifetime", "eirairc:config.property.eirairc_fancyOverlayLifetime", 240);
        visibleTime.setMinMax(120, 2400);
        scale = event.config.registerProperty("eirairc", "fancyOverlayScale", "eirairc:config.property.eirairc_fancyOverlayScale", 0.5f);
        scale.setMinMax(0.5f, 1f);
        overlay.setVisibleTime(visibleTime);
        overlay.setScale(scale);
    }

    @SubscribeEvent
    public void onIRCUserJoin(IRCUserJoinEvent event) {
        if (enabled.get()) {
            if (ConfigHelper.getGeneralSettings(event.channel).muted.get() || !ConfigHelper.getBotSettings(event.channel).relayIRCJoinLeave.get()) {
                return;
            }
            String format = ConfigHelper.getBotSettings(event.channel).getMessageFormat().mcUserJoin;
            overlay.addMessage(MessageFormat.formatChatComponent(format, event.connection, event.channel, event.user, "", MessageFormat.Target.Minecraft, MessageFormat.Mode.Emote));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onIRCNickChange(IRCUserNickChangeEvent event) {
        if (enabled.get()) {
            if (ConfigHelper.getGeneralSettings(event.user).muted.get() || !ConfigHelper.getBotSettings(event.user).relayNickChanges.get()) {
                return;
            }
            String format = ConfigHelper.getBotSettings(event.user).getMessageFormat().mcUserNickChange;
            format = format.replace("{OLDNICK}", event.oldNick);
            overlay.addMessage(MessageFormat.formatChatComponent(format, event.connection, null, event.user, "", MessageFormat.Target.Minecraft, MessageFormat.Mode.Emote));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onIRCUserLeave(IRCUserLeaveEvent event) {
        if (enabled.get()) {
            if (ConfigHelper.getGeneralSettings(event.channel).muted.get() || !ConfigHelper.getBotSettings(event.channel).relayIRCJoinLeave.get()) {
                return;
            }
            String format = ConfigHelper.getBotSettings(event.channel).getMessageFormat().mcUserLeave;
            overlay.addMessage(MessageFormat.formatChatComponent(format, event.connection, event.channel, event.user, "", MessageFormat.Target.Minecraft, MessageFormat.Mode.Emote));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onConnectedEvent(IRCConnectEvent event) {
        if (enabled.get()) {
            overlay.addMessage(new TextComponentTranslation("eirairc:general.connected", event.connection.getHost()));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onConnectionFailed(IRCConnectionFailedEvent event) {
        if (enabled.get()) {
            overlay.addMessage(new TextComponentTranslation("eirairc:error.couldNotConnect", event.connection.getHost(), event.exception));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onReconnecting(IRCReconnectEvent event) {
        if (enabled.get()) {
            overlay.addMessage(new TextComponentTranslation("eirairc:general.reconnecting", event.connection.getHost(), event.waitingTime / 1000));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onDisconnectedEvent(IRCDisconnectEvent event) {
        if (enabled.get()) {
            overlay.addMessage(new TextComponentTranslation("eirairc:general.disconnected", event.connection.getHost()));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onIRCUserQuit(IRCUserQuitEvent event) {
        if (enabled.get()) {
            if (ConfigHelper.getGeneralSettings(event.user).muted.get() || !ConfigHelper.getBotSettings(event.user).relayIRCJoinLeave.get()) {
                return;
            }
            String format = ConfigHelper.getBotSettings(event.user).getMessageFormat().mcUserQuit;
            overlay.addMessage(MessageFormat.formatChatComponent(format, event.connection, null, event.user, "", MessageFormat.Target.Minecraft, MessageFormat.Mode.Emote));
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CHAT) {
            overlay.updateAndRender(event.getPartialTicks());
        }
    }

}
