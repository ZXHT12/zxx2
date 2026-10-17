package dev.zxht.client.fx;

import dev.zxht.client.config.ZxhtConfig;
import dev.zxht.client.ui.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/** Small glass info panel in the top-left corner. */
public final class Hud {
    private Hud() {}

    public static void render(DrawContext c, RenderTickCounter tick) {
        ZxhtConfig cfg = ZxhtConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!cfg.hud || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        List<String> lines = new ArrayList<>();
        if (cfg.showFps) lines.add("FPS  " + mc.getCurrentFps());
        if (cfg.showCps) lines.add("CPS  " + CpsTracker.cps());
        if (cfg.showPing && mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) lines.add("PING " + e.getLatency() + " ms");
        }

        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth("ZXHT CLIENT");
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int x = 6, y = 6, pad = 5;
        int h = 10 + lines.size() * 10 + pad * 2 - (lines.isEmpty() ? 0 : 0);

        Draw.glass(c, x, y, w + pad * 2, h, 4, 1f);
        c.drawText(tr, "ZXHT CLIENT", x + pad, y + pad, Draw.gray(255, 255), false);
        int ly = y + pad + 11;
        for (String l : lines) {
            c.drawText(tr, l, x + pad, ly, Draw.gray(210, 255), false);
            ly += 10;
        }
    }
}
