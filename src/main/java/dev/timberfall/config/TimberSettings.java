package dev.timberfall.config;

import net.minecraft.network.FriendlyByteBuf;

public final class TimberSettings {
	public enum Mode { INSTANT, BLOCK_BY_BLOCK }

	public enum Replant { OFF, FROM_INVENTORY, FREE }

	public enum SneakRule { SNEAK_PAUSES, SNEAK_REQUIRED, IGNORE }

	public static final int MIN_DELAY_TICKS = 1;
	public static final int MAX_DELAY_TICKS = 20;
	public static final int MIN_BLOCKS = 16;
	public static final int MAX_BLOCKS = 2048;
	public static final int BLOCK_STEP = 16;

	public boolean enabled = true;
	public Mode mode = Mode.INSTANT;
	public int delayTicks = 2;
	public boolean autoPickup = true;
	public boolean useDurability = true;
	public boolean protectAxe = true;
	public Replant replant = Replant.FROM_INVENTORY;
	public boolean breakLeaves = true;
	public boolean requireAxe = true;
	public boolean requireLeaves = true;
	public SneakRule sneakRule = SneakRule.SNEAK_PAUSES;
	public int maxBlocks = 512;

	public TimberSettings copy() {
		TimberSettings c = new TimberSettings();
		c.enabled = enabled;
		c.mode = mode;
		c.delayTicks = delayTicks;
		c.autoPickup = autoPickup;
		c.useDurability = useDurability;
		c.protectAxe = protectAxe;
		c.replant = replant;
		c.breakLeaves = breakLeaves;
		c.requireAxe = requireAxe;
		c.requireLeaves = requireLeaves;
		c.sneakRule = sneakRule;
		c.maxBlocks = maxBlocks;
		return c;
	}

	public TimberSettings sanitize() {
		if (mode == null) mode = Mode.INSTANT;
		if (replant == null) replant = Replant.FROM_INVENTORY;
		if (sneakRule == null) sneakRule = SneakRule.SNEAK_PAUSES;
		delayTicks = Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, delayTicks));
		maxBlocks = Math.max(MIN_BLOCKS, Math.min(MAX_BLOCKS, maxBlocks));
		return this;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(enabled);
		buf.writeEnum(mode);
		buf.writeVarInt(delayTicks);
		buf.writeBoolean(autoPickup);
		buf.writeBoolean(useDurability);
		buf.writeBoolean(protectAxe);
		buf.writeEnum(replant);
		buf.writeBoolean(breakLeaves);
		buf.writeBoolean(requireAxe);
		buf.writeBoolean(requireLeaves);
		buf.writeEnum(sneakRule);
		buf.writeVarInt(maxBlocks);
	}

	public static TimberSettings read(FriendlyByteBuf buf) {
		TimberSettings s = new TimberSettings();
		s.enabled = buf.readBoolean();
		s.mode = buf.readEnum(Mode.class);
		s.delayTicks = buf.readVarInt();
		s.autoPickup = buf.readBoolean();
		s.useDurability = buf.readBoolean();
		s.protectAxe = buf.readBoolean();
		s.replant = buf.readEnum(Replant.class);
		s.breakLeaves = buf.readBoolean();
		s.requireAxe = buf.readBoolean();
		s.requireLeaves = buf.readBoolean();
		s.sneakRule = buf.readEnum(SneakRule.class);
		s.maxBlocks = buf.readVarInt();
		return s.sanitize();
	}
}
