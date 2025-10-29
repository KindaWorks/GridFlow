package com.kindaworks.gridflow.client.gui.components;

import com.kindaworks.gridflow.client.gui.side_buttons.Granularity;
import com.kindaworks.gridflow.client.gui.side_buttons.LineStyle;
import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
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
import java.util.stream.Stream;

/**
 * Stores the graph data and displays the graph.
 */
public class FlowScopeGraph {
    // #region Properties
    private final int MINECRAFT_STYLE_VERTICAL_RESOLUTION = 1; // config value?
    private final int HEIGHT = 80;
    private final int WIDTH = 200;

    private final int PRODUCTION_GRAPH_COLOR = 0xff4b7f52;
    private final int CONSUMPTION_GRAPH_COLOR = 0xffad343e;
    private final int NET_GRAPH_COLOR = 0xff66ddff;
    private final int GRAPH_GRADIENT_FROM = 0x00313f;
    private final int GRAPH_GRADIENT_TO = 0x66ddff;

    public LineStyle lineStyle = LineStyle.BLOCKY;

    private int left = 0;
    private int bottom = 0;
    private long maxValue = 0;
    private long minValue = 0;
    private int pointsAmount = 0;

    private PlatformResourceKey itemKey;
    private Granularity granularity;
    private LocalDateTime dataTimeStamp;

    private long[] productionData;
    private long[] consumptionData;
    private long[] netData;
    private Long totalStored;

    private int selectedIndex = -1;
    private boolean loading = true;

    // #region Getters
    public Vector2i getGraphSize() {
        return new Vector2i(WIDTH, HEIGHT);
    }

    public PlatformResourceKey getItemKey() {
        return itemKey;
    }

    public long[] getProductionData() {
        return productionData;
    }

    public long[] getConsumptionData() {
        return consumptionData;
    }

    public long[] getNetData() {
        return netData;
    }

    public Long getTotalStored() {
        return totalStored;
    }

    public boolean isLoading() {
        return loading;
    }

    // #endregion
    // #region Setters

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void setGraphPos(int graphLeft, int graphBottom) {
        this.left = graphLeft;
        this.bottom = graphBottom;
    }

    public void setData(Map<ResourceChangeGranularityKey, long[]> data, Granularity granularity) {
        ResourceChangeGranularityKey rgk = data.keySet().stream().findFirst().orElse(null);
        if (rgk == null)
            return;
        this.itemKey = rgk.resourceKey();
        this.granularity = granularity;
        this.productionData = data.getOrDefault(
                new ResourceChangeGranularityKey(itemKey, (short) +1, granularity.getTickAmount()), new long[0]);
        this.consumptionData = data.getOrDefault(
                new ResourceChangeGranularityKey(itemKey, (short) -1, granularity.getTickAmount()), new long[0]);
        this.netData = IntStream.range(0, productionData.length).mapToLong(i -> productionData[i] - consumptionData[i])
                .toArray();
        this.totalStored = Arrays
                .stream(data.getOrDefault(
                        new ResourceChangeGranularityKey(itemKey, (short) 0, granularity.getTickAmount()), new long[0]))
                .findAny().orElse(0L);
        this.maxValue = Math.max(Arrays.stream(productionData).max().orElse(0L),
                Arrays.stream(consumptionData).max().orElse(0L));
        this.minValue = Math.max(Arrays.stream(productionData).min().orElse(0L),
                Arrays.stream(consumptionData).min().orElse(0L));
        this.pointsAmount = (int) Arrays.stream(productionData).count();

        this.dataTimeStamp = LocalDateTime.now();
        this.loading = false;
    }

