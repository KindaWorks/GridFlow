/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.gridflow.init;

import com.kindaworks.gridflow.GridflowMod;
import com.kindaworks.gridflow.network.MenuStateUpdateMessage;
import com.kindaworks.gridflow.world.inventory.FlowScopeMenuMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GridflowModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, GridflowMod.MODID);
    public interface MenuAccessor {
        Map<String, Object> getMenuState();

        Map<Integer, Slot> getSlots();

        default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
            getMenuState().put(elementType + ":" + name, elementState);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
            } else if (player.level().isClientSide) {
                if (Minecraft.getInstance().screen instanceof GridflowModScreens.ScreenAccessor accessor && needClientUpdate)
                    accessor.updateMenuState(elementType, name, elementState);
                PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
            }
        }

        @SuppressWarnings("unchecked")
        default <T> T getMenuState(int elementType, String name, T defaultValue) {
            try {
                return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
    }    public static final DeferredHolder<MenuType<?>, MenuType<FlowScopeMenuMenu>> FLOW_SCOPE_MENU = REGISTRY.register("flow_scope_menu", () -> IMenuTypeExtension.create(FlowScopeMenuMenu::new));


}