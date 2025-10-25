/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.rsfactory.init;

import com.kindaworks.rsfactory.client.gui.AbstractFactoryMonitorMenuScreen;
import com.kindaworks.rsfactory.client.gui.FactoryMonitorMenuScreen;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import java.util.Map;

import com.kindaworks.rsfactory.world.inventory.FactoryMonitorMenuMenu;
import com.kindaworks.rsfactory.network.MenuStateUpdateMessage;
import com.kindaworks.rsfactory.RsfactoryMod;

public class RsfactoryModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, RsfactoryMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<FactoryMonitorMenuScreen>> FACTORY_MONITOR_MENU = REGISTRY.register("factory_monitor_menu", () -> FactoryMonitorMenuScreen());

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof RsfactoryModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}