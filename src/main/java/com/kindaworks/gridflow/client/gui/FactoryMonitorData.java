package com.kindaworks.rsfactory.client.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FactoryMonitorData(boolean active) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FactoryMonitorData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, FactoryMonitorData::active,
                    FactoryMonitorData::new
            );
}
