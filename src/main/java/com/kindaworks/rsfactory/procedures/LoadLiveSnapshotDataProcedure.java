package com.kindaworks.rsfactory.procedures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import com.kindaworks.rsfactory.init.RsfactoryModMenus;
import com.kindaworks.rsfactory.RsfactoryMod;

import com.kindaworks.rsfactory.block.entity.FactoryMonitorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;


import java.util.HashMap;
import java.util.Map;

public class LoadLiveSnapshotDataProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        RsfactoryMod.queueServerWork(1, () -> {
            BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FactoryMonitorBlockEntity factoryMonitor) {
                Map<String, Long> snapshotMap = factoryMonitor.getLastSnapshot();
                Map<String, Map<Short, Long>> data = new HashMap<>();

                for (String signedKey : snapshotMap.keySet()) {
                    String key = signedKey.substring(1);
                    data.put(key, new HashMap<>());
                    Map<Short, Long> itemChange = data.get(key);
                    itemChange.put((short) +1, snapshotMap.getOrDefault("+" + key, 0L));
                    itemChange.put((short) -1, Math.abs(snapshotMap.getOrDefault("-" + key, 0L)));
                }
                if (entity instanceof Player player && player.containerMenu instanceof RsfactoryModMenus.MenuAccessor menu) {
                    menu.sendMenuStateUpdate(player, 2, "lastSnapshot", data, true);
                }
            }
        });
    }
}
