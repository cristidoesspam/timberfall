package dev.timberfall.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.timberfall.Timberfall;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TimberConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static TimberSettings current = new TimberSettings();

	private TimberConfig() {}

	public static TimberSettings get() {
		return current;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("timberfall.json");
	}

	public static void load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				TimberSettings loaded = GSON.fromJson(reader, TimberSettings.class);
				if (loaded != null) {
					current = loaded.sanitize();
				}
			} catch (Exception e) {
				Timberfall.LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		save();
	}

	public static void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(current, writer);
			}
		} catch (IOException e) {
			Timberfall.LOGGER.warn("Could not write {}", file, e);
		}
	}
}
