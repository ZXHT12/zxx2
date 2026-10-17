package dev.zxht.client.fx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Calm chime sounds built from vanilla sound events (no audio files needed). */
public final class SoundFx {
    private SoundFx() {}

    private static void play(SoundEvent event, float pitch, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
    }

    /** Soft ding for button clicks. */
    public static void click() {
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.7f, 0.45f);
    }

    /** Soft ding when you hit a player. */
    public static void hit() {
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.35f, 0.4f);
    }

    /** A smooth two-layer "ting" when you kill a player. */
    public static void kill() {
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.55f);
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
    }
}
