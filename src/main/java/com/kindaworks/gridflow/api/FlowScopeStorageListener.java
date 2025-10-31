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

    public ResourceChangeKey getSnapshotKey(PlatformResourceKey resourceKey, short sign) {
        ResourceChangeKey snapshotKey = new ResourceChangeKey((PlatformResourceKey) resourceKey, sign);
        try {
            snapshotKey = new ResourceChangeKey(
                    (PlatformResourceKey) ((ItemResource) snapshotKey.resourceKey()).normalize(), sign);
        } catch (Exception ignored) {
        }
        return snapshotKey;
    }

    @Override
    public void changed(MutableResourceList.OperationResult change) {
        // Abstract condition to filter out network change
        if ((Math.abs(change.change()) >= (change.amount() * .5)) && Math.abs(change.change()) > 30)
            return;
        short sign = (short) (change.change() >= 0 ? +1 : -1);
        ResourceChangeKey snapshotKey = getSnapshotKey((PlatformResourceKey) change.resource(), sign);
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
