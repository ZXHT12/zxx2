package dev.zxht.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/** Base class: dark translucent background over the (blurred) game/panorama, plus a smooth fade-in. */
public abstract class ZxhtScreen extends Screen {
    protected final Screen parent;
    private long openTime = -1L;

    protected ZxhtScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** 0..1, eased, over 350 ms after the screen first renders. */
    protected float fade() {
        if (openTime < 0) openTime = Util.getMeasuringTimeMs();
        return Draw.ease(MathHelper.clamp((Util.getMeasuringTimeMs() - openTime) / 350f, 0f, 1f));
    }

    /** Called every frame before rendering; default fades all widgets in together. */
    protected void animate(float fade) {
        for (Element e : children()) {
            if (e instanceof ClickableWidget w) w.setAlpha(fade);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta); // vanilla panorama/blur
        context.fill(0, 0, this.width, this.height, Draw.argb((int) (150 * fade()), 0, 0, 0));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animate(fade());
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
