package com.kindaworks.gridflow.block.entity;

import com.kindaworks.gridflow.api.FlowScopeStorageListener;
import com.kindaworks.gridflow.data.GridFlowSnapshotData;
import com.kindaworks.gridflow.init.GridflowModBlockEntities;
import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
import com.kindaworks.gridflow.resource.ResourceChangeKey;
import com.kindaworks.gridflow.util.TickScheduler;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import com.refinedmods.refinedstorage.common.util.PlatformUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class FlowScopeBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowScopeBlockEntity.FlowScopeNetworkNode> {
    public int TAG_FACTORY_ID;
    private TickScheduler saveScheduler = new TickScheduler(20);
    private GridFlowSnapshotData snapshotData;

    public FlowScopeBlockEntity(BlockPos position, BlockState state) {
        super(GridflowModBlockEntities.FLOW_SCOPE.get(), position, state, new FlowScopeNetworkNode(100));
        setFactoryId();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            this.snapshotData = GridFlowSnapshotData.get((ServerLevel) getLevel(), TAG_FACTORY_ID);
            this.saveScheduler = new TickScheduler(snapshotData.dataGranularity);
        }
    }

    @Override
    public Component getName() {
        return Component.literal("Flow Scope");
    }

    @Override
    public void activenessChanged(final boolean newActive) {
        super.activenessChanged(newActive);
        PlatformUtil.sendBlockUpdateToClient(level, worldPosition);
    }

    public boolean isActive() {
        return mainNetworkNode.isActive();
    }

    @Override
    protected InWorldNetworkNodeContainer createMainContainer(final FlowScopeNetworkNode networkNode) {
        return RefinedStorageApi.INSTANCE.createNetworkNodeContainer(this, networkNode)
                .priority(10)
                .connectionStrategy(new SimpleConnectionStrategy(getBlockPos()))
                .build();
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
        if (mainNetworkNode.isActive()) {
            if (saveScheduler.shouldRun()) {
                snapshotData.recordSnapshot(mainNetworkNode.networkChangeListener.flushDeltaChange());
            }
        }
        ticker.tick(mainNetworkNode);
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(int granularity) {
        return snapshotData.getLastSnapshotAggregated(granularity);
    }

    public Map<ResourceChangeGranularityKey, long[]> getDetailedSnapshot(PlatformResourceKey itemKey, int granularity) {
        Map<ResourceChangeGranularityKey, long[]> ret = snapshotData.getGenerationDetails(itemKey, granularity);
        long[] itemAmount = new long[1];
        itemAmount[0] = getStorageNetworkComponent().get().get(itemKey);
        ret.put(new ResourceChangeGranularityKey(itemKey, (short) 0, granularity), itemAmount);
        return ret;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("FactoryId", TAG_FACTORY_ID);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("FactoryId")) {
            TAG_FACTORY_ID = tag.getInt("FactoryId");
        }
    }

    public void setFactoryId() {
        TAG_FACTORY_ID = this.hashCode();
    }

    // MCreator's crap
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
        return this.saveWithFullMetadata(lookupProvider);
    }

    public static class FlowScopeNetworkNode extends SimpleNetworkNode {
        @Nullable
        private FlowScopeStorageListener networkChangeListener;

        public FlowScopeNetworkNode(long energyUsage) {
            super(energyUsage);
        }

        @Override
        public void setNetwork(@Nullable Network network) {
            if (this.getNetwork() != null && networkChangeListener != null) {
                this.getNetwork().getComponent(StorageNetworkComponent.class).removeListener(networkChangeListener);
            }

            super.setNetwork(network);
            if (network == null) return;

            networkChangeListener = new FlowScopeStorageListener(networkChangeListener != null ? networkChangeListener.getLastSnapshot() : null);
            network.getComponent(StorageNetworkComponent.class).addListener(networkChangeListener);
        }
    }
}