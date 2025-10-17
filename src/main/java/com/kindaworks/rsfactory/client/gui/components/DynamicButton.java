package com.kindaworks.rsfactory.client.gui.components;

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

<<<<<<< HEAD:src/main/java/com/kindaworks/rsfactory/client/gui/DynamicButton.java
    public final Runnable onClick;

    /** Assumes default Minecraft button sprite */
    public DynamicButton(int x, int y, int width, int height, String label,
                         Runnable onClick) {
=======
    /**
     * Assumes default Minecraft button sprite
     */
    public DynamicButton(int x, int y, int width, int height, String label,
                         Runnable onClick, Runnable onRightClick, List<ClientTooltipComponent> lines) {
>>>>>>> 4c5d786 (UI/UX overhaul):src/main/java/com/kindaworks/rsfactory/client/gui/components/DynamicButton.java
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
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
<<<<<<< HEAD:src/main/java/com/kindaworks/rsfactory/client/gui/DynamicButton.java
                         WidgetSprites sprites, ResourceLocation overlay, Runnable onClick) {
=======
                         WidgetSprites sprites, ResourceLocation overlay, Runnable onClick, Runnable onRightClick, List<ClientTooltipComponent> lines) {
>>>>>>> 4c5d786 (UI/UX overhaul):src/main/java/com/kindaworks/rsfactory/client/gui/components/DynamicButton.java
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = sprites;
        this.overlay = overlay;
        this.tooltipLines = lines;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);

//        p_281670_.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.blitSprite(sprites.get(true, hovered), x, y, width, height);
//        graphics.fill(x, y, x + width, y + height, color);
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
            }
        }
        return false;
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public String getLabel() {
        return label;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}