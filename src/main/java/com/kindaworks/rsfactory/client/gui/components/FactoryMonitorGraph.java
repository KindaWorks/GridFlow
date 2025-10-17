package com.kindaworks.rsfactory.client.gui.components;

import com.kindaworks.rsfactory.client.gui.side_buttons.Granularity;
import com.kindaworks.rsfactory.client.gui.side_buttons.LineStyle;
import com.kindaworks.rsfactory.resource.ResourceGranularityKey;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import com.refinedmods.refinedstorage.common.support.tooltip.SmallTextClientTooltipComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class FactoryMonitorGraph {
    final int MINECRAFT_STYLE_VERTICAL_RESOLUTION = 1;
    final int GRAPH_HEIGHT = 80;
    final int GRAPH_WIDTH = 200;
    int graphLeft = 0;
    int graphBottom = 0;

    long graphMaxValue = 0;
    long graphMinValue = 0;
    int graphPointsAmt = 0;

    PlatformResourceKey itemKey;
    Granularity granularity;
    Map<ResourceGranularityKey, long[]> data;
    long[] incgen;
    long[] decgen;

    LocalDateTime dataTimeStamp;
    int selectedIndex = -1;

    public Vector2i getGuiXYFromGraphValue(Integer index, Long value) {
        int x = (int) ((GRAPH_WIDTH / Math.max(1, graphPointsAmt - 1f)) * index);
        if (graphMaxValue - graphMinValue == 0) return new Vector2i(graphLeft + x, graphBottom - (GRAPH_HEIGHT / 2));
        int y = (int) ((Math.max(value, 0f) / graphMaxValue) * GRAPH_HEIGHT);
        return new Vector2i(graphLeft + x, graphBottom - y);
    }

    public Vector2i getGraphValueFromGuiXY(double x, double y) {
        int index = (int) Math.floor(((x - graphLeft) * ((graphPointsAmt - 1f) / GRAPH_WIDTH)));
        int value = (int) Math.max(0, graphMaxValue * (graphBottom - y) / GRAPH_HEIGHT);
        return new Vector2i(index, value);
    }

    public void setGraphPos(int graphLeft, int graphBottom) {
        this.graphLeft = graphLeft;
        this.graphBottom = graphBottom;
    }

    public void setData(Map<ResourceGranularityKey, long[]> data, Granularity granularity) {
        ResourceGranularityKey rgk = data.keySet().stream().findFirst().orElse(null);
        if (rgk == null) return;
        this.itemKey = rgk.resourceKey();
        this.granularity = granularity;
        this.data = data;
        this.incgen = data.getOrDefault(new ResourceGranularityKey(itemKey, (short) +1, granularity.getTickAmount()), new long[0]);
        this.decgen = data.getOrDefault(new ResourceGranularityKey(itemKey, (short) -1, granularity.getTickAmount()), new long[0]);
        this.graphMaxValue = Math.max(Arrays.stream(incgen).max().orElse(0L), Arrays.stream(decgen).max().orElse(0L));
        this.graphMinValue = Math.max(Arrays.stream(incgen).min().orElse(0L), Arrays.stream(decgen).min().orElse(0L));
        this.graphPointsAmt = (int) Arrays.stream(incgen).count();

        this.dataTimeStamp = LocalDateTime.now();
    }

    public List<Vector2d> drawLine(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, int color, int thickness, LineStyle lineStyle) {
        // I've decided to go with .fill(), because it gives more Minecraft'y result than nice and straight GL-rendered lines.
        if (lineStyle == LineStyle.BLOCKY) {
            Vector2d p1 = new Vector2d(
                    x1,
                    Math.ceil(y1 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            Vector2d p2 = new Vector2d(
                    x2,
                    Math.floor(y2 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            guiGraphics.fill((int) p1.x, (int) p1.y, (int) p2.x, (int) (p2.y == p1.y ? p2.y + MINECRAFT_STYLE_VERTICAL_RESOLUTION : p2.y), color);
            return List.of(p1, p2);
        } else if (lineStyle == LineStyle.EXACT) {
            double ht = thickness / 2f;
            double rads = Math.atan2(y2 - y1, x2 - x1);
            double tx = Math.sin(rads) * ht;
            double ty = -Math.cos(rads) * ht;
            List<Vector2d> points = List.of(
                    new Vector2d((float) (x1 + tx), (float) (y1 + ty)),
                    new Vector2d((float) (x1 - tx), (float) (y1 - ty)),
                    new Vector2d((float) (x2 - tx), (float) (y2 - ty)),
                    new Vector2d((float) (x2 + tx), (float) (y2 + ty))
            );
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
            points.forEach(p -> vc.addVertex(matrix4f, (float) p.x, (float) p.y, 0f).setColor(color));
            guiGraphics.flush();

            // debug
//            guiGraphics.fill((int) (x1 - ht + 1), (int) (y1 - ht + 1), (int) (x1 + ht - 1), (int) (y1 + ht - 1), color);
            return points;
        }
        return null;
    }

    public void drawGradientBg(GuiGraphics guiGraphics, List<Vector2d> points, double graphBottom, int baseColor, int targetColor) {
        int baseR = ((baseColor & 0xff0000) >> 16);
        int baseG = ((baseColor & 0x00ff00) >> 8);
        int baseB = baseColor & 0x0000ff;
        int targetR = ((targetColor & 0xff0000) >> 16) - baseR;
        int targetG = ((targetColor & 0x00ff00) >> 8) - baseG;
        int targetB = (targetColor & 0x0000ff) - baseB;
        int maxIdx = points.size() - 1;
        IntStream.range(0, maxIdx)
                .forEach(i -> {
                    Vector2d a = points.get(i);
                    Vector2d b = points.get(i + 1);

                    int color = 0xff000000 + baseColor
                            + ((0x010000 * Math.round(((i + 1) * 1f / maxIdx) * targetR))
                            + (0x000100 * Math.round(((i + 1) * 1f / maxIdx) * targetG))
                            + (Math.round(((i + 1) * 1f / maxIdx) * targetB)));
                    int prevColor = 0xff000000 + baseColor
                            + ((0x010000 * Math.round((i * 1f / maxIdx) * targetR))
                            + (0x000100 * Math.round((i * 1f / maxIdx) * targetG))
                            + (Math.round((i * 1f / maxIdx) * targetB)));

                    Matrix4f matrix4f = guiGraphics.pose().last().pose();
                    VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
                    vc.addVertex(matrix4f, (float) b.x, (float) b.y, 0f).setColor(color);
                    vc.addVertex(matrix4f, (float) a.x, (float) a.y, 0f).setColor(prevColor);
                    vc.addVertex(matrix4f, (float) a.x, (float) graphBottom, 0f).setColor(prevColor);
                    vc.addVertex(matrix4f, (float) b.x, (float) graphBottom, 0f).setColor(color);
                    guiGraphics.flush();
                });
    }

    public boolean isInBounds(int mouseX, int mouseY) {
        return mouseY <= graphBottom && mouseY >= graphBottom - GRAPH_HEIGHT && mouseX >= graphLeft && mouseX <= graphLeft + GRAPH_WIDTH;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0)
            if (isInBounds((int) mouseX, (int) mouseY)) {
                this.selectedIndex = getGraphValueFromGuiXY(mouseX, mouseY).x;
                return true;
            }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.selectedIndex = -1;
            return true;
        }
        return false;
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isInBounds(mouseX, mouseY) && (selectedIndex == -1)) return;
        int index = getGraphValueFromGuiXY(mouseX, mouseY).x;
        if ((index < 0 || index >= graphPointsAmt) && selectedIndex == -1) return; // just additional precaution

        int indexFrom = selectedIndex != -1 ? Math.min(Math.max(0, index), selectedIndex) : index;
        int indexTo = selectedIndex != -1 ? Math.max(Math.min(index, graphPointsAmt - 1) + 1, selectedIndex) : index + 1;

        int graphX1 = getGuiXYFromGraphValue(indexFrom, 0L).x;
        int graphX2 = getGuiXYFromGraphValue(indexTo, 0L).x;
        guiGraphics.fill(graphX1, graphBottom, graphX2, graphBottom - GRAPH_HEIGHT, 250, 0x66ffffff);

        LocalDateTime indexDate = dataTimeStamp.minus(graphPointsAmt - indexFrom, granularity.getChronoUnit());
        LocalDateTime nextIndexDate = dataTimeStamp.minus(graphPointsAmt - (indexTo), granularity.getChronoUnit());
        long incvalue = Arrays.stream(incgen).skip(indexFrom).limit(indexTo - indexFrom).sum();
        long decvalue = Arrays.stream(decgen).skip(indexFrom).limit(indexTo - indexFrom).sum();

        ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(itemKey.getClass());
        List<ClientTooltipComponent> lines = new ArrayList<>();

        lines.add(new ClientTextTooltip(resourceRendering.getDisplayName(itemKey).getVisualOrderText()));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(
                "Throughput from " +
                        indexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                        " to " +
                        nextIndexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        )));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(String.format("Production:  +%,d", incvalue)).withColor(0xff00ff00)));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(String.format("Consumption: -%,d", decvalue)).withColor(0xffff0000)));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(String.format("Net: %,d", incvalue - decvalue)).withColor(0xff66ddff)));
        Platform.INSTANCE.renderTooltip(guiGraphics, lines, mouseX, mouseY);
    }
}
