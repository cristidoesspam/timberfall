package dev.timberfall.client;

import com.mojang.serialization.Codec;
import dev.timberfall.config.TimberConfig;
import dev.timberfall.config.TimberSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TimberfallScreen extends OptionsSubScreen {
	private static final String KEY = "timberfall.option.";

	private final TimberSettings s = TimberConfig.get();
	private OptionInstance<Integer> delayOption;

	public TimberfallScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("timberfall.screen.title"));
	}

	@Override
	protected void addOptions() {
		list.addBig(toggle("enabled", s.enabled, v -> s.enabled = v));

		list.addSmall(
			cycle("mode", TimberSettings.Mode.class, s.mode, v -> {
				s.mode = v;
				updateDelayWidget();
			}),
			delayOption = new OptionInstance<>(KEY + "delay", tooltip("delay"),
				(caption, v) -> Component.translatable(KEY + "delay.value", caption, v, String.format(Locale.ROOT, "%.2f", v / 20.0)),
				new OptionInstance.IntRange(TimberSettings.MIN_DELAY_TICKS, TimberSettings.MAX_DELAY_TICKS),
				s.delayTicks, v -> s.delayTicks = v)
		);

		list.addSmall(
			toggle("auto_pickup", s.autoPickup, v -> s.autoPickup = v),
			toggle("use_durability", s.useDurability, v -> s.useDurability = v)
		);
		list.addSmall(
			toggle("protect_axe", s.protectAxe, v -> s.protectAxe = v),
			cycle("replant", TimberSettings.Replant.class, s.replant, v -> s.replant = v)
		);
		list.addSmall(
			toggle("break_leaves", s.breakLeaves, v -> s.breakLeaves = v),
			toggle("require_axe", s.requireAxe, v -> s.requireAxe = v)
		);
		list.addSmall(
			toggle("require_leaves", s.requireLeaves, v -> s.requireLeaves = v),
			cycle("sneak_rule", TimberSettings.SneakRule.class, s.sneakRule, v -> s.sneakRule = v)
		);

		int step = TimberSettings.BLOCK_STEP;
		list.addBig(new OptionInstance<>(KEY + "max_blocks", tooltip("max_blocks"),
			(caption, v) -> Component.translatable(KEY + "max_blocks.value", caption, v * step),
			new OptionInstance.IntRange(TimberSettings.MIN_BLOCKS / step, TimberSettings.MAX_BLOCKS / step),
			s.maxBlocks / step, v -> s.maxBlocks = v * step));

		updateDelayWidget();
	}

	private void updateDelayWidget() {
		if (list == null || delayOption == null) return;
		AbstractWidget widget = list.findOption(delayOption);
		if (widget != null) {
			widget.active = s.mode == TimberSettings.Mode.BLOCK_BY_BLOCK;
		}
	}

	@Override
	public void removed() {
		TimberConfig.save();
		TimberfallClient.sendSettings();
		super.removed();
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
		Tooltip tip = Tooltip.create(Component.translatable(KEY + name + ".tooltip"));
		return value -> tip;
	}

	private static OptionInstance<Boolean> toggle(String name, boolean value, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(KEY + name, tooltip(name), value, setter::accept);
	}

	private static <E extends Enum<E>> OptionInstance<E> cycle(String name, Class<E> type, E value, Consumer<E> setter) {
		E[] all = type.getEnumConstants();
		Codec<E> codec = Codec.INT.xmap(i -> all[Mth.clamp(i, 0, all.length - 1)], Enum::ordinal);
		return new OptionInstance<>(KEY + name, tooltip(name),
			(caption, v) -> Component.translatable(KEY + name + "." + v.name().toLowerCase(Locale.ROOT)),
			new OptionInstance.Enum<>(List.of(all), codec), value, setter::accept);
	}
}
