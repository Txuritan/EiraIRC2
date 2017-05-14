package net.blay09.mods.eirairc.client.gui.overlay;

import net.blay09.mods.eirairc.api.config.IConfigProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public class OverlayJoinLeave extends Gui {

    private static class JoinLeaveMessage {
        public final ITextComponent chatComponent;
        public int y;
        public float timeLeft;

        public JoinLeaveMessage(ITextComponent chatComponent, int y, float timeLeft) {
            this.chatComponent = chatComponent;
            this.y = y;
            this.timeLeft = timeLeft;
        }
    }

    private final List<JoinLeaveMessage> messages = new ArrayList<>();
    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private IConfigProperty<Integer> visibleTime;
    private IConfigProperty<Float> scale;

    public OverlayJoinLeave(Minecraft mc, FontRenderer fontRenderer) {
        this.mc = mc;
        this.fontRenderer = fontRenderer;
    }

    public void setVisibleTime(IConfigProperty<Integer> visibleTime) {
        this.visibleTime = visibleTime;
    }

    public void setScale(IConfigProperty<Float> scale) {
        this.scale = scale;
    }

    public void addMessage(ITextComponent component) {
        if (visibleTime == null) {
            return;
        }
        for (JoinLeaveMessage message : messages) {
            message.y -= fontRenderer.FONT_HEIGHT + 2;
        }
        messages.add(new JoinLeaveMessage(component, 0, visibleTime.get()));
    }

    public void updateAndRender(float renderTickTime) {
        if (scale == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        final int height = 64;
        int guiTop = resolution.getScaledHeight() - height;
        int guiLeft = resolution.getScaledWidth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiLeft, guiTop, 0f);
        GlStateManager.scale(scale.get(), scale.get(), 1f);
        GlStateManager.enableBlend();
        for (int i = messages.size() - 1; i >= 0; i--) {
            JoinLeaveMessage message = messages.get(i);
            message.timeLeft -= renderTickTime;
            int alpha = 255;
            if (message.timeLeft < visibleTime.get() / 5f) {
                alpha = (int) Math.max(11, (255f * (message.timeLeft / (visibleTime.get() / 5f))));
            }
            if (message.timeLeft <= 0) {
                messages.remove(i);
            }
            String formattedText = message.chatComponent.getFormattedText();
            fontRenderer.drawString(formattedText, -fontRenderer.getStringWidth(formattedText) - 16, message.y, 16777215 | (alpha << 24), true);
        }
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

}
