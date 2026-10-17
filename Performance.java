package dev.zxht.client.fx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

/** Performance mode: lowers the settings that cost the most FPS, and restores them when turned off. */
public final class Performance {
    private static boolean saved = false;
    private static int viewDistance, biomeBlend;
    private static ParticlesMode particles;
    private static CloudRenderMode clouds;
    private static boolean shadows, vsync;
    private static int maxFps;

    private Performance() {}

    public static void apply(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        if (on) {
            if (!saved) {
                viewDistance = o.getViewDistance().getValue();
                biomeBlend = o.getBiomeBlendRadius().getValue();
                particles = o.getParticles().getValue();
                clouds = o.getCloudRenderMode().getValue();
                shadows = o.getEntityShadows().getValue();
                vsync = o.getEnableVsync().getValue();
                maxFps = o.getMaxFps().getValue();
                saved = true;
            }
            o.getViewDistance().setValue(Math.min(viewDistance, 8));
            o.getBiomeBlendRadius().setValue(0);
            o.getParticles().setValue(ParticlesMode.MINIMAL);
            o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            o.getEntityShadows().setValue(false);
            o.getEnableVsync().setValue(false);
            o.getMaxFps().setValue(260); // 260 = unlimited
        } else if (saved) {
            o.getViewDistance().setValue(viewDistance);
            o.getBiomeBlendRadius().setValue(biomeBlend);
            o.getParticles().setValue(particles);
            o.getCloudRenderMode().setValue(clouds);
            o.getEntityShadows().setValue(shadows);
            o.getEnableVsync().setValue(vsync);
            o.getMaxFps().setValue(maxFps);
            saved = false;
        }
        o.write();
    }
}
