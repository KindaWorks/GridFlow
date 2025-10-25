package com.kindaworks.rsfactory.client.gui.side_buttons;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public enum SortingType {
    NAME {
        @Override
        public String getSpritePath() {
            return super.getSpritePath() + "name";
        }

        @Override
        public LinkedHashMap<PlatformResourceKey, Map<Short, Long>> sort(Map<PlatformResourceKey, Map<Short, Long>> map) {
            return map.entrySet().stream()
                    .sorted((a, b) ->
                            Map.Entry.<String, Integer>comparingByKey().compare(
                                    Map.entry(a.getKey().getResourceType().getTitle().getString(), 0),
                                    Map.entry(b.getKey().getResourceType().getTitle().getString(), 0)
                            ))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        }
    },
    QUANTITY {
        @Override
        public String getSpritePath() {
            return super.getSpritePath() + "quantity";
        }

        @Override
        public LinkedHashMap<PlatformResourceKey, Map<Short, Long>> sort(Map<PlatformResourceKey, Map<Short, Long>> map) {
            return map.entrySet().stream()
                    .sorted(Comparator.comparingLong(entry -> {
                        Map<Short, Long> inner = entry.getValue();
                        long generation = inner.getOrDefault((short) +1, 0L);
                        long consumption = inner.getOrDefault((short) -1, 0L);
                        return generation - consumption;
                    }))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        }
    };

    public static SortingType next(SortingType current) {
        return SortingType.values()[(current.ordinal() + 1) % SortingType.values().length];
    }

    protected String getSpritePath() {
        return "widget/side_button/grid/sorting_type/";
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("refinedstorage", this.getSpritePath());
    }

    public abstract SequencedMap<PlatformResourceKey, Map<Short, Long>> sort(Map<PlatformResourceKey, Map<Short, Long>> map);
}