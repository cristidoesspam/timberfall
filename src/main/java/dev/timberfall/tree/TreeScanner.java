package dev.timberfall.tree;

import dev.timberfall.config.TimberSettings;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TreeScanner {
	public record Tree(List<BlockPos> logs, List<BlockPos> leaves, List<BlockPos> bases) {}

	private static final int RADIUS = 16;
	private static final int DOWN = 40;
	private static final int UP = 48;
	private static final int LEAF_CAP = 2048;

	private TreeScanner() {}

	public enum Failure { TOO_BIG, NO_LEAVES }

	public record Result(Tree tree, Failure failure) {}

	public static Tree scan(ServerLevel level, BlockPos origin, BlockState originState, TimberSettings settings) {
		return scanDetailed(level, origin, originState, settings).tree();
	}

	public static Result scanDetailed(ServerLevel level, BlockPos origin, BlockState originState, TimberSettings settings) {
		Block logBlock = originState.getBlock();

		LongOpenHashSet logSet = new LongOpenHashSet();
		List<BlockPos> logs = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		logSet.add(origin.asLong());
		queue.add(origin);

		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			logs.add(cur);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;
						BlockPos n = cur.offset(dx, dy, dz);
						if (!inBounds(origin, n) || logSet.contains(n.asLong())) continue;
						if (!level.isLoaded(n) || !level.getBlockState(n).is(logBlock)) continue;
						if (logSet.size() >= settings.maxBlocks) return new Result(null, Failure.TOO_BIG);
						logSet.add(n.asLong());
						queue.add(n);
					}
				}
			}
		}

		List<BlockPos> leaves = findLeaves(level, logs);
		if (settings.requireLeaves && leaves.isEmpty()) return new Result(null, Failure.NO_LEAVES);

		List<BlockPos> bases = new ArrayList<>();
		for (BlockPos log : logs) {
			if (!logSet.contains(log.below().asLong())) bases.add(log);
		}
		bases.sort(Comparator.comparingInt(BlockPos::getY));
		return new Result(new Tree(logs, leaves, bases), null);
	}

	private static boolean inBounds(BlockPos origin, BlockPos p) {
		int dy = p.getY() - origin.getY();
		return Math.abs(p.getX() - origin.getX()) <= RADIUS
			&& Math.abs(p.getZ() - origin.getZ()) <= RADIUS
			&& dy >= -DOWN && dy <= UP;
	}

	private static List<BlockPos> findLeaves(ServerLevel level, List<BlockPos> logs) {
		LongOpenHashSet seen = new LongOpenHashSet();
		List<BlockPos> result = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();

		for (BlockPos log : logs) {
			for (Direction dir : Direction.values()) {
				BlockPos n = log.relative(dir);
				if (seen.contains(n.asLong())) continue;
				BlockState s = level.getBlockState(n);
				if (isNaturalLeaf(s) && (!isDecayingLeaf(s) || s.getValue(LeavesBlock.DISTANCE) == 1)) {
					seen.add(n.asLong());
					queue.add(n);
				}
			}
		}

		while (!queue.isEmpty() && result.size() < LEAF_CAP) {
			BlockPos cur = queue.poll();
			result.add(cur);
			BlockState curState = level.getBlockState(cur);
			for (Direction dir : Direction.values()) {
				BlockPos n = cur.relative(dir);
				if (seen.contains(n.asLong()) || !level.isLoaded(n)) continue;
				BlockState s = level.getBlockState(n);
				if (!isNaturalLeaf(s)) continue;
				boolean follows = isDecayingLeaf(curState) && isDecayingLeaf(s)
					? s.getValue(LeavesBlock.DISTANCE) == curState.getValue(LeavesBlock.DISTANCE) + 1
					: !isDecayingLeaf(curState) && !isDecayingLeaf(s);
				if (follows) {
					seen.add(n.asLong());
					queue.add(n);
				}
			}
		}
		return result;
	}

	private static boolean isDecayingLeaf(BlockState s) {
		return s.getBlock() instanceof LeavesBlock;
	}

	private static boolean isNaturalLeaf(BlockState s) {
		if (s.getBlock() instanceof LeavesBlock) {
			return !s.getValue(LeavesBlock.PERSISTENT);
		}
		return s.is(BlockTags.WART_BLOCKS) || s.is(Blocks.SHROOMLIGHT);
	}
}
