/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.rsfactory.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import com.kindaworks.rsfactory.block.FactoryMonitorBlock;
import com.kindaworks.rsfactory.RsfactoryMod;

public class RsfactoryModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(RsfactoryMod.MODID);
	public static final DeferredBlock<Block> FACTORY_MONITOR = REGISTRY.register("factory_monitor", FactoryMonitorBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}