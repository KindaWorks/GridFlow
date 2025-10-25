package com.kindaworks.rsfactory.client.gui;

import com.refinedmods.refinedstorage.api.autocrafting.status.TaskStatus;
import com.refinedmods.refinedstorage.api.autocrafting.status.TaskStatusListener;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskState;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;

public abstract class AbstractFactoryMonitorMenuScreen extends AbstractBaseContainerMenu {
    private final Player player;
    private boolean active;

    protected AbstractFactoryMonitorMenuScreen(final MenuType<?> menuType,
                                       final int syncId,
                                       final Inventory playerInventory,
                                       final FactoryMonitorData data) {
        super(menuType, syncId);
        this.active = data.active();
        this.player = playerInventory.player;
    }

    AbstractFactoryMonitorMenuScreen(final MenuType<?> menuType,
                             final int syncId,
                             final Player player) {
        super(menuType, syncId);
        this.player = player;
    }

//    @Override
//    public void removed(final Player removedPlayer) {
//        super.removed(removedPlayer);
//        if (autocraftingMonitor != null) {
//            autocraftingMonitor.removeListener(this);
//            autocraftingMonitor.removeWatcher(this);
//        }
//    }

//    void setListener(@Nullable final AutocraftingMonitorListener listener) {
//        this.listener = listener;
//    }

    List<TaskStatus.Item> getCurrentItems() {
        final TaskStatus status = null; // statusByTaskId.get(currentTaskId);
        if (status == null) {
            return Collections.emptyList();
        }
        return status.items();
    }

    public void activeChanged(final boolean newActive) {
        if (player instanceof ServerPlayer serverPlayer) {
            S2CPackets.sendAutocraftingMonitorActive(serverPlayer, newActive);
        } else {
            this.active = newActive;
        }
    }

    boolean isActive() {
        return active;
    }
}
