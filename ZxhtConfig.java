package dev.zxht.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** All ZXHT settings, saved to config/zxht-client.json. */
public class ZxhtConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zxht-client.json");
    private static ZxhtConfig instance = new ZxhtConfig();

    // UI
    public boolean styledButtons = true;

    // Visuals
    public boolean wings = true;
    public boolean footprints = true;
    public boolean rings = true;
    public boolean ownRing = false;

    // Sounds
    public boolean hitSound = true;
    public boolean killSound = true;
    public boolean clickSound = true;

    // HUD
    public boolean hud = true;
    public boolean showFps = true;
    public boolean showCps = true;
    public boolean showPing = true;

    // Performance
    public boolean performanceMode = false;

    // Microsoft login: the Azure "Application (client) ID" of YOUR registered app (see README).
    public String msaClientId = "";

    public static ZxhtConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ZxhtConfig loaded = GSON.fromJson(Files.readString(FILE), ZxhtConfig.class);
                if (loaded != null) instance = loaded;
            }
        } catch (Exception e) {
            System.err.println("[ZXHT] Could not read config, using defaults: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[ZXHT] Could not save config: " + e);
        }
    }
}
