package net.blay09.mods.eirairc.client.gui.base.list;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Gui;

import java.util.List;

public abstract class GuiListEntry extends Gui {

    protected final List<String> tooltipList = Lists.newArrayList();
    private boolean selected;
    protected GuiList parentList;

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            onSelected();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void onSelected() {
    }

    public void onDoubleClick() {
    }

    public abstract void drawEntry(int x, int y);

    public void setParentList(GuiList parentList) {
        this.parentList = parentList;
    }

    public final List<String> getTooltipText() {
        return tooltipList;
    }
}
