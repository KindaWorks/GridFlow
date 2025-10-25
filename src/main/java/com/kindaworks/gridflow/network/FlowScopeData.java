package com.kindaworks.gridflow.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FlowScopeData(boolean active) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FlowScopeData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, FlowScopeData::active,
                    FlowScopeData::new
            );
}
