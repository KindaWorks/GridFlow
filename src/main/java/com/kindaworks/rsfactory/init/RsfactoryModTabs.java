/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.rsfactory.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import com.kindaworks.rsfactory.RsfactoryMod;

public class RsfactoryModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RsfactoryMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RS_FACTORY = REGISTRY.register("rs_factory",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.rsfactory.rs_factory")).icon(() -> new ItemStack(RsfactoryModBlocks.FACTORY_MONITOR.get())).displayItems((parameters, tabData) -> {
				tabData.accept(RsfactoryModBlocks.FACTORY_MONITOR.get().asItem());
			}).build());
}