package dev.zxht.client.ui;

import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.fx.Performance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** All ZXHT toggles in one glass panel. Saved instantly. */
public class ZxhtSettingsScreen extends ZxhtScreen {
    private static final int COL_W = 150;
    private static final int ROW_H = 24;

    public ZxhtSettingsScreen(Screen parent) {
        super(Text.literal("ZXHT Settings"), parent);
    }

    @Override
    protected void init() {
        ZxhtConfig c = ZxhtConfig.get();
        int left = this.width / 2 - COL_W - 4;
        int right = this.width / 2 + 4;
        int top = Math.max(34, this.height / 2 - 100);

        // left column: visuals + sounds
        int y = top;
        toggle(left, y, "Black Wings", () -> c.wings, v -> c.wings = v); y += ROW_H;
        toggle(left, y, "Step/Jump Logo", () -> c.footprints, v -> c.footprints = v); y += ROW_H;
        toggle(left, y, "Player Rings", () -> c.rings, v -> c.rings = v); y += ROW_H;
        toggle(left, y, "Own Ring", () -> c.ownRing, v -> c.ownRing = v); y += ROW_H;
        toggle(left, y, "Hit Sound", () -> c.hitSound, v -> c.hitSound = v); y += ROW_H;
        toggle(left, y, "Kill Sound", () -> c.killSound, v -> c.killSound = v); y += ROW_H;
        toggle(left, y, "Click Sound", () -> c.clickSound, v -> c.clickSound = v);

        // right column: HUD + performance + UI
        y = top;
        toggle(right, y, "HUD Panel", () -> c.hud, v -> c.hud = v); y += ROW_H;
        toggle(right, y, "Show FPS", () -> c.showFps, v -> c.showFps = v); y += ROW_H;
        toggle(right, y, "Show CPS", () -> c.showCps, v -> c.showCps = v); y += ROW_H;
        toggle(right, y, "Show Ping", () -> c.showPing, v -> c.showPing = v); y += ROW_H;
        toggle(right, y, "Glass Buttons", () -> c.styledButtons, v -> c.styledButtons = v); y += ROW_H;
        toggle(right, y, "Performance Mode", () -> c.performanceMode, v -> {
            c.performanceMode = v;
            Performance.apply(v);
        });

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                .dimensions(this.width / 2 - 60, top + ROW_H * 7 + 10, 120, 20).build());
    }

    private void toggle(int x, int y, String name, BooleanSupplier get, Consumer<Boolean> set) {
        ButtonWidget button = ButtonWidget.builder(label(name, get.getAsBoolean()), b -> {
            boolean value = !get.getAsBoolean();
            set.accept(value);
            b.setMessage(label(name, value));
            ZxhtConfig.save();
        }).dimensions(x, y, COL_W, 20).build();
        this.addDrawableChild(button);
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": ")
                .append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.WHITE : Formatting.GRAY));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        float f = fade();
        String title = "Z X H T   S E T T I N G S";
        c.drawText(this.textRenderer, Text.literal(title), this.width / 2 - this.textRenderer.getWidth(title) / 2,
                Math.max(34, this.height / 2 - 100) - 18, Draw.gray(255, (int) (255 * f)), false);
        String hint = "Performance Mode lowers render distance, particles, shadows and clouds and uncaps FPS.";
        c.drawText(this.textRenderer, Text.literal(hint), this.width / 2 - this.textRenderer.getWidth(hint) / 2,
                this.height - 16, Draw.gray(150, (int) (220 * f)), false);
    }
}
