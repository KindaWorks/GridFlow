/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.kindaworks.rsfactory.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import com.kindaworks.rsfactory.client.gui.FactoryMonitorMenuScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RsfactoryModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(RsfactoryModMenus.FACTORY_MONITOR_MENU.get(), FactoryMonitorMenuScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}