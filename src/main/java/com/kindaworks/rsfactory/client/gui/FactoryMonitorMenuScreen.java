package com.kindaworks.rsfactory.client.gui;

import com.refinedmods.refinedstorage.api.autocrafting.status.TaskStatus;
import com.refinedmods.refinedstorage.api.autocrafting.status.TaskStatusListener;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskState;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import com.refinedmods.refinedstorage.common.support.packet.c2s.C2SPackets;
import com.refinedmods.refinedstorage.common.support.packet.s2c.S2CPackets;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.refinedmods.refinedstorage.common.support.widget.RedstoneModeSideButtonWidget;
import com.refinedmods.refinedstorage.common.support.widget.ScrollbarWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;

public class FactoryMonitorMenuScreen extends AbstractBaseScreen<AbstractFactoryMonitorMenuScreen> {
    private static final int ROWS_VISIBLE = 6;
    private static final int COLUMNS = 3;
    private static final int ITEMS_AREA_HEIGHT = 179;

    private static final int ROW_HEIGHT = 30;
    private static final int ROW_WIDTH = 221;

    private static final ResourceLocation TEXTURE = createIdentifier("textures/screens/factory_monitor_menu.png");
    private static final ResourceLocation ROW = createIdentifier("textures/sprites/row");

    @Nullable
    private ScrollbarWidget taskItemsScrollbar;
    @Nullable
    private ScrollbarWidget taskButtonsScrollbar;

    @Nullable
    private TaskId currentTaskId;
    private boolean active;

    public FactoryMonitorMenuScreen(final MenuType<?> menuType,
                                                       final AbstractFactoryMonitorMenuScreen menu,
                                                       final Inventory playerInventory,
                                                       final Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 254;
        this.imageHeight = 231;
    }

    @Override
    protected void init() {
        super.init();
        taskItemsScrollbar = new ScrollbarWidget(
                leftPos + 235,
                topPos + 20,
                ScrollbarWidget.Type.NORMAL,
                ITEMS_AREA_HEIGHT
        );
        taskItemsScrollbar.setEnabled(false);
//        getMenu().setListener(this);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (taskItemsScrollbar != null) {
            taskItemsScrollbar.render(graphics, mouseX, mouseY, partialTicks);
        }
        if (taskButtonsScrollbar != null) {
            taskButtonsScrollbar.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float delta, final int mouseX, final int mouseY) {
        super.renderBg(graphics, delta, mouseX, mouseY);
        final List<TaskStatus.Item> items = getMenu().getCurrentItems();
        if (items.isEmpty() || taskItemsScrollbar == null || !getMenu().isActive()) {
            return;
        }
        final int x = leftPos + 8;
        final int y = topPos + 20;
        graphics.enableScissor(x, y, x + 221, y + ITEMS_AREA_HEIGHT);
        final int rows = Math.ceilDiv(items.size(), COLUMNS);
        for (int i = 0; i < rows; ++i) {
            final int scrollOffset = taskItemsScrollbar.isSmoothScrolling()
                    ? (int) taskItemsScrollbar.getOffset()
                    : (int) taskItemsScrollbar.getOffset() * ROW_HEIGHT;
            final int yy = y + (i * ROW_HEIGHT) - scrollOffset;
            renderRow(graphics, x, yy, i, items, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void renderRow(final GuiGraphics graphics,
                           final int x,
                           final int y,
                           final int i,
                           final List<TaskStatus.Item> items,
                           final double mouseX,
                           final double mouseY) {
        if (y <= topPos + 20 - ROW_HEIGHT || y > topPos + 20 + ITEMS_AREA_HEIGHT) {
            return;
        }
        graphics.blitSprite(ROW, x, y, ROW_WIDTH, ROW_HEIGHT);
        for (int column = i * COLUMNS; column < Math.min(i * COLUMNS + COLUMNS, items.size()); ++column) {
            final TaskStatus.Item item = items.get(column);
            final int xx = x + (column % COLUMNS) * 74;
//            renderItem(graphics, xx, y, item, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int clickedButton) {
        if (taskItemsScrollbar != null
                && taskItemsScrollbar.mouseClicked(mouseX, mouseY, clickedButton)) {
            return true;
        }
        if (taskButtonsScrollbar != null && taskButtonsScrollbar.mouseClicked(mouseX, mouseY, clickedButton)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, clickedButton);
    }

    @Override
    public void mouseMoved(final double mx, final double my) {
        if (taskItemsScrollbar != null) {
            taskItemsScrollbar.mouseMoved(mx, my);
        }
        if (taskButtonsScrollbar != null) {
            taskButtonsScrollbar.mouseMoved(mx, my);
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        if (taskItemsScrollbar != null && taskItemsScrollbar.mouseReleased(mx, my, button)) {
            return true;
        }
        if (taskButtonsScrollbar != null && taskButtonsScrollbar.mouseReleased(mx, my, button)) {
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double z, final double delta) {
        final boolean didTaskItemsScrollbar = taskItemsScrollbar != null
                && isHoveringOverItems(x, y)
                && taskItemsScrollbar.mouseScrolled(x, y, z, delta);
        return didTaskItemsScrollbar || super.mouseScrolled(x, y, z, delta);
    }

    private boolean isHoveringOverItems(final double x, final double y) {
        return isHovering(8, 20, 221, ITEMS_AREA_HEIGHT, x, y);
    }

    @Override
    public void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