    // #endregion
    // #endregion Properties
    // #region Drawing methods
    public void drawLine(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, int color, int thickness,
            LineStyle lineStyle) {
        // I've decided to go with .fill(), because it gives more Minecraft'y result
        // than nice and straight GL-rendered lines.
        if (lineStyle == LineStyle.BLOCKY) {
            Vector2d p1 = new Vector2d(
                    x1,
                    Math.ceil(y1 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            Vector2d p2 = new Vector2d(
                    x2,
                    Math.floor(y2 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            guiGraphics.fill((int) p1.x, (int) p1.y, (int) p2.x,
                    (int) (p2.y == p1.y ? p2.y + MINECRAFT_STYLE_VERTICAL_RESOLUTION : p2.y), color);

        } else if (lineStyle == LineStyle.EXACT) {
            double ht = thickness / 2f;
            double rads = Math.atan2(y2 - y1, x2 - x1);
            double tx = Math.sin(rads) * ht;
            double ty = -Math.cos(rads) * ht;
            List<Vector2d> points = List.of(
                    new Vector2d((float) (x1 + tx), (float) (y1 + ty)),
                    new Vector2d((float) (x1 - tx), (float) (y1 - ty)),
                    new Vector2d((float) (x2 - tx), (float) (y2 - ty)),
                    new Vector2d((float) (x2 + tx), (float) (y2 + ty)));
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
            points.forEach(p -> vc.addVertex(matrix4f, (float) p.x, (float) p.y, 0f).setColor(color));
            guiGraphics.flush();

            // debug
            // guiGraphics.fill((int) (x1 - ht + 1), (int) (y1 - ht + 1), (int) (x1 + ht -
            // 1), (int) (y1 + ht - 1), color);
        }
    }

    public void drawGradientBg(GuiGraphics guiGraphics, List<Vector2d> points, int baseColor, int targetColor) {
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
                    vc.addVertex(matrix4f, (float) a.x, (float) bottom, 0f).setColor(prevColor);
                    vc.addVertex(matrix4f, (float) b.x, (float) bottom, 0f).setColor(color);
                    guiGraphics.flush();
                });
    }

    public void drawGraph(GuiGraphics guiGraphics, List<Vector2d> points, int thickness, int color) {
        getPairStream(points)
                .forEach(p -> drawLine(guiGraphics, p.prev.x, p.prev.y, p.cur.x, p.cur.y, color, thickness, lineStyle));
    }

    public void drawGraph(GuiGraphics guiGraphics, long[] arr, int thickness, int color) {
        drawGraph(guiGraphics, getGuiXYArrayD(arr), thickness, color);
    }

    // #endregion
    // #region Draw graph
    public void drawGraphs(GuiGraphics graphics) {
        List<Vector2d> netPoints = getGuiXYArrayD(netData);
        drawGradientBg(graphics, netPoints, GRAPH_GRADIENT_FROM, GRAPH_GRADIENT_TO);
        drawGraph(graphics, netPoints, 1, NET_GRAPH_COLOR);
        drawGraph(graphics, productionData, 1, PRODUCTION_GRAPH_COLOR);
        drawGraph(graphics, consumptionData, 1, CONSUMPTION_GRAPH_COLOR);
    }

    // #endregion
    // #region Rendering utils
    public ResourceRendering getResourceRendering() {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(itemKey.getClass());
    }

    public Component getItemName() {
        return getResourceRendering().getDisplayName(itemKey);
    }

    public void renderItem(GuiGraphics guiGraphics, int x, int y) {
        getResourceRendering().render(itemKey, guiGraphics, x, y);
    }

    // #endregion
    // #region Value Translators
    public Vector2i getGuiXYFromGraphValue(Integer index, Long value) {
        int x = (int) ((WIDTH / Math.max(1, pointsAmount - 1f)) * index);
        if (maxValue - minValue == 0)
            return new Vector2i(left + x, bottom - (HEIGHT / 2));
        int y = (int) ((Math.max(value, 0f) / maxValue) * HEIGHT);
        return new Vector2i(left + x, bottom - y);
    }

    public Vector2i getGraphValueFromGuiXY(double x, double y) {
        int index = (int) Math.floor(((x - left) * ((pointsAmount - 1f) / WIDTH)));
        int value = (int) Math.max(0, maxValue * (bottom - y) / HEIGHT);
        return new Vector2i(index, value);
    }

    public List<Vector2i> getGuiXYArray(long[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> getGuiXYFromGraphValue(i, arr[i]))
                .toList();
    }

    public List<Vector2d> getGuiXYArrayD(long[] arr) {
        return getGuiXYArray(arr).stream().map(v -> new Vector2d(v.x, v.y)).toList();
    }

    // #endregion
    // #region Calculations
    // #region Production
    public Long getMaxProduction() {
        return Arrays.stream(productionData).max().orElse(0L);
    }

    public Long getMinProduction() {
        return Arrays.stream(productionData).min().orElse(0L);
    }

    public double getAvgProduction() {
        return Arrays.stream(productionData).average().orElse(0.0);
    }

    // #endregion
    // #region Consumption
    public Long getMaxConsumption() {
        return Arrays.stream(consumptionData).max().orElse(0L);
    }

    public Long getMinConsumption() {
        return Arrays.stream(consumptionData).min().orElse(0L);
    }

    public double getAvgConsumption() {
        return Arrays.stream(consumptionData).average().orElse(0.0);
    }

    // #endregion
    // #region Net
    public double getNetAvg() {
        return Arrays.stream(netData).average().orElse(0.0);
    }

    // #endregion
    // #endregion
    // #region Hooks
    public boolean isInBounds(int mouseX, int mouseY) {
        return mouseY <= bottom && mouseY >= bottom - HEIGHT && mouseX >= left && mouseX <= left + WIDTH;
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
        if (!isInBounds(mouseX, mouseY) && (selectedIndex == -1))
            return;
        int index = getGraphValueFromGuiXY(mouseX, mouseY).x;
        if ((index < 0 || index >= pointsAmount) && selectedIndex == -1)
            return; // just additional precaution

        int indexFrom = selectedIndex != -1 ? Math.min(Math.max(0, index), selectedIndex) : index;
        int indexTo = selectedIndex != -1 ? Math.max(Math.min(index, pointsAmount - 1) + 1, selectedIndex) : index + 1;

        int selectionX1 = getGuiXYFromGraphValue(indexFrom, 0L).x;
        int selectionX2 = getGuiXYFromGraphValue(indexTo, 0L).x;
        guiGraphics.fill(selectionX1, bottom, selectionX2, bottom - HEIGHT, 250, 0x66ffffff);

        LocalDateTime indexDate = dataTimeStamp.minus(pointsAmount - indexFrom, granularity.getChronoUnit());
        LocalDateTime nextIndexDate = dataTimeStamp.minus(pointsAmount - (indexTo), granularity.getChronoUnit());
        // Math.max and Math.abs are here as a precaution
        long incvalue = Math.max(0, Arrays.stream(productionData).skip(indexFrom).limit(indexTo - indexFrom).sum());
        long decvalue = Math.abs(Arrays.stream(consumptionData).skip(indexFrom).limit(indexTo - indexFrom).sum());

        ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(itemKey.getClass());
        List<ClientTooltipComponent> lines = new ArrayList<>();

        lines.add(new ClientTextTooltip(resourceRendering.getDisplayName(itemKey).getVisualOrderText()));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(
                "Flow between " +
                        indexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                        " and " +
                        nextIndexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        lines.add(new SmallTextClientTooltipComponent(
                Component.literal(String.format("Inflow:  +%,d", incvalue)).withColor(0xff00ff00)));
        lines.add(new SmallTextClientTooltipComponent(
                Component.literal(String.format("Outflow: -%,d", decvalue)).withColor(0xffff0000)));
        lines.add(new SmallTextClientTooltipComponent(
                Component.literal(String.format("Netflow: %,d", incvalue - decvalue)).withColor(0xff66ddff)));
        Platform.INSTANCE.renderTooltip(guiGraphics, lines, mouseX, mouseY);
    }

    // #endregion
    // #region Value utils
    private Stream<PointPair> getPairStream(List<Vector2d> points) {
        return IntStream.range(0, points.size())
                .mapToObj(i -> new PointPair(i == 0 ? points.get(i) : points.get(i - 1), points.get(i)));
    }

    private record PointPair(Vector2d prev, Vector2d cur) {
    }
    // #endregion
}
