package com.kindaworks.rsfactory.block.entity;

import com.kindaworks.rsfactory.api.FactoryMonitorStorageListener;
import com.kindaworks.rsfactory.data.RSFactorySnapshotData;
import com.kindaworks.rsfactory.util.TickScheduler;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.stream.IntStream;

import com.kindaworks.rsfactory.init.RsfactoryModBlockEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactoryMonitorBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode> implements WorldlyContainer {
    private static final Logger log = LoggerFactory.getLogger(FactoryMonitorBlockEntity.class);
    private final FactoryMonitorStorageListener LISTENER = new FactoryMonitorStorageListener();
    private final TickScheduler saveScheduler = new TickScheduler(20);
    private RSFactorySnapshotData snapshotData;

	public FactoryMonitorBlockEntity(BlockPos position, BlockState state) {
		super(RsfactoryModBlockEntities.FACTORY_MONITOR.get(), position, state, new SimpleNetworkNode(100));
	}

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            this.snapshotData = RSFactorySnapshotData.get((ServerLevel) getLevel());
        }
    }

    @Override
    public Component getName() {
        return Component.literal("Factory Monitor");
    }

    @Override
    protected void activenessChanged(final boolean newActive) {
        super.activenessChanged(newActive);
        log.info("Activeness changed for FactoryMonitorBlockEntity: " + newActive);
    }

    private Optional<StorageNetworkComponent> getStorageNetworkComponent() {
        final Network network = mainNetworkNode.getNetwork();
        if (network == null) {
            return Optional.empty();
        }
        return Optional.of(network.getComponent(StorageNetworkComponent.class));
    }

    @Override
    public void doWork() {
        if (!LISTENER.isListening()) {
            getStorageNetworkComponent().ifPresent(storageNetworkComponent -> {
                storageNetworkComponent.addListener(LISTENER);
                LISTENER.start();
            });
        }
        else {
            if (saveScheduler.shouldRun()) {
                snapshotData.recordSnapshot(LISTENER.flushDeltaChange());
            }
        }
        ticker.tick(mainNetworkNode);
    }


//	@Override
//	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
//		super.loadAdditional(compound, lookupProvider);
//		if (!this.tryLoadLootTable(compound))
//			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
//		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
//	}
//
//	@Override
//	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
//		super.saveAdditional(compound, lookupProvider);
//		if (!this.trySaveLootTable(compound)) {
//			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
//		}
//	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

    @Override
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return null;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        return;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

//    @Override
//	public Component getDefaultName() {
//		return Component.literal("factory_monitor");
//	}
//
//	@Override
//	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
//		return new FactoryMonitorMenuMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
//	}

//	@Override
//	protected NonNullList<ItemStack> getItems() {
//		return this.stacks;
//	}
//
//	@Override
//	protected void setItems(NonNullList<ItemStack> stacks) {
//		this.stacks = stacks;
//	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		return true;
	}

    @Override
    public void clearContent() {

    }
}