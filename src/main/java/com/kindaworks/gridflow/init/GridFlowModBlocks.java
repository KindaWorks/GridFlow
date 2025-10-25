/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.gridflow.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import com.kindaworks.gridflow.block.FlowScopeBlock;
import com.kindaworks.gridflow.GridFlowMod;

public class GridFlowModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(GridFlowMod.MODID);
	public static final DeferredBlock<Block> FLOW_SCOPE = REGISTRY.register("flow_scope", FlowScopeBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}