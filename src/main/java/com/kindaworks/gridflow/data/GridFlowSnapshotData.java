package com.kindaworks.gridflow.data;

import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
import com.kindaworks.gridflow.resource.ResourceChangeKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GridFlowSnapshotData extends SavedData {

    public final int dataGranularity = 20; // TODO: move to config
    private final List<Map<ResourceChangeKey, Long>> snapshots = new ArrayList<>();
    private final int maxCollectionSnapshots = 20 * 60 * 60 * 24 * 7; // TODO: move to config
    private final int maxSnapshotsForClientboundPacket = 209; // moving that to config might be quite tricky

    private final Map<PlatformResourceKey, Integer> itemResolutionMap = new LinkedHashMap<>();
    private final AtomicInteger itemIdCounter = new AtomicInteger(1);

    public static GridFlowSnapshotData load(CompoundTag tag, HolderLookup.Provider provider) {
        GridFlowSnapshotData data = new GridFlowSnapshotData();
        ListTag snapshotsTag = tag.getList("snapshots", Tag.TAG_COMPOUND);
        CompoundTag itemsMapTag = tag.getCompound("items_map");
        snapshotsTag.stream()
                .map(CompoundTag.class::cast)
                .map(t -> t.getAllKeys().stream()
                        // snapshot scope
                        .flatMap(itemId ->
                        // item scope
                        ResourceCodecs.CODEC
                                .decode(NbtOps.INSTANCE, itemsMapTag.get(String.valueOf(
                                        Math.abs(Integer.parseInt(itemId)))))
                                .result().stream()
                                .map(p -> new ResourceChangeKey(p.getFirst(),
                                        t.getLong(itemId) > 0 ? (short) +1
                                                : (short) -1))
                                .map(k -> Map.entry(k, t.getLong(itemId))))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .forEach(data.snapshots::add);
        return data;
    }

    public static GridFlowSnapshotData get(ServerLevel level, int networkId) {
        GridFlowSnapshotData instance = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        GridFlowSnapshotData::new,
                        GridFlowSnapshotData::load),
                "gridflow_snapshots/" + networkId);
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // write items map
        CompoundTag itemsResolutionMapTag = new CompoundTag();
        AtomicInteger counter = new AtomicInteger(1);

        itemResolutionMap.forEach((key, id) -> {
            ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, key).result()
                    .ifPresent(k -> itemsResolutionMapTag.put(String.valueOf(counter.getAndIncrement()), k));
        });
        tag.put("items_map", itemsResolutionMapTag);

        // write snapshots
        ListTag snapshotsTag = new ListTag();
        for (Map<ResourceChangeKey, Long> snap : snapshots) {
            CompoundTag snapshotTag = new CompoundTag();
            for (var entry : snap.entrySet()) {
                int itemId = itemResolutionMap.computeIfAbsent(
                        entry.getKey().resourceKey(),
                        k -> itemIdCounter.getAndIncrement());
                int signedId = itemId * (entry.getKey().sign() > 0 ? 1 : -1);
                snapshotTag.putLong(Integer.toString(signedId), entry.getValue());
            }
            snapshotsTag.add(snapshotTag);
        }
        tag.put("snapshots", snapshotsTag);
        return tag;
    }

    public void save(File file, HolderLookup.Provider provider) {
        Path dir = Path.of(file.getParent());
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        super.save(file, provider);
    }

    /**
     * Record one snapshot of deltas
     */
    public void recordSnapshot(Map<ResourceChangeKey, Long> deltaMap) {
        // add new items to resolution map
        for (ResourceChangeKey key : deltaMap.keySet()) {
            PlatformResourceKey resKey = key.resourceKey();
            itemResolutionMap.computeIfAbsent(resKey, k -> itemIdCounter.getAndIncrement());
        }

        snapshots.add(new HashMap<>(deltaMap));
        trimToLast(maxCollectionSnapshots);
        setDirty();
    }

    /**
     * Keep only the last N snapshots
     */
    public void trimToLast(int max) {
        int extra = snapshots.size() - max;
        if (extra > 0)
            snapshots.subList(0, extra).clear();
    }

    public List<Map<ResourceChangeKey, Long>> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    private List<LinkedHashMap<ResourceChangeKey, Long>> aggregateSnapshotsToGranularity(int desiredGranularity) {
        int granularity = desiredGranularity / dataGranularity;
        /**
         * First, we're splitting the list into frames from the end of the list
         * (.reversed()).
         * Then we're summing them all by key.
         * Then we're reversing it back again.
         *
         * The whole double-reverse concept is there to take only the newest frames.
         */
        return IntStream.range(0, (int) Math.ceil((double) snapshots.size() / granularity))
                // splitting into frames
                .mapToObj(
                        i -> snapshots.reversed().subList(
                                i * granularity,
                                Math.min((i + 1) * granularity, snapshots.size())))
                .map(frame -> frame.stream()
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Long::sum,
                                LinkedHashMap::new)))
                // precaution
                .limit((long) granularity * maxSnapshotsForClientboundPacket)
                .toList()
                .reversed();
    }

    public Map<ResourceChangeGranularityKey, long[]> getGenerationDetails(PlatformResourceKey itemKey,
            int desiredGranularity) {
        LongBuffer incbuf = LongBuffer.allocate(maxSnapshotsForClientboundPacket);
        LongBuffer decbuf = LongBuffer.allocate(maxSnapshotsForClientboundPacket);
        List<LinkedHashMap<ResourceChangeKey, Long>> aggregatedSnapshots = aggregateSnapshotsToGranularity(
                desiredGranularity);
        for (Map<ResourceChangeKey, Long> snapshot : aggregatedSnapshots) {
            if (incbuf.hasRemaining()) {
                incbuf.put(snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) +1), 0L));
                decbuf.put(Math.abs(
                        snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) -1), 0L)));
            }
        }
        if (incbuf.hasRemaining()) {
            incbuf = LongBuffer
                    .allocate(incbuf.position())
                    .put(incbuf.flip().rewind());
            decbuf = LongBuffer
                    .allocate(incbuf.position())
                    .put(decbuf.flip().rewind());
        }
        Map<ResourceChangeGranularityKey, long[]> generationDetails = new HashMap<>();
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) +1, desiredGranularity),
                incbuf.array());
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) -1, desiredGranularity),
                decbuf.array());
        return generationDetails;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(int desiredGranularity) {
        List<LinkedHashMap<ResourceChangeKey, Long>> aggregatedSnapshots = aggregateSnapshotsToGranularity(
                desiredGranularity);
        if (aggregatedSnapshots.isEmpty())
            return new HashMap<>();
        return aggregatedSnapshots.getLast();
    }
}