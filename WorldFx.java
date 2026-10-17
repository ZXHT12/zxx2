package dev.zxht.client.fx;

import dev.zxht.client.config.ZxhtConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything drawn in the 3D world: black wings, rings around players, and the ZXHT logo
 * that appears under your feet when you walk or jump. Visual only.
 */
public final class WorldFx {
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final class Mark {
        final double x, y, z;
        final float yaw;
        final long born;
        final boolean jump;

        Mark(double x, double y, double z, float yaw, boolean jump) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.born = Util.getMeasuringTimeMs();
            this.jump = jump;
        }
    }

    private static final List<Mark> MARKS = new ArrayList<>();
    private static Vec3d lastStep = null;
    private static boolean wasOnGround = true;

    private WorldFx() {}

    // ------------------------------------------------------------ tick (spawns marks)

    public static void tick(MinecraftClient mc) {
        ZxhtConfig cfg = ZxhtConfig.get();
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            MARKS.clear();
            lastStep = null;
            return;
        }
        if (cfg.footprints) {
            Vec3d pos = p.getEntityPos();
            boolean ground = p.isOnGround();

            if (wasOnGround && !ground && p.getVelocity().y > 0.1) {
                MARKS.add(new Mark(pos.x, pos.y, pos.z, p.getBodyYaw(), true));
            } else if (ground) {
                if (lastStep == null) lastStep = pos;
                if (pos.squaredDistanceTo(lastStep) > 1.2 * 1.2) {
                    MARKS.add(new Mark(pos.x, pos.y, pos.z, p.getBodyYaw(), false));
                    lastStep = pos;
                }
            }
            wasOnGround = ground;
        }
        long now = Util.getMeasuringTimeMs();
        MARKS.removeIf(m -> now - m.born > 1600L);
        if (MARKS.size() > 40) MARKS.subList(0, MARKS.size() - 40).clear();
    }

    // ------------------------------------------------------------ render

    public static void render(MinecraftClient mc, MatrixStack matrices, VertexConsumerProvider consumers,
                              Vec3d camera, float tickProgress) {
        ZxhtConfig cfg = ZxhtConfig.get();
        if (mc.world == null || mc.player == null) return;
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();

        VertexConsumer quads = consumers.getBuffer(RenderLayers.debugQuads());
        float time = Util.getMeasuringTimeMs() / 1000f;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player.isSpectator() || player.isInvisible()) continue;
            boolean self = player == mc.player;
            Vec3d pos = player.getLerpedPos(tickProgress);

            if (cfg.rings && (!self || cfg.ownRing)) {
                drawRing(matrices, quads, pos, camera, time, player.getId());
            }
            if (cfg.wings && !(self && firstPerson)) {
                drawWings(matrices, quads, player, pos, camera, time);
            }
        }

        if (cfg.footprints) {
            drawMarks(mc, matrices, consumers, camera);
        }
    }

    // ------------------------------------------------------------ rings

    private static void drawRing(MatrixStack m, VertexConsumer q, Vec3d pos, Vec3d cam, float time, int id) {
        float pulse = 0.5f + 0.5f * MathHelper.sin(time * 2.2f + id);
        float radius = 0.78f + 0.06f * pulse;
        float width = 0.07f;
        int segments = 48;
        int alpha = (int) (150 + 70 * pulse);

        m.push();
        m.translate(pos.x - cam.x, pos.y - cam.y + 0.02, pos.z - cam.z);
        Matrix4f mat = m.peek().getPositionMatrix();
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (i * Math.PI * 2 / segments);
            float a1 = (float) ((i + 1) * Math.PI * 2 / segments);
            // brightness sweeps around the ring so it looks like it is gliding
            float sweep = 0.55f + 0.45f * MathHelper.sin(a0 * 2f - time * 2.5f);
            int c = argb((int) (alpha * sweep), 255, 255, 255);
            float r0 = radius, r1 = radius + width;
            quad(q, mat,
                    MathHelper.cos(a0) * r0, 0, MathHelper.sin(a0) * r0,
                    MathHelper.cos(a0) * r1, 0, MathHelper.sin(a0) * r1,
                    MathHelper.cos(a1) * r1, 0, MathHelper.sin(a1) * r1,
                    MathHelper.cos(a1) * r0, 0, MathHelper.sin(a1) * r0,
                    c, c, c, c);
        }
        m.pop();
    }

    // ------------------------------------------------------------ wings

    private static void drawWings(MatrixStack m, VertexConsumer q, PlayerEntity player, Vec3d pos, Vec3d cam, float time) {
        double speed = player.getVelocity().horizontalLength();
        boolean airborne = !player.isOnGround();
        // idle: slow breathing; moving/airborne: faster, wider flap
        float freq = airborne ? 5.5f : (speed > 0.05 ? 3.2f : 1.4f);
        float amp = airborne ? 24f : (speed > 0.05 ? 14f : 6f);
        float flap = 18f + MathHelper.sin(time * freq + player.getId()) * amp;
        float height = player.isSneaking() ? 1.05f : 1.38f;

        m.push();
        m.translate(pos.x - cam.x, pos.y - cam.y + height, pos.z - cam.z);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.getBodyYaw()));
        if (player.isSneaking()) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(25f));
        m.translate(0, 0, -0.17); // just behind the back (player faces +Z at yaw 0)

        for (int side = -1; side <= 1; side += 2) {
            m.push();
            m.translate(side * 0.10, 0, 0);
            // swing the whole wing back/forth around the vertical axis
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-side * flap));
            drawOneWing(m.peek().getPositionMatrix(), q, side);
            m.pop();
        }
        m.pop();
    }

    /** One wing = a fan of tapered black feathers in the local XY plane. */
    private static void drawOneWing(Matrix4f mat, VertexConsumer q, int side) {
        final int feathers = 7;
        for (int i = 0; i < feathers; i++) {
            float t = i / (float) (feathers - 1);
            // angle from horizontal: top feather points up, bottom feather points down-out
            float angle = (float) Math.toRadians(70 - t * 125);
            float len = 1.15f - 0.55f * Math.abs(t - 0.25f) - 0.1f * t;
            float w = 0.115f;

            float dx = side * MathHelper.cos(angle);
            float dy = MathHelper.sin(angle);
            // perpendicular in the plane
            float px = -dy * side;
            float py = dx * side;

            float rx0 = px * w * 0.5f, ry0 = py * w * 0.5f;
            float tipX = dx * len, tipY = dy * len;
            float tx = px * w * 0.18f, ty = py * w * 0.18f;

            int root = argb(240, 6, 6, 8);
            int tip = argb(150, 20, 20, 24);

            // both windings so it is visible from either side
            quad(q, mat,
                    -rx0, -ry0, 0, rx0, ry0, 0, tipX + tx, tipY + ty, 0, tipX - tx, tipY - ty, 0,
                    root, root, tip, tip);
            quad(q, mat,
                    rx0, ry0, 0, -rx0, -ry0, 0, tipX - tx, tipY - ty, 0, tipX + tx, tipY + ty, 0,
                    root, root, tip, tip);
        }
    }

    // ------------------------------------------------------------ logo under your feet

    private static void drawMarks(MinecraftClient mc, MatrixStack m, VertexConsumerProvider consumers, Vec3d cam) {
        TextRenderer tr = mc.textRenderer;
        long now = Util.getMeasuringTimeMs();
        for (Mark mark : MARKS) {
            float age = (now - mark.born) / 1600f;
            float fadeIn = Math.min(1f, age * 8f);
            float alpha = fadeIn * (1f - age) * (1f - age);
            int a = (int) (230 * alpha);
            if (a < 6) continue;

            float scale = mark.jump ? 0.05f : 0.034f;
            float grow = mark.jump ? 1f + age * 0.6f : 1f;
            Text text = Text.literal("ZXHT");

            m.push();
            m.translate(mark.x - cam.x, mark.y - cam.y + 0.03, mark.z - cam.z);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mark.yaw));
            m.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90f)); // lie flat, readable from above
            m.scale(scale * grow, -scale * grow, scale * grow);
            Matrix4f mat = m.peek().getPositionMatrix();
            float x = -tr.getWidth(text) / 2f;
            tr.draw(text, x, -4f, argb(a, 255, 255, 255), false, mat, consumers,
                    TextRenderer.TextLayerType.NORMAL, 0, FULL_BRIGHT);
            m.pop();
        }
    }

    // ------------------------------------------------------------ helpers

    private static int argb(int a, int r, int g, int b) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

    private static void quad(VertexConsumer q, Matrix4f mat,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             int c0, int c1, int c2, int c3) {
        q.vertex(mat, x0, y0, z0).color(c0);
        q.vertex(mat, x1, y1, z1).color(c1);
        q.vertex(mat, x2, y2, z2).color(c2);
        q.vertex(mat, x3, y3, z3).color(c3);
    }
}
