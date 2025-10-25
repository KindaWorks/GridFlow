package com.kindaworks.gridflow.api;

import com.kindaworks.gridflow.resource.ResourceChangeKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class FlowScopeStorageListener implements RootStorageListener {
    private final Map<ResourceChangeKey, Long> deltaChange = new HashMap<>();
    private Map<ResourceChangeKey, Long> lastSnapshot;

    public FlowScopeStorageListener(@Nullable Map<ResourceChangeKey, Long> savedLastSnapshot) {
        lastSnapshot = savedLastSnapshot;
    }

    @Override
    public long afterInsert(ResourceKey resourceKey, long amount) {
        ResourceChangeKey snapshotKey = new ResourceChangeKey((PlatformResourceKey) resourceKey, (short) +1);
        try {
            snapshotKey = new ResourceChangeKey((PlatformResourceKey) ((ItemResource) snapshotKey.resourceKey()).normalize(), (short) +1);
        } catch (Exception ignored) {
        }

        long newDeltaChange = deltaChange.getOrDefault(snapshotKey, 0L) + amount;
        deltaChange.put(snapshotKey, newDeltaChange);
        return 0;
    }

    @Override
    public void changed(MutableResourceList.OperationResult change) {
        if (change.change() >= 0) return; // filtering only exported items

        ResourceChangeKey snapshotKey = new ResourceChangeKey((PlatformResourceKey) change.resource(), (short) -1);
        try {
            snapshotKey = new ResourceChangeKey((PlatformResourceKey) ((ItemResource) snapshotKey.resourceKey()).normalize(), (short) +1);
        } catch (Exception ignored) {
        }

        long newDeltaChange = deltaChange.getOrDefault(snapshotKey, 0L) + change.change();
        deltaChange.put(snapshotKey, newDeltaChange);
    }

    public Map<ResourceChangeKey, Long> flushDeltaChange() {
        lastSnapshot = new HashMap<>(deltaChange);
        deltaChange.clear();
        return lastSnapshot;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshot() {
        return lastSnapshot;
    }

}
