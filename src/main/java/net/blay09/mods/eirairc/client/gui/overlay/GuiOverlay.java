package net.blay09.mods.eirairc.client.gui.overlay;

import net.blay09.mods.eirairc.client.gui.EiraGuiScreen;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public abstract class GuiOverlay extends EiraGuiScreen {

    public GuiOverlay(GuiScreen parentScreen) {
        super(parentScreen);
        allowSideClickClose = false;
    }

    @Override
    public boolean mouseClick(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseX < menuX || mouseX >= menuX + menuWidth || mouseY < menuY || mouseY >= menuY + menuHeight) {
            gotoPrevious();
            return true;
        }
        super.controlClicked(mouseX, mouseY, mouseButton);
        return true;
    }

    @Override
    public void gotoPrevious() {
        ((EiraGuiScreen) parentScreen).setOverlay(null);
    }
}
