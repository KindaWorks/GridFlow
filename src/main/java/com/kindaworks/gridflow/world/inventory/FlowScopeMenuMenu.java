package com.kindaworks.gridflow.world.inventory;

import com.kindaworks.gridflow.init.GridFlowModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowScopeMenuMenu extends AbstractContainerMenu implements GridFlowModMenus.MenuAccessor {
    public final Map<String, Object> menuState = new HashMap<>() {
        @Override
        public Object put(String key, Object value) {
            if (!this.containsKey(key) && this.size() >= 2)
                return null;
            return super.put(key, value);
        }
    };
    public final Level world;
    public final Player entity;
    private final BlockEntity boundBlockEntity = null;
    public int x, y, z;
    private ContainerLevelAccess access = ContainerLevelAccess.NULL;

    public FlowScopeMenuMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(GridFlowModMenus.FLOW_SCOPE_MENU.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        if (extraData != null) {
            BlockPos pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            access = ContainerLevelAccess.create(world, pos);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.boundBlockEntity != null)
            return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public Map<String, Object> getMenuState() {
        return menuState;
    }
}