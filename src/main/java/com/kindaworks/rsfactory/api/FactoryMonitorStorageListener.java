package com.kindaworks.rsfactory.api;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FactoryMonitorStorageListener implements RootStorageListener {
    private boolean listening = false;
    private Map<String, Long> deltaChange = new HashMap<>();

    @Override
    public void changed(MutableResourceList.OperationResult change) {
        if (change.change()==change.amount()) return;

        ResourceKey resourceKey = change.resource();
        String snapshotKey;

        if (change.resource() instanceof ItemResource) {
            snapshotKey = ((ItemResource) change.resource()).item().toString();
        }
        else if (change.resource() instanceof FluidResource) {
            snapshotKey = ((FluidResource) change.resource()).fluid().toString();
        }
        else return; // Other types are not supported

        if (change.change() < 0) snapshotKey = "-" + snapshotKey.toString();
        else snapshotKey = "+" + snapshotKey.toString();

        long newDeltaChange = deltaChange.getOrDefault(snapshotKey, 0L) + change.change();
        deltaChange.put(snapshotKey, newDeltaChange);
    }

    public  Map<String, Long> flushDeltaChange() {
        try {
            return new HashMap<>(deltaChange);
        } finally {
            deltaChange.clear();
        }
    }

    public boolean isListening() {
        return listening;
    }

    public void start() {
        this.listening = true;
    }
}
