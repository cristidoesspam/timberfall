package dev.timberfall;

import dev.timberfall.config.TimberConfig;
import dev.timberfall.network.SettingsPayload;
import dev.timberfall.tree.FellingManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timberfall implements ModInitializer {
	public static final String MOD_ID = "timberfall";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		TimberConfig.load();

		PayloadTypeRegistry.serverboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE,
			(payload, context) -> FellingManager.setSettings(context.player(), payload.settings()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FellingManager.forget(handler.getPlayer()));

		PlayerBlockBreakEvents.BEFORE.register(FellingManager::onBeforeBreak);
		ServerTickEvents.END_SERVER_TICK.register(FellingManager::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FellingManager.clear());
	}
}
