package dev.timberfall.tree;

import dev.timberfall.config.TimberConfig;
import dev.timberfall.config.TimberSettings;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FellingManager {
	private static final int MAX_ACTIVE_TASKS = 64;
	private static final int HINT_COOLDOWN_TICKS = 100;

	private static final List<FellingTask> TASKS = new ArrayList<>();
	private static final Map<UUID, TimberSettings> PLAYER_SETTINGS = new HashMap<>();
	private static final Map<UUID, Long> LAST_HINT = new HashMap<>();

	private static boolean firingEvent;

	private FellingManager() {}

	public static void setSettings(ServerPlayer player, TimberSettings settings) {
		PLAYER_SETTINGS.put(player.getUUID(), settings.copy().sanitize());
	}

	public static void forget(ServerPlayer player) {
		PLAYER_SETTINGS.remove(player.getUUID());
		LAST_HINT.remove(player.getUUID());
	}

	public static void clear() {
		TASKS.clear();
		PLAYER_SETTINGS.clear();
		LAST_HINT.clear();
	}

	private static TimberSettings settingsFor(ServerPlayer player) {
		TimberSettings s = PLAYER_SETTINGS.get(player.getUUID());
		return s != null ? s : TimberConfig.get();
	}

	static boolean fireBreakEvent(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		firingEvent = true;
		try {
			return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, null);
		} finally {
			firingEvent = false;
		}
	}

	public static boolean onBeforeBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (firingEvent || !state.is(BlockTags.LOGS)) return true;
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;
		if (serverPlayer.isSpectator() || TASKS.size() >= MAX_ACTIVE_TASKS) return true;

		TimberSettings settings = settingsFor(serverPlayer);
		if (!settings.enabled) return true;

		boolean sneaking = serverPlayer.isShiftKeyDown();
		switch (settings.sneakRule) {
			case SNEAK_PAUSES -> {
				if (sneaking) return true;
			}
			case SNEAK_REQUIRED -> {
				if (!sneaking) return true;
			}
			case IGNORE -> { }
		}

		ItemStack tool = serverPlayer.getMainHandItem();
		if (settings.requireAxe && !tool.is(ItemTags.AXES)) return true;

		TreeScanner.Result scan = TreeScanner.scanDetailed(serverLevel, pos, state, settings);
		TreeScanner.Tree tree = scan.tree();
		if (tree == null) {
			if (scan.failure() == TreeScanner.Failure.TOO_BIG) {
				hint(serverPlayer, "Timberfall: this tree is over the " + settings.maxBlocks
					+ " log limit, so only one log broke. Raise Max Tree Size in the Timberfall settings.");
			}
			return true;
		}

		if (settings.useDurability && settings.protectAxe && !serverPlayer.isCreative() && tool.isDamageableItem()) {
			int remaining = tool.getMaxDamage() - tool.getDamageValue();
			if (remaining <= tree.logs().size()) {
				hint(serverPlayer, "Timberfall: this tree has " + tree.logs().size() + " logs but your axe only has " + remaining
					+ " durability left, so only one log broke. Use a sturdier axe or turn off Protect Axe.");
				return true;
			}
		}

		TASKS.add(new FellingTask(serverPlayer, serverLevel, settings.copy(), tree, state.getBlock()));
		return false;
	}

	private static void hint(ServerPlayer player, String message) {
		long now = player.level().getGameTime();
		Long last = LAST_HINT.get(player.getUUID());
		if (last != null && now - last < HINT_COOLDOWN_TICKS) return;
		LAST_HINT.put(player.getUUID(), now);
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW), true);
	}

	public static void tick(MinecraftServer server) {
		if (TASKS.isEmpty()) return;
		Iterator<FellingTask> it = TASKS.iterator();
		while (it.hasNext()) {
			if (it.next().tick()) it.remove();
		}
	}
}
