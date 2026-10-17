package dev.zxht.client;

import dev.zxht.client.account.AccountManager;
import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.fx.CpsTracker;
import dev.zxht.client.fx.Hud;
import dev.zxht.client.fx.Performance;
import dev.zxht.client.fx.SoundFx;
import dev.zxht.client.fx.WorldFx;
import dev.zxht.client.ui.ZxhtSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public class ZxhtClient implements ClientModInitializer {
    public static final String MOD_ID = "zxht";

    private static KeyBinding settingsKey;
    private static PlayerEntity lastTarget;
    private static long lastHitMs;

    @Override
    public void onInitializeClient() {
        ZxhtConfig.load();
        AccountManager.load();
        AccountManager.restoreLast();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        settingsKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.zxht.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        // HUD panel
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "hud"), Hud::render);

        // wings / rings / logo under feet
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            WorldFx.render(mc, ctx.matrices(), ctx.consumers(),
                    ctx.worldState().cameraRenderState.pos,
                    mc.getRenderTickCounter().getTickProgress(false));
        });

        // hit sound (calm ding) when you attack a player
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof PlayerEntity target && ZxhtConfig.get().hitSound) {
                SoundFx.hit();
                lastTarget = target;
                lastHitMs = Util.getMeasuringTimeMs();
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (settingsKey.wasPressed()) {
                mc.setScreen(new ZxhtSettingsScreen(mc.currentScreen));
            }
            CpsTracker.poll(mc.options.attackKey.isPressed() && mc.currentScreen == null);
            WorldFx.tick(mc);
            checkKill();
        });

        // re-apply performance mode on launch if it was left on
        if (ZxhtConfig.get().performanceMode) {
            ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
                private boolean done = false;

                @Override
                public void onEndTick(MinecraftClient mc) {
                    if (!done && mc.options != null) {
                        Performance.apply(true);
                        done = true;
                    }
                }
            });
        }
    }

    /** Plays the kill "ting" when the player you recently hit dies. */
    private static void checkKill() {
        if (lastTarget == null) return;
        if (Util.getMeasuringTimeMs() - lastHitMs > 4000L) {
            lastTarget = null;
            return;
        }
        LivingEntity t = lastTarget;
        if (t.isDead() || t.getHealth() <= 0f) {
            if (ZxhtConfig.get().killSound) SoundFx.kill();
            lastTarget = null;
        }
    }
}
