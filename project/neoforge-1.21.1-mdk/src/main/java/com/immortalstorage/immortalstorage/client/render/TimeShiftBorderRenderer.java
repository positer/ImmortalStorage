package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.client.TimeShiftClientCache;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftTiers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

/**
 * Draws the synced time-shift record of the current dimension: one box per
 * shifted block and its multiplier written on all six faces.
 *
 * <p>The box is drawn with {@code RenderType.lines()} so terrain still hides it,
 * and the labels use {@code Font.DisplayMode.NORMAL} so they are occluded the
 * same way.  Labels, colours and the box are precomputed constants, so the loop
 * allocates no components or formatted text of its own.</p>
 *
 * <p>Lines and text share the buffer source's single shared buffer, so each box
 * is flushed before its labels claim that buffer, and the frame ends with a
 * final {@code endBatch()}.  Holding the box's vertex consumer across a text
 * draw would write into released memory.</p>
 */
public final class TimeShiftBorderRenderer {
    /** Contract view distance, squared for the distance test. */
    private static final double MAX_DISTANCE_SQUARED = 64.0D * 64.0D;
    /** Face centre: half a block plus clearance, so the label never z-fights the face. */
    private static final double FACE_OFFSET = 0.51D;
    /** One block in block-local space, inflated like every other outline in this mod. */
    private static final AABB BLOCK_BOX = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D).inflate(0.002D);
    /** Same scale vanilla uses for world-space name tags. */
    private static final float TEXT_SCALE = 0.025F;
    private static final float ALPHA = 0.9F;
    private static final float BOOST_RED = 1.0F;
    private static final float BOOST_GREEN = 0.15F;
    private static final float BOOST_BLUE = 0.1F;
    private static final float PERMANENT_RED = 0.0F;
    private static final float PERMANENT_GREEN = 0.85F;
    private static final float PERMANENT_BLUE = 1.0F;
    private static final int BOOST_COLOR = argb(BOOST_RED, BOOST_GREEN, BOOST_BLUE);
    private static final int PERMANENT_COLOR = argb(PERMANENT_RED, PERMANENT_GREEN, PERMANENT_BLUE);
    /** The boost multiplier is a documented constant, not a per-client config read. */
    private static final int BOOST_MULTIPLIER = TimeShiftTiers.boostMultiplier(TimeShiftTiers.DEFAULT_BOOST_LOG2);
    /** The six faces as outward normal plus the rotation that aims local +Z along it. */
    private static final Face[] FACES = {
        new Face(0.0D, 1.0D, 0.0D, Axis.XP.rotationDegrees(-90.0F)),
        new Face(0.0D, -1.0D, 0.0D, Axis.XP.rotationDegrees(90.0F)),
        new Face(0.0D, 0.0D, -1.0D, Axis.YP.rotationDegrees(180.0F)),
        new Face(0.0D, 0.0D, 1.0D, new Quaternionf()),
        new Face(-1.0D, 0.0D, 0.0D, Axis.YP.rotationDegrees(-90.0F)),
        new Face(1.0D, 0.0D, 0.0D, Axis.YP.rotationDegrees(90.0F))
    };
    private static final Style BOOST_STYLE;
    private static final Style[] TIER_STYLES = new Style[TimeShiftTiers.COUNT];

    static {
        BOOST_STYLE = new Style(label(BOOST_MULTIPLIER), BOOST_COLOR,
                BOOST_RED, BOOST_GREEN, BOOST_BLUE);
        for (int tier = 0; tier < TimeShiftTiers.COUNT; tier++) {
            TIER_STYLES[tier] = new Style(label(TimeShiftTiers.permanentMultiplier(tier)), PERMANENT_COLOR,
                    PERMANENT_RED, PERMANENT_GREEN, PERMANENT_BLUE);
        }
    }

    private TimeShiftBorderRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        List<TimeShiftClientCache.Entry> records = TimeShiftClientCache.entries(minecraft.level.dimension());
        if (records.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        long now = System.currentTimeMillis();
        for (TimeShiftClientCache.Entry record : records) {
            BlockPos pos = record.pos();
            if (minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                    > MAX_DISTANCE_SQUARED) {
                continue;
            }
            boolean boosted = record.boosted() && now < record.boostEndEpochMillis();
            Style style = boosted ? BOOST_STYLE : TIER_STYLES[TimeShiftTiers.clampTier(record.tier())];
            // Both lines and text live in the buffer source's single shared
            // buffer, so the box is flushed before the labels claim it.  Holding
            // that VertexConsumer across a text draw would alias released memory.
            poses.pushPose();
            poses.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            LevelRenderer.renderLineBox(poses, buffers.getBuffer(RenderType.lines()), BLOCK_BOX,
                    style.red(), style.green(), style.blue(), ALPHA);
            poses.popPose();
            buffers.endBatch(RenderType.lines());
            drawLabels(poses, font, buffers, pos, camera, style);
        }
        buffers.endBatch();
    }

    private static void drawLabels(PoseStack poses, Font font, MultiBufferSource buffers,
                                   BlockPos pos, Vec3 camera, Style style) {
        float halfWidth = font.width(style.label()) / 2.0F;
        poses.pushPose();
        poses.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        for (Face face : FACES) {
            poses.pushPose();
            poses.translate(0.5D + face.x() * FACE_OFFSET,
                    0.5D + face.y() * FACE_OFFSET, 0.5D + face.z() * FACE_OFFSET);
            poses.mulPose(face.rotation());
            poses.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
            font.drawInBatch(style.label(), -halfWidth, -4.0F, style.color(), false,
                    poses.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            poses.popPose();
        }
        poses.popPose();
    }

    /**
     * Renders {@code multiplier} as a literal label, built once per style.
     *
     * <p>A {@link Component} is used instead of {@code FormattedCharSequence} because 26.1.2's
     * {@code Font#drawInBatch} overload set no longer takes the pre-laid-out sequence, while the
     * {@code Component} overload exists on both generations.</p>
     */
    private static Component label(int multiplier) {
        return Component.literal(TimeShiftTiers.label(multiplier));
    }

    private static int argb(float red, float green, float blue) {
        return ((int) (ALPHA * 255.0F) << 24) | ((int) (red * 255.0F) << 16)
                | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
    }

    private record Face(double x, double y, double z, Quaternionf rotation) {}

    private record Style(Component label, int color, float red, float green, float blue) {}
}
