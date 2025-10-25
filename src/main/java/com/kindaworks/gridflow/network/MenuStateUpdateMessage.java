package com.kindaworks.gridflow.network;

import com.kindaworks.gridflow.GridFlowMod;
import com.kindaworks.gridflow.init.GridFlowModScreens;
import com.kindaworks.gridflow.procedures.LoadLiveSnapshotDataProcedure;
import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
import com.kindaworks.gridflow.world.inventory.FlowScopeMenuMenu;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@EventBusSubscriber
public record MenuStateUpdateMessage(int elementType, String name, Object elementState) implements CustomPacketPayload {

    public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GridFlowMod.MODID, "guistate_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.of(MenuStateUpdateMessage::write, MenuStateUpdateMessage::read);

    public static void write(FriendlyByteBuf buffer, MenuStateUpdateMessage message) {
        buffer.writeInt(message.elementType);
        buffer.writeUtf(message.name);
        if (message.elementType == 0) {
            buffer.writeUtf((String) message.elementState);
        } else if (message.elementType == 1) {
            buffer.writeBoolean((boolean) message.elementState);
        } else if (message.elementType == 2) {
            buffer.writeMap(
                    (Map<PlatformResourceKey, Map<Short, Long>>) message.elementState,
                    (buf, key) -> ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, key),
                    (buf, value) -> buf.writeMap(
                            value,
                            (b, s) -> b.writeShort(s),
                            FriendlyByteBuf::writeLong
                    )
            );
        } else if (message.elementType == 3) {
            Map<ResourceChangeGranularityKey, long[]> state = (Map<ResourceChangeGranularityKey, long[]>) message.elementState;
            ResourceChangeGranularityKey firstKey = state.keySet().iterator().next();
            ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, firstKey.resourceKey());
            buffer.writeMap(
                    state,
                    (buf, key) -> buf.writeShort(key.sign()),
                    FriendlyByteBuf::writeLongArray
            );
            buffer.writeInt(firstKey.granularity());
        } else if (message.elementType == 4) {
            buffer.writeInt((int) message.elementState);
        } else if (message.elementType == 5) {
            ResourceChangeGranularityKey itemKey = (ResourceChangeGranularityKey) message.elementState;
            ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, itemKey.resourceKey());
            buffer.writeInt(itemKey.granularity());
        }
    }

    public static MenuStateUpdateMessage read(FriendlyByteBuf buffer) {
        int elementType = buffer.readInt();
        String name = buffer.readUtf();
        Object elementState = null;
        if (elementType == 0) {
            elementState = buffer.readUtf();
        } else if (elementType == 1) {
            elementState = buffer.readBoolean();
        } else if (elementType == 2) {
            elementState = buffer.readMap(
                    (buf) -> ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf),
                    (buf, map) -> buf.readMap(FriendlyByteBuf::readShort, FriendlyByteBuf::readLong)
            );
        } else if (elementType == 3) {
            PlatformResourceKey itemKey = ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
            Map<Short, long[]> map = buffer.readMap(
                    FriendlyByteBuf::readShort,
                    (buf, arr) -> buf.readLongArray()
            );
            Integer granularity = buffer.readInt();
            elementState = map.entrySet().stream()
                    .map(e -> Map.entry(new ResourceChangeGranularityKey(itemKey, e.getKey(), granularity), e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        } else if (elementType == 4) {
            elementState = buffer.readInt();
        } else if (elementType == 5) {
            PlatformResourceKey itemKey = ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
            Integer granularity = buffer.readInt();
            elementState = new ResourceChangeGranularityKey(itemKey, (short) 0, granularity);
        }
        return new MenuStateUpdateMessage(elementType, name, elementState);
    }

    public static void handleMenuState(final MenuStateUpdateMessage message, final IPayloadContext context) {
        if (message.name.length() > 256 || message.elementState instanceof String string && string.length() > 8192)
            return;
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof FlowScopeMenuMenu menu) {
                if (message.name.equals("detailedFactoryGenerationRequest")) {
                    ResourceChangeGranularityKey itemKey = ((ResourceChangeGranularityKey) message.elementState);
                    LoadLiveSnapshotDataProcedure.executeDetailed(context.player().level(), menu.x, menu.y, menu.z, context.player(), itemKey.resourceKey(), itemKey.granularity());
                } else if (message.name.equals("simpleFactoryGenerationRequest")) {
                    int granularity = (int) message.elementState;
                    LoadLiveSnapshotDataProcedure.execute(context.player().level(), menu.x, menu.y, menu.z, context.player(), granularity);
                } else {
                    menu.getMenuState().put(message.elementType + ":" + message.name, message.elementState);
                }
                if (context.flow() == PacketFlow.CLIENTBOUND && Minecraft.getInstance().screen instanceof GridFlowModScreens.ScreenAccessor accessor) {
                    accessor.updateMenuState(message.elementType, message.name, message.elementState);
                }
            }
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
        });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        GridFlowMod.addNetworkMessage(MenuStateUpdateMessage.TYPE, MenuStateUpdateMessage.STREAM_CODEC, MenuStateUpdateMessage::handleMenuState);
    }

    @Override
    public Type<MenuStateUpdateMessage> type() {
        return TYPE;
    }
}