/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.gridflow.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import com.kindaworks.gridflow.GridFlowMod;

public class GridFlowModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GridFlowMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GRID_FLOW = REGISTRY.register("grid_flow",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.gridflow.grid_flow")).icon(() -> new ItemStack(GridFlowModBlocks.FLOW_SCOPE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(GridFlowModBlocks.FLOW_SCOPE.get().asItem());
			}).build());
}