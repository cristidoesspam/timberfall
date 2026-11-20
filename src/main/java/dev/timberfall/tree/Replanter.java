package dev.timberfall.tree;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

final class Replanter {
	private Replanter() {}

	static @Nullable Block saplingFor(Block log) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(log);
		if (id == null) return null;

		String wood = id.getPath();
		if (wood.startsWith("stripped_")) wood = wood.substring("stripped_".length());
		for (String suffix : new String[] {"_log", "_wood", "_stem", "_hyphae"}) {
			if (wood.endsWith(suffix)) {
				wood = wood.substring(0, wood.length() - suffix.length());
				break;
			}
		}

		String planted = switch (wood) {
			case "mangrove" -> "mangrove_propagule";
			case "crimson", "warped" -> wood + "_fungus";
			default -> wood + "_sapling";
		};

		Block sapling = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(id.getNamespace(), planted));
		return sapling == null || sapling == Blocks.AIR ? null : sapling;
	}
}
