/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.gridflow.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import com.kindaworks.gridflow.block.entity.FlowScopeBlockEntity;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import com.kindaworks.gridflow.GridflowMod;

@EventBusSubscriber
public class GridflowModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GridflowMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlowScopeBlockEntity>> FLOW_SCOPE = register("flow_scope", GridflowModBlocks.FLOW_SCOPE, FlowScopeBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(), FLOW_SCOPE.get(), (be, side) -> be.getContainerProvider());
    }


}