package com.kindaworks.gridflow.procedures;

import com.kindaworks.gridflow.GridflowMod;
import com.kindaworks.gridflow.block.entity.FlowScopeBlockEntity;
import com.kindaworks.gridflow.init.GridflowModMenus;
import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
import com.kindaworks.gridflow.resource.ResourceChangeKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class LoadLiveSnapshotDataProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, int granularity) {
        GridflowMod.queueServerWork(1, () -> {
            BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowScopeBlockEntity flowScope && flowScope.isActive()) {
                Map<ResourceChangeKey, Long> snapshotMap = flowScope.getLastSnapshotAggregated(granularity);
                Map<PlatformResourceKey, Map<Short, Long>> data = new HashMap<>();

                for (ResourceChangeKey resourceChangeKey : snapshotMap.keySet()) {
                    PlatformResourceKey resourceKey = resourceChangeKey.resourceKey();
                    data.put(resourceKey, new HashMap<>());
                    Map<Short, Long> itemChange = data.get(resourceKey);
                    itemChange.put((short) +1, snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) +1), 0L));
                    itemChange.put((short) -1, Math.abs(snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) -1), 0L)));
                }
                if (entity instanceof Player player && player.containerMenu instanceof GridflowModMenus.MenuAccessor menu) {
                    menu.sendMenuStateUpdate(player, 2, "lastSnapshot", data, true);
                }
            }
        });
    }

    public static void executeDetailed(LevelAccessor world, double x, double y, double z, Entity entity, PlatformResourceKey itemKey, int granularity) {
        GridflowMod.queueServerWork(1, () -> {
            BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowScopeBlockEntity flowScope && flowScope.isActive()) {
                Map<ResourceChangeGranularityKey, long[]> data = flowScope.getDetailedSnapshot(itemKey, granularity);
                if (entity instanceof Player player && player.containerMenu instanceof GridflowModMenus.MenuAccessor menu) {
                    menu.sendMenuStateUpdate(player, 3, "detailedFactoryGeneration", data, true);
                }
            }
        });
    }
}
