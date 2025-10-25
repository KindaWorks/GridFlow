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
    private final List<Map<String, Long>> snapshots = new ArrayList<>();
    private final List<Map<ResourceChangeKey, Long>> snapshotsv2 = new ArrayList<>();
    private final int maxCollectionSnapshots = 20 * 60 * 60 * 24 * 7; // TODO: move to config
    private final int maxSnapshotsForClientboundPacket = 209; // moving that to config might be quite tricky

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
                                        .decode(NbtOps.INSTANCE, itemsMapTag.get(String.valueOf(Math.abs(Integer.parseInt(itemId)))))
                                        .result().stream()
                                        .map(p -> new ResourceChangeKey(p.getFirst(), t.getLong(itemId) > 0 ? (short) +1 : (short) -1))
                                        .map(k -> Map.entry(k, t.getLong(itemId)))
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ).forEach(data.snapshotsv2::add);
        return data;
    }

    public static GridFlowSnapshotData get(ServerLevel level, int networkId) {
        GridFlowSnapshotData instance = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        GridFlowSnapshotData::new,
                        GridFlowSnapshotData::load
                ),
                "gridflow_snapshots/" + networkId
        );
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        AtomicInteger counter = new AtomicInteger(1);
        Map<PlatformResourceKey, Integer> itemsList = snapshotsv2.stream()
                .flatMap(map -> map.keySet().stream())
                .map(ResourceChangeKey::resourceKey)
                .distinct()
                .collect(Collectors.toMap(
                        a -> a,
                        __ -> counter.getAndIncrement(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        counter.set(1);
        CompoundTag itemsResolutionMapTag = itemsList.keySet().stream()
                .flatMap(k -> ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, k).result()
                        .stream())
                .collect(
                        CompoundTag::new,
                        (t, k) -> t.put(String.valueOf(counter.getAndIncrement()), k), CompoundTag::merge);
        tag.put("items_map", itemsResolutionMapTag);

        ListTag snapshotsTag = snapshotsv2.stream().map(snap ->
                        snap.entrySet().stream()
                                .map(k ->
                                        Map.entry(itemsList.get(k.getKey().resourceKey()) * (k.getKey().sign() > 0 ? 1 : -1), k.getValue())
                                ).collect(CompoundTag::new, (t, e) -> t.putLong(e.getKey().toString(), e.getValue()), CompoundTag::merge))
                .collect(ListTag::new, ListTag::add, ListTag::addAll);
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
        snapshotsv2.add(new HashMap<>(deltaMap));
        trimToLast(maxCollectionSnapshots);
        setDirty();
    }

    /**
     * Keep only the last N snapshots
     */
    public void trimToLast(int max) {
        int extra = snapshotsv2.size() - max;
        if (extra > 0) snapshotsv2.subList(0, extra).clear();
    }

    public List<Map<String, Long>> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    private List<LinkedHashMap<ResourceChangeKey, Long>> aggregateSnapshotsToGranularity(int desiredGranularity) {
        int granularity = desiredGranularity / dataGranularity;
        /**
         *  First, we're splitting the list into frames from the end of the list (.reversed()).
         *  Then we're summing them all by key.
         *  Then we're reversing it back again.
         *
         *  The whole double-reverse concept is there to take only the newest frames.
         */
        return IntStream.range(0, (int) Math.ceil((double) snapshotsv2.size() / granularity))
                // splitting into frames
                .mapToObj(
                        i -> snapshotsv2.reversed().subList(
                                i * granularity,
                                Math.min((i + 1) * granularity, snapshotsv2.size())
                        )
                )
                .map(frame -> frame.stream()
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Long::sum,
                                LinkedHashMap::new
                        ))
                )
                // precaution
                .limit((long) granularity * maxSnapshotsForClientboundPacket)
                .toList()
                .reversed();
    }

    public Map<ResourceChangeGranularityKey, long[]> getGenerationDetails(PlatformResourceKey itemKey, int desiredGranularity) {
        LongBuffer incbuf = LongBuffer.allocate(maxSnapshotsForClientboundPacket);
        LongBuffer decbuf = LongBuffer.allocate(maxSnapshotsForClientboundPacket);
        List<LinkedHashMap<ResourceChangeKey, Long>> aggregatedSnapshots = aggregateSnapshotsToGranularity(desiredGranularity);
        for (Map<ResourceChangeKey, Long> snapshot : aggregatedSnapshots) {
            if (incbuf.hasRemaining()) {
                incbuf.put(snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) +1), 0L));
                decbuf.put(Math.abs(snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) -1), 0L)));
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
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) +1, desiredGranularity), incbuf.array());
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) -1, desiredGranularity), decbuf.array());
        return generationDetails;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(int desiredGranularity) {
        List<LinkedHashMap<ResourceChangeKey, Long>> aggregatedSnapshots = aggregateSnapshotsToGranularity(desiredGranularity);
        if (aggregatedSnapshots.isEmpty()) return new HashMap<>();
        return aggregatedSnapshots.getLast();
    }
}