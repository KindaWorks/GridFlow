package com.kindaworks.rsfactory.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;

import java.util.*;

public class RSFactorySnapshotData extends SavedData {

    private final List<Map<String, Long>> snapshots = new ArrayList<>();
    private final int maxSnapshots = 10000; // move to config

    /** Load from NBT */
    public static RSFactorySnapshotData load(CompoundTag tag, HolderLookup.Provider provider) {
        RSFactorySnapshotData data = new RSFactorySnapshotData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag snapTag = (CompoundTag) t;
            Map<String, Long> map = new HashMap<>();
            for (String key : snapTag.getAllKeys()) {
                map.put(key, snapTag.getLong(key));
            }
            data.snapshots.add(map);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map<String, Long> snap : snapshots) {
            CompoundTag snapTag = new CompoundTag();
            snap.forEach(snapTag::putLong);
            list.add(snapTag);
        }
        tag.put("snapshots", list);
        return tag;
    }

    /** Record one snapshot of deltas */
    public void recordSnapshot(Map<String, Long> deltaMap) {
        snapshots.add(new HashMap<>(deltaMap));
        trimToLast(maxSnapshots);
        setDirty();
    }

    /** Keep only the last N snapshots */
    public void trimToLast(int max) {
        int extra = snapshots.size() - max;
        if (extra > 0) snapshots.subList(0, extra).clear();
    }

    public List<Map<String, Long>> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    /** Fully NeoForge-compatible getter */
    public static RSFactorySnapshotData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        RSFactorySnapshotData::new,
                        RSFactorySnapshotData::load
                ),
                "rsfactory_snapshots"
        );
    }
}