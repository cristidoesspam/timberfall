package dev.timberfall.network;

import dev.timberfall.config.TimberSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SettingsPayload(TimberSettings settings) implements CustomPacketPayload {
	public static final Type<SettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("timberfall", "settings"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.of(
		(buf, payload) -> payload.settings().write(buf),
		buf -> new SettingsPayload(TimberSettings.read(buf))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
