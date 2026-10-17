package dev.zxht.client.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.session.Session;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** The ZXHT main menu: glass buttons, animated logo, small skin preview on the left. */
public class ZxhtTitleScreen extends ZxhtScreen {
    private final List<ButtonWidget> menu = new ArrayList<>();
    private long openMs = -1L;
    private Supplier<SkinTextures> skin;

    public ZxhtTitleScreen() {
        super(Text.literal("ZXHT Client"), null);
    }

    @Override
    protected void init() {
        menu.clear();

        Session session = this.client.getSession();
        UUID id = session.getUuidOrNull();
        if (id == null) {
            id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getUsername()).getBytes(StandardCharsets.UTF_8));
        }
        this.skin = this.client.getSkinProvider().supplySkinTextures(new GameProfile(id, session.getUsername()), false);

        add("Singleplayer", b -> this.client.setScreen(new SelectWorldScreen(this)));
        add("Multiplayer", b -> this.client.setScreen(new MultiplayerScreen(this)));
        add("ZXHT Settings", b -> this.client.setScreen(new ZxhtSettingsScreen(this)));
        add("Accounts", b -> this.client.setScreen(new AccountScreen(this)));
        add("Minecraft Options", b -> this.client.setScreen(new OptionsScreen(this, this.client.options)));
        add("Quit Game", b -> this.client.scheduleStop());
    }

    private int blockTop() {
        int blockH = 62 + menu.size() * 24;
        return Math.max(12, (this.height - blockH) / 2);
    }

    private void add(String label, ButtonWidget.PressAction action) {
        int i = menu.size();
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), action)
                .dimensions(this.width / 2 - 100, 0, 200, 20)
                .build();
        menu.add(b);
        this.addDrawableChild(b);
    }

    @Override
    protected void animate(float fade) {
        long t = Util.getMeasuringTimeMs();
        if (openMs < 0) openMs = t;
        int top = blockTop() + 62;
        for (int i = 0; i < menu.size(); i++) {
            ButtonWidget b = menu.get(i);
            float p = Draw.ease(MathHelper.clamp((t - openMs - 150f - i * 70f) / 420f, 0f, 1f));
            b.setX(this.width / 2 - 100 + Math.round((1f - p) * 36f));
            b.setY(top + i * 24);
            b.setAlpha(p * fade);
        }
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        float fade = fade();
        long ms = Util.getMeasuringTimeMs();
        float time = ms / 1000f;
        int cx = this.width / 2;
        int top = blockTop();

        // ---- logo: ZXHT (big, shimmering) ----
        String word = "ZXHT";
        float scale = 3.2f;
        int spacing = 2;
        int totalW = 0;
        for (int i = 0; i < word.length(); i++) totalW += this.textRenderer.getWidth(word.substring(i, i + 1)) + spacing;
        totalW -= spacing;
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(cx - totalW * scale / 2f, top);
        c.getMatrices().scale(scale, scale);
        int x = 0;
        for (int i = 0; i < word.length(); i++) {
            String ch = word.substring(i, i + 1);
            float shimmer = 0.78f + 0.22f * MathHelper.sin(time * 2.0f - i * 0.7f);
            int level = (int) (255 * shimmer);
            c.drawText(this.textRenderer, Text.literal(ch).formatted(Formatting.BOLD), x, 0,
                    Draw.argb((int) (255 * fade), level, level, level), false);
            x += this.textRenderer.getWidth(ch) + spacing;
        }
        c.getMatrices().popMatrix();

        // ---- logo: CLIENT (small, spaced) ----
        String sub = "C L I E N T";
        c.drawText(this.textRenderer, Text.literal(sub), cx - this.textRenderer.getWidth(sub) / 2, top + 34,
                Draw.gray(190, (int) (255 * fade)), false);

        // animated underline
        float line = 0.55f + 0.45f * MathHelper.sin(time * 1.4f);
        int lw = (int) (110 * line * fade);
        c.fill(cx - lw / 2, top + 48, cx + lw / 2, top + 49, Draw.gray(255, (int) (200 * fade)));

        // ---- skin card on the left ----
        drawSkinCard(c, fade, time);

        // ---- footer ----
        String footer = "ZXHT Client 1.0  |  Minecraft 1.21.11  |  Right Shift: settings";
        c.drawText(this.textRenderer, Text.literal(footer), 8, this.height - 12, Draw.gray(150, (int) (200 * fade)), false);
    }

    private void drawSkinCard(DrawContext c, float fade, float time) {
        int cardW = 86;
        int cardH = 136;
        int cardX = 18;
        int cardY = this.height / 2 - cardH / 2;
        Draw.glass(c, cardX, cardY, cardW, cardH, 6, fade);

        Identifier tex = skin.get().body().texturePath();
        int s = 3; // pixel scale: whole figure is 48 x 96
        int cx = cardX + cardW / 2;
        int y = cardY + 10 + Math.round(MathHelper.sin(time * 1.6f)); // gentle idle bob
        int col = Draw.argb((int) (255 * fade), 255, 255, 255);

        // base layer then overlay layer (hat, jacket, sleeves, pants)
        for (int layer = 0; layer < 2; layer++) {
            int ov = layer == 0 ? 0 : 1;
            part(c, tex, cx - 4 * s, y,           ov == 0 ? 8 : 40, 8,           8, 8, s, col);   // head
            part(c, tex, cx - 4 * s, y + 8 * s,   20,               ov == 0 ? 20 : 36, 8, 12, s, col); // body
            part(c, tex, cx - 8 * s, y + 8 * s,   44,               ov == 0 ? 20 : 36, 4, 12, s, col); // right arm
            part(c, tex, cx + 4 * s, y + 8 * s,   ov == 0 ? 36 : 52, 52,          4, 12, s, col); // left arm
            part(c, tex, cx - 4 * s, y + 20 * s,  4,                ov == 0 ? 20 : 36, 4, 12, s, col); // right leg
            part(c, tex, cx,         y + 20 * s,  ov == 0 ? 20 : 4, 52,            4, 12, s, col); // left leg
        }

        String name = this.client.getSession().getUsername();
        c.drawText(this.textRenderer, Text.literal(name), cx - this.textRenderer.getWidth(name) / 2,
                cardY + cardH - 14, Draw.gray(235, (int) (255 * fade)), false);
    }

    private void part(DrawContext c, Identifier tex, int dx, int dy, int u, int v, int w, int h, int scale, int color) {
        c.drawTexture(RenderPipelines.GUI_TEXTURED, tex, dx, dy, u, v, w * scale, h * scale, w, h, 64, 64, color);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
