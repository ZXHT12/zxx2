package dev.zxht.client.mixin;

import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.fx.SoundFx;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swaps the harsh vanilla button click for a calm chime. */
@Mixin(ClickableWidget.class)
public abstract class ClickSoundMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void zxht$calmClick(SoundManager soundManager, CallbackInfo ci) {
        if (ZxhtConfig.get().clickSound) {
            SoundFx.click();
            ci.cancel();
        }
    }
}
