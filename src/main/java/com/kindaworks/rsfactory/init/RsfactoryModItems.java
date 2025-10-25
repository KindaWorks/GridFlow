/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.rsfactory.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import com.kindaworks.rsfactory.RsfactoryMod;

public class RsfactoryModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(RsfactoryMod.MODID);
	public static final DeferredItem<Item> FACTORY_MONITOR = block(RsfactoryModBlocks.FACTORY_MONITOR);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}