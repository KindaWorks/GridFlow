package com.kindaworks.rsfactory.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DynamicButton {
    private final WidgetSprites sprites;
    @Nullable private final ResourceLocation overlay;

    private final int x, y, width, height;
    private final String label;
    private final Font font;

    public final Runnable onClick;

    /** Assumes default Minecraft button sprite */
    public DynamicButton(int x, int y, int width, int height, String label,
                         Runnable onClick) {
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
    }

    public DynamicButton(int x, int y, int width, int height, String label,
                         WidgetSprites sprites, ResourceLocation overlay, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = sprites;
        this.overlay = overlay;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);

        int color = hovered ? 0xFFAAAAAA : 0xFF666666;
//        p_281670_.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.blitSprite(sprites.get(true, hovered), x, y, width, height);
//        graphics.fill(x, y, x + width, y + height, color);
        graphics.drawString(font, label, x + 5, y + 6, 0xFFFFFF);
        if (overlay != null) {
            graphics.blitSprite(overlay, x, y, width, height);
        }
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

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}