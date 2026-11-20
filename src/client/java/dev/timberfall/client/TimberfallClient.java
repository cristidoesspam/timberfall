package dev.timberfall.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.timberfall.config.TimberConfig;
import dev.timberfall.network.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TimberfallClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath("timberfall", "timberfall"));

	private static KeyMapping openSettingsKey;
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		openSettingsKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.timberfall.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));
		toggleKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.timberfall.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendSettings());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				var settings = TimberConfig.get();
				settings.enabled = !settings.enabled;
				TimberConfig.save();
				sendSettings();
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.translatable(
						settings.enabled ? "timberfall.toggle.on" : "timberfall.toggle.off"));
				}
			}
			while (openSettingsKey.consumeClick()) {
				if (client.player != null) {
					client.gui.setScreen(new TimberfallScreen(client.gui.screen()));
				}
			}
		});
	}

	public static void sendSettings() {
		if (Minecraft.getInstance().player != null && ClientPlayNetworking.canSend(SettingsPayload.TYPE)) {
			ClientPlayNetworking.send(new SettingsPayload(TimberConfig.get().copy()));
		}
	}
}
