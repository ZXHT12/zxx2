package dev.zxht.client.mixin;

import dev.zxht.client.ui.ZxhtTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replaces the vanilla title screen with the ZXHT one. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen zxht$swapTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new ZxhtTitleScreen();
        }
        return screen;
    }
}
