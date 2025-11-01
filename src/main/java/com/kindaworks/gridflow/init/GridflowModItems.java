/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.gridflow.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import com.kindaworks.gridflow.GridflowMod;

public class GridflowModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(GridflowMod.MODID);
	public static final DeferredItem<Item> FLOW_SCOPE = block(GridflowModBlocks.FLOW_SCOPE);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}