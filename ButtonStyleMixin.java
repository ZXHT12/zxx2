package dev.zxht.client.mixin;

import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.ui.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-skins every normal button in the game (menus, pause screen, options...) as a
 * transparent black/white glass button with a smooth hover animation.
 */
@Mixin(PressableWidget.class)
public abstract class ButtonStyleMixin extends ClickableWidget {
    @Unique private float zxht$hover = 0f;
    @Unique private long zxht$lastNanos = 0L;

    protected ButtonStyleMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void zxht$renderGlass(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof ButtonWidget)) return;
        if (!ZxhtConfig.get().styledButtons) return;
        if (getMessage().getString().isEmpty()) return; // leave icon-only buttons alone

        long now = System.nanoTime();
        float dt = zxht$lastNanos == 0L ? 0f : (now - zxht$lastNanos) / 1_000_000_000f;
        zxht$lastNanos = now;

        boolean hovered = this.isHovered() && this.active;
        float target = hovered ? 1f : 0f;
        zxht$hover += (target - zxht$hover) * Math.min(1f, dt * 14f);

        float a = this.alpha;
        float h = Draw.ease(zxht$hover);
        int grow = Math.round(h * 1.5f);

        int x = getX() - grow;
        int y = getY() - grow;
        int w = getWidth() + grow * 2;
        int ht = getHeight() + grow * 2;

        // fill: dark glass -> frosted white on hover
        int fr = (int) Draw.lerp(12, 255, h);
        int fa = (int) (Draw.lerp(100, 52, h) * a);
        Draw.roundRect(context, x, y, w, ht, 4, Draw.argb(fa, fr, fr, fr));

        // border brightens on hover
        int ba = (int) (Draw.lerp(55, 210, h) * a);
        Draw.roundOutline(context, x, y, w, ht, 4, Draw.argb(ba, 255, 255, 255));

        // label
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int level = this.active ? 255 : 130;
        int textAlpha = Math.max(4, (int) (255 * a));
        Text msg = getMessage();
        int tx = getX() + (getWidth() - tr.getWidth(msg)) / 2;
        int ty = getY() + (getHeight() - 8) / 2;
        context.drawText(tr, msg, tx, ty, Draw.argb(textAlpha, level, level, level), false);

        ci.cancel();
    }
}
