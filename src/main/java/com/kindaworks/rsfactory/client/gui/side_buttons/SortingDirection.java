package com.kindaworks.rsfactory.client.gui.side_buttons;

import net.minecraft.resources.ResourceLocation;

import java.util.SequencedMap;

public enum SortingDirection {
    ASCENDING {
        @Override
        protected String getSpritePath() {
            return super.getSpritePath() + "ascending";
        }

        @Override
        public <K, V> SequencedMap<K, V> sort(SequencedMap<K, V> map) {
            return map.reversed();
        }
    },
    DESCENDING {
        @Override
        protected String getSpritePath() {
            return super.getSpritePath() + "descending";
        }

        @Override
        public <K, V> SequencedMap<K, V> sort(SequencedMap<K, V> map) {
            return map;
        }
    };

    public static SortingDirection next(SortingDirection current) {
        return SortingDirection.values()[(current.ordinal() + 1) % SortingDirection.values().length];
    }

    protected String getSpritePath() {
        return "widget/side_button/grid/sorting_direction/";
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("refinedstorage", this.getSpritePath());
    }

    public abstract <K, V> SequencedMap<K, V> sort(SequencedMap<K, V> map);
}
