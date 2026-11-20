package dev.timberfall.tree;

import dev.timberfall.config.TimberSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

final class FellingTask {
	private enum Result { BROKEN, SKIPPED, STOP }

	private static final int LEAF_BATCH = 6;
	private static final int MAX_PLANTED = 4;

	private final ServerPlayer player;
	private final ServerLevel level;
	private final TimberSettings settings;
	private final TreeScanner.Tree tree;
	private final Block logBlock;

	private final Block sapling;
	private final Item saplingItem;
	private final int saplingsWanted;
	private int saplingsReserved;

	private int logIndex;
	private int leafIndex;
	private int cooldown;
	private int effectsThisTick;
	private boolean pickedUp;
	private boolean aborted;

	FellingTask(ServerPlayer player, ServerLevel level, TimberSettings settings, TreeScanner.Tree tree, Block logBlock) {
		this.player = player;
		this.level = level;
		this.settings = settings;
		this.tree = tree;
		this.logBlock = logBlock;

		Block planted = settings.replant == TimberSettings.Replant.OFF ? null : Replanter.saplingFor(logBlock);
		this.sapling = planted;
		this.saplingItem = planted == null ? null : planted.asItem();
		this.saplingsWanted = planted == null ? 0 : Math.min(MAX_PLANTED, tree.bases().size());
	}

	boolean tick() {
		if (player.isRemoved() || !player.isAlive() || player.level() != level) {
			return true;
		}
		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		effectsThisTick = 0;
		pickedUp = false;
		boolean finished;
		if (settings.mode == TimberSettings.Mode.INSTANT) {
			while (hasMore() && breakNext()) {
			}
			finished = true;
		} else {
			finished = !breakNext() || !hasMore();
			cooldown = Math.max(0, settings.delayTicks - 1);
		}

		if (pickedUp) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
				0.2F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 1.4F + 2.0F);
		}
		if (finished) {
			finish();
		}
		return finished;
	}

	private boolean hasMore() {
		return logIndex < tree.logs().size() || (settings.breakLeaves && leafIndex < tree.leaves().size());
	}

	private boolean breakNext() {
		while (logIndex < tree.logs().size()) {
			Result result = breakLog(tree.logs().get(logIndex++));
			if (result == Result.BROKEN) return true;
			if (result == Result.STOP) {
				aborted = true;
				return false;
			}
		}

		if (!settings.breakLeaves) return true;
		int batch = settings.mode == TimberSettings.Mode.INSTANT ? Integer.MAX_VALUE : LEAF_BATCH;
		int broken = 0;
		while (leafIndex < tree.leaves().size() && broken < batch) {
			if (breakLeaf(tree.leaves().get(leafIndex++))) broken++;
		}
		return true;
	}

	private Result breakLog(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(logBlock) || !level.isLoaded(pos)) return Result.SKIPPED;

		ItemStack tool = player.getMainHandItem();
		if (settings.requireAxe && !tool.is(ItemTags.AXES)) return Result.STOP;
		if (!mayBreak(pos, state)) return Result.SKIPPED;

		List<ItemStack> drops = player.isCreative() ? List.of() : Block.getDrops(state, level, pos, null, player, tool);

		if (settings.useDurability && !player.isCreative() && tool.isDamageableItem()) {
			tool.hurtAndBreak(1, level, player, broken -> player.onEquippedItemBroken(broken, EquipmentSlot.MAINHAND));
		}

		remove(pos);
		if (!player.isCreative()) {
			player.causeFoodExhaustion(0.005F);
			player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
		}
		give(drops, pos);
		return Result.BROKEN;
	}

	private boolean breakLeaf(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !level.isLoaded(pos) || !mayBreak(pos, state)) return false;

		List<ItemStack> drops = player.isCreative() ? List.of() : Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
		remove(pos);
		give(drops, pos);
		return true;
	}

	private boolean mayBreak(BlockPos pos, BlockState state) {
		if (!level.mayInteract(player, pos)) return false;
		if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return false;
		return FellingManager.fireBreakEvent(level, player, pos, state);
	}

	private void remove(BlockPos pos) {
		effectsThisTick++;
		if (effectsThisTick <= 8 || effectsThisTick % 4 == 0) {
			level.destroyBlock(pos, false, player, 512);
		} else {
			level.removeBlock(pos, false);
		}
	}

	private void give(List<ItemStack> drops, BlockPos pos) {
		for (ItemStack stack : drops) {
			ItemStack rest = stack.copy();
			if (rest.isEmpty()) continue;

			if (saplingItem != null && settings.replant == TimberSettings.Replant.FROM_INVENTORY
				&& saplingsReserved < saplingsWanted && rest.is(saplingItem)) {
				int take = Math.min(rest.getCount(), saplingsWanted - saplingsReserved);
				rest.shrink(take);
				saplingsReserved += take;
			}
			if (rest.isEmpty()) continue;

			if (settings.autoPickup) {
				if (player.getInventory().add(rest)) pickedUp = true;
			}
			if (!rest.isEmpty()) {
				Block.popResource(level, pos, rest);
			}
		}
	}

	private void finish() {
		if (!aborted && sapling != null) {
			replant();
		}
		if (saplingsReserved > 0 && saplingItem != null) {
			int count = saplingsReserved;
			saplingsReserved = 0;
			give(List.of(new ItemStack(saplingItem, count)), player.blockPosition());
		}
	}

	private void replant() {
		int planted = 0;
		for (BlockPos base : tree.bases()) {
			if (planted >= MAX_PLANTED) break;
			BlockState current = level.getBlockState(base);
			if (!current.isAir() && !current.canBeReplaced()) continue;

			BlockState plant = sapling.defaultBlockState();
			if (!plant.canSurvive(level, base)) continue;
			if (!pay()) break;

			level.setBlock(base, plant, Block.UPDATE_ALL);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, base);
			level.playSound(null, base, plant.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
			planted++;
		}
	}

	private boolean pay() {
		if (settings.replant == TimberSettings.Replant.FREE || player.isCreative()) return true;
		if (saplingsReserved > 0) {
			saplingsReserved--;
			return true;
		}
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(saplingItem)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}
}
