package com.kindaworks.gridflow.client.gui.components;

import com.refinedmods.refinedstorage.common.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public class DynamicButton {
    public final Runnable onClick;
    @Nullable
    public final Runnable onRightClick;
    private final WidgetSprites sprites;
    @Nullable
    private final ResourceLocation overlay;
    private final int x, y, width, height;
    private final String label;
    private final Font font;
    private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
    private final List<ClientTooltipComponent> tooltipLines;

    /**
     * Assumes default Minecraft button sprite
     */
    public DynamicButton(int x, int y, int width, int height, String label,
                         Runnable onClick, Runnable onRightClick, List<ClientTooltipComponent> lines) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = new WidgetSprites(
                ResourceLocation.withDefaultNamespace("widget/button"),
                ResourceLocation.withDefaultNamespace("widget/button_disabled"),
                ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        );
        this.overlay = null;
        this.tooltipLines = lines;
    }

    public DynamicButton(int x, int y, int width, int height, String label,
                         WidgetSprites sprites, ResourceLocation overlay, Runnable onClick, Runnable onRightClick, List<ClientTooltipComponent> lines) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = sprites;
        this.overlay = overlay;
        this.tooltipLines = lines;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        graphics.blitSprite(sprites.get(true, hovered), x, y, width, height);
        graphics.drawString(font, label, x + 5, y + 6, 0xFFFFFF);
        if (overlay != null) {
            graphics.blitSprite(overlay, x, y, width, height);
        }
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY))
            Platform.INSTANCE.renderTooltip(graphics, tooltipLines, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (button == 0 && onClick != null) {
                this.onClick.run();
                return true;
            } else if (button == 1 && onRightClick != null) {
                this.onRightClick.run();
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
}