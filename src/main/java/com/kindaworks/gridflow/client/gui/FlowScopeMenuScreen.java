package com.kindaworks.gridflow.client.gui;

import com.kindaworks.gridflow.block.FlowScopeBlock;
import com.kindaworks.gridflow.client.gui.components.DynamicButton;
import com.kindaworks.gridflow.client.gui.components.FlowScopeGraph;
import com.kindaworks.gridflow.client.gui.side_buttons.Granularity;
import com.kindaworks.gridflow.client.gui.side_buttons.LineStyle;
import com.kindaworks.gridflow.client.gui.side_buttons.SortingDirection;
import com.kindaworks.gridflow.client.gui.side_buttons.SortingType;
import com.kindaworks.gridflow.init.GridFlowModMenus;
import com.kindaworks.gridflow.init.GridFlowModScreens;
import com.kindaworks.gridflow.resource.ResourceChangeGranularityKey;
import com.kindaworks.gridflow.util.TickScheduler;
import com.kindaworks.gridflow.world.inventory.FlowScopeMenuMenu;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import com.refinedmods.refinedstorage.common.support.widget.ScrollbarWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2i;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class FlowScopeMenuScreen extends AbstractContainerScreen<FlowScopeMenuMenu> implements GridFlowModScreens.ScreenAccessor {
    //#region Properties
    final int PRODUCTION_GREEN = 0xff00ff00;
    final int CONSUMPTION_RED = 0xffff0000;
    final int ESTIMATES_BLUE = 0xff66ddff;
    final int WHITE = 0xffffffff;

    final int SIDE_BUTTON_ROW_HEIGHT = 20;

    // Simple generation constants
    final int ROW_HEIGHT = 25;
    final int INNER_WIDTH = 222;
    final int INNER_HEIGHT = 180;
    final int NUMBER_OF_COLS = 3;

    // Detailed generation constants
    final int TEXT_LINE_HEIGHT = 12;
    final WidgetSprites side_button_sprites;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    private final List<DynamicButton> itemButtons = new ArrayList<>();
    private final List<DynamicButton> sideButtons = new ArrayList<>();
    private final FlowScopeGraph graph = new FlowScopeGraph();
    int INNER_LEFT = this.leftPos + 5;
    int INNER_TOP = this.topPos + 20;
    SortingType sortingType = SortingType.QUANTITY;
    SortingDirection sortingDirection = SortingDirection.DESCENDING;
    Button button_done;
    ScrollbarWidget scrollbar;
    Granularity granularity = Granularity.MINUTE;
    TickScheduler tickScheduler = new TickScheduler(granularity.getTickAmount());
    private boolean menuStateUpdateActive = false;
    private Map<PlatformResourceKey, Map<Short, Long>> lastSnapshot = new HashMap<>();
    private boolean hasDetailedGenerationData = false;

    //#endregion
    //#region Constructor
    public FlowScopeMenuScreen(FlowScopeMenuMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 250;
        this.imageHeight = 225;

        side_button_sprites = new WidgetSprites(
                ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/base"),
                null,
                ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/hovered"),
                null
        );
    }

    //#endregion
    //#region Rendering screens
    private void renderDetailedGenerationStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);

        guiGraphics.blit(ResourceLocation.parse(
                        "gridflow:textures/screens/flow_scope_detail.png"),
                this.leftPos - 3, this.topPos,
                0, 0, 256, 256, 256, 256);

        PlatformResourceKey itemKey = graph.getItemKey();
        guiGraphics.drawString(font, this.title.getString() + " - Flow details", this.leftPos + 8, this.topPos + 6, 4210752, false);

        graph.renderItem(guiGraphics, this.leftPos + 7, this.topPos + 26);
        guiGraphics.drawString(font, graph.getItemName(), this.leftPos + 36, this.topPos + 30, WHITE);


        final int graphLeft = this.leftPos + 7;
        final int graphBottom = this.topPos + 134;
        final int graphWidth = graph.getGraphSize().x;
        final int graphHeight = graph.getGraphSize().y;
        graph.setGraphPos(graphLeft, graphBottom);
        if (graph.isLoading()) {
            guiGraphics.drawString(font, "Loading...", graphLeft + (graphWidth / 4), graphBottom - (graphHeight / 2), WHITE);
            return;
        }

        graph.drawGraphs(guiGraphics);

        // Main stats
        int PRODUCTION_ROW_SHIFT = 2;
        guiGraphics.drawString(font, "Inflow", graphLeft + PRODUCTION_ROW_SHIFT, graphBottom + TEXT_LINE_HEIGHT + 2, PRODUCTION_GREEN);
        guiGraphics.drawString(font, "Max: " + formatAmount(itemKey, graph.getMaxProduction()) + granularity.perStr(), graphLeft + PRODUCTION_ROW_SHIFT, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        guiGraphics.drawString(font, "Min:  " + formatAmount(itemKey, graph.getMinProduction()) + granularity.perStr(), graphLeft + PRODUCTION_ROW_SHIFT, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        int CONSUMPTION_ROW_SHIFT = 78;
        guiGraphics.drawString(font, "Outflow", graphLeft + CONSUMPTION_ROW_SHIFT, graphBottom + TEXT_LINE_HEIGHT + 2, CONSUMPTION_RED);
        guiGraphics.drawString(font, "Max: " + formatAmount(itemKey, graph.getMaxConsumption()) + granularity.perStr(), graphLeft + CONSUMPTION_ROW_SHIFT, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        guiGraphics.drawString(font, "Min:  " + formatAmount(itemKey, graph.getMinConsumption()) + granularity.perStr(), graphLeft + CONSUMPTION_ROW_SHIFT, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        //Average lines
        guiGraphics.drawString(font, "Avg", graphLeft + graphWidth + 8, graphBottom - graphHeight - 14, WHITE);

        double decAvg = graph.getAvgConsumption();
        Vector2i decAvgPoint = graph.getGuiXYFromGraphValue(0, (long) decAvg);
        graph.drawLine(guiGraphics, graphLeft, decAvgPoint.y, graphLeft + graphWidth + 20, decAvgPoint.y, CONSUMPTION_RED, 1, LineStyle.EXACT);

        double incAvg = graph.getAvgProduction();
        Vector2i incAvgPoint = graph.getGuiXYFromGraphValue(0, (long) incAvg);
        graph.drawLine(guiGraphics, graphLeft, incAvgPoint.y, graphLeft + graphWidth + 20, incAvgPoint.y, PRODUCTION_GREEN, 1, LineStyle.EXACT);

        //Average line values
        int AVG_VALUE_SHIFT = 3;
        if (incAvg >= decAvg) {
            guiGraphics.drawString(font, "+" + formatAmount(itemKey, (long) incAvg) + granularity.perStr(), graphLeft + graphWidth + AVG_VALUE_SHIFT, incAvgPoint.y - 10, PRODUCTION_GREEN);
            guiGraphics.drawString(font, "-" + formatAmount(itemKey, (long) decAvg) + granularity.perStr(), graphLeft + graphWidth + AVG_VALUE_SHIFT, decAvgPoint.y + 2, CONSUMPTION_RED);
        } else {
            guiGraphics.drawString(font, "+" + formatAmount(itemKey, (long) incAvg) + granularity.perStr(), graphLeft + graphWidth + AVG_VALUE_SHIFT, incAvgPoint.y + 2, PRODUCTION_GREEN);
            guiGraphics.drawString(font, "-" + formatAmount(itemKey, (long) decAvg) + granularity.perStr(), graphLeft + graphWidth + AVG_VALUE_SHIFT, decAvgPoint.y - 10, CONSUMPTION_RED);
        }

        // Estimates
        int ESTIMATES_ROW_SHIFT = 159;
        guiGraphics.drawString(font, "Estimates (net)", graphLeft + ESTIMATES_ROW_SHIFT, graphBottom + 25, ESTIMATES_BLUE);
        long seconds = (long) Granularity.SECOND.convertFrom(granularity, graph.getNetAvg());
        guiGraphics.drawString(font, formatSignedAmount(itemKey, seconds) + Granularity.SECOND.perStr(),
                graphLeft + ESTIMATES_ROW_SHIFT, graphBottom + 5 + TEXT_LINE_HEIGHT * 3, WHITE);
        long minutes = (long) Granularity.MINUTE.convertFrom(granularity, graph.getNetAvg());
        guiGraphics.drawString(font, formatSignedAmount(itemKey, minutes) + Granularity.MINUTE.perStr(),
                graphLeft + ESTIMATES_ROW_SHIFT, graphBottom + 5 + TEXT_LINE_HEIGHT * 4, WHITE);
        long hours = (long) Granularity.HOUR.convertFrom(granularity, graph.getNetAvg());
        guiGraphics.drawString(font, formatSignedAmount(itemKey, hours) + Granularity.HOUR.perStr(),
                graphLeft + ESTIMATES_ROW_SHIFT, graphBottom + 5 + TEXT_LINE_HEIGHT * 5, WHITE);
        long days = (long) Granularity.DAY.convertFrom(granularity, graph.getNetAvg());
        guiGraphics.drawString(font, formatSignedAmount(itemKey, days) + Granularity.DAY.perStr(),
                graphLeft + ESTIMATES_ROW_SHIFT, graphBottom + 5 + TEXT_LINE_HEIGHT * 6, WHITE);

        // Overall stats
        final long net = Arrays.stream(graph.getNetData()).sum();
        guiGraphics.drawString(font, "Total in storage: " + graph.getTotalStored(), graphLeft + PRODUCTION_ROW_SHIFT, graphBottom + 4 + TEXT_LINE_HEIGHT * 5, WHITE);
        guiGraphics.drawString(font, "Net in this timeframe: " + (net > 0 ? "+" : "") + formatAmount(itemKey, net), graphLeft + PRODUCTION_ROW_SHIFT, graphBottom + 4 + TEXT_LINE_HEIGHT * 6, WHITE);

        guiGraphics.pose().popPose();
    }

    private void renderSimpleGenerationStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        INNER_LEFT = this.leftPos + 5;
        INNER_TOP = this.topPos + 20;
        final int rowWidth = INNER_WIDTH / NUMBER_OF_COLS;
        enableScissorFromGui(INNER_LEFT, INNER_TOP, INNER_WIDTH, INNER_HEIGHT);

        int counter = 0;
        itemButtons.clear();
        for (Map.Entry<PlatformResourceKey, Map<Short, Long>> item : sortingDirection.sort(sortingType.sort(lastSnapshot)).entrySet()) {
            final PlatformResourceKey itemKey = item.getKey();
            final Map<Short, Long> itemChange = item.getValue();
            final int itemX = INNER_LEFT + ((counter % NUMBER_OF_COLS) * rowWidth);
            final int itemY = INNER_TOP + (Math.floorDiv(counter, NUMBER_OF_COLS) * ROW_HEIGHT) - (int) scrollbar.getOffset();
            if (itemY > this.topPos + this.height) break;
            if (itemY < this.topPos - ROW_HEIGHT) {
                counter++;
                continue;
            }
            ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(itemKey.getClass());

            // Rendering buttons
            DynamicButton button = new DynamicButton(
                    itemX,
                    itemY,
                    rowWidth - 1, ROW_HEIGHT - 1, "",
                    () -> requestDetailedGenerationStats(itemKey, true),
                    null,
                    List.of(new ClientTextTooltip(resourceRendering.getDisplayName(itemKey).getVisualOrderText()))
            );
            button.render(guiGraphics, mouseX, mouseY);
            itemButtons.add(button);

            // Adding margins
            final int left = itemX + 4;
            final int top = itemY + 4;

            // Rendering item texture
            resourceRendering.render(itemKey, guiGraphics, left, top);

            // Rendering generation stats
            final long net = (itemChange.get((short) +1)) - (itemChange.get((short) -1));
            guiGraphics.drawString(font,
                    formatSignedAmount(itemKey, net) + granularity.perStr(),
                    left + 20, top + 5,
                    (net > 0 ? PRODUCTION_GREEN : net < 0 ? CONSUMPTION_RED : WHITE)
            );
//            guiGraphics.drawString(font, "+"+ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) +1), true)+granularity.perStr(), left + 20, top, PRODUCTION_GREEN);
//            guiGraphics.drawString(font, "-"+ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) -1), true)+granularity.perStr(), left + 20, top + 10, CONSUMPTION_RED);

            counter++;
        }
        if (scrollbar != null) {
            double rowsAmount = Math.ceil(lastSnapshot.size() * 1f / NUMBER_OF_COLS);
            double maxOffset = rowsAmount * ROW_HEIGHT;
            scrollbar.setEnabled(maxOffset > 180);
            scrollbar.setMaxOffset(maxOffset - 180);
        }
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
    }

    //#endregion
    //#region Protocol
    private void requestDetailedGenerationStats(PlatformResourceKey itemKey, boolean manual) {
        if (entity instanceof Player player && player.containerMenu instanceof GridFlowModMenus.MenuAccessor menu) {
            if (manual) graph.setLoading(true);
            menu.sendMenuStateUpdate(player, 5, "detailedFactoryGenerationRequest", new ResourceChangeGranularityKey(itemKey, (short) 0, granularity.getTickAmount()), false);
        }
    }

    private void requestSimpleGenerationStats() {
        if (entity instanceof Player player && player.containerMenu instanceof GridFlowModMenus.MenuAccessor menu) {
            menu.sendMenuStateUpdate(player, 4, "simpleFactoryGenerationRequest", granularity.getTickAmount(), false);
        }
    }

    @Override
    public void updateMenuState(int elementType, String name, Object elementState) {
        menuStateUpdateActive = true;
        if (name.equals("detailedFactoryGeneration")) {
            graph.setData((Map<ResourceChangeGranularityKey, long[]>) elementState, granularity);
            hasDetailedGenerationData = true;
        } else if (name.equals("lastSnapshot")) {
            lastSnapshot = (Map<PlatformResourceKey, Map<Short, Long>>) elementState;
        }
        menuStateUpdateActive = false;
    }

    //#endregion
    //#region Hooks
    //#region Hooks -> IO
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasDetailedGenerationData) {
            // back button
            if (button == 3) {
                hasDetailedGenerationData = false;
                return true;
            }
            if (graph.mouseClicked(mouseX, mouseY, button)) return true;
        } else {
            // forward button
            if (button == 4) {
                hasDetailedGenerationData = !graph.isLoading();
                return true;
            }
            for (DynamicButton b : itemButtons) {
                if (INNER_LEFT < mouseX && mouseX < INNER_LEFT + INNER_WIDTH && INNER_TOP < mouseY && mouseY < INNER_TOP + INNER_HEIGHT)
                    if (b.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }

        for (DynamicButton b : sideButtons) {
            if (b.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (scrollbar != null && scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        System.out.printf("%f, %f - %d%n", mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(final double mx, final double my) {
        if (scrollbar != null) {
            scrollbar.mouseMoved(mx, my);
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        if (scrollbar != null && scrollbar.mouseReleased(mx, my, button)) {
            return true;
        }
        if (hasDetailedGenerationData)
            if (graph.mouseReleased(mx, my, button)) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double z, final double delta) {
        final boolean didScroll = scrollbar != null
                && isHoveringOverArea(x, y)
                && scrollbar.mouseScrolled(x, y, z, delta);
        return didScroll || super.mouseScrolled(x, y, z, delta);
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            if (hasDetailedGenerationData) {
                hasDetailedGenerationData = false;
            } else {
                assert this.minecraft != null;
                assert this.minecraft.player != null;
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(key, b, c);
    }
    //#endregion

    @Override
    public void removed() {
        super.removed();
        BlockState blockState = world.getBlockState(new BlockPos(x, y, z))
                .setValue(FlowScopeBlock.SORT_TYPE, sortingType.ordinal())
                .setValue(FlowScopeBlock.SORT_DIRECTION, sortingDirection.ordinal())
                .setValue(FlowScopeBlock.GRANULARITY, granularity.ordinal());
        world.setBlock(new BlockPos(x, y, z), blockState, 0); // temporary, client-side only
    }

    @Override
    public void init() {
        super.init();
        button_done = Button.builder(Component.translatable("gui.gridflow.flow_scope_menu.button_done"), b -> {
            assert this.minecraft != null;
            assert this.minecraft.player != null;
            this.minecraft.player.closeContainer();
        }).bounds(this.leftPos + 195, this.topPos + 205, 46, 20).build();
        this.addRenderableWidget(button_done);
        scrollbar = new ScrollbarWidget(this.leftPos + 232, this.topPos + 20, ScrollbarWidget.Type.NORMAL, 180);
        this.addRenderableWidget(scrollbar);

        BlockState blockState = world.getBlockState(new BlockPos(x, y, z));
        // TODO: maybe save that data back?
        sortingType = SortingType.values()[blockState.getValue(FlowScopeBlock.SORT_TYPE)];
        sortingDirection = SortingDirection.values()[blockState.getValue(FlowScopeBlock.SORT_DIRECTION)];
    }

    //#endregion
    //#region Tick
    protected void containerTick() {
        super.containerTick();
        if (tickScheduler.shouldRun()) {
            if (hasDetailedGenerationData) {
                PlatformResourceKey itemKey = graph.getItemKey();
                requestDetailedGenerationStats(itemKey, false);
            } else {
                requestSimpleGenerationStats();
            }
        }
    }

    //#endregion
    //#region Utils
    private boolean isHoveringOverArea(final double x, final double y) {
        return isHovering(8, 20, 221, 180, x, y);
    }

    private String formatAmount(PlatformResourceKey resourceKey, long amount) {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(resourceKey.getClass()).formatAmount(amount, true);
    }

    private String formatSignedAmount(final PlatformResourceKey resourceKey, long amount) {
        final long unsignedAmount = Math.abs(amount);
        String formatted = formatAmount(resourceKey, unsignedAmount);
        if (amount < 0) return "-" + formatted;
        else return "+" + formatted;
    }

    //#endregion
    //#region Rendering utils - these should probably live in a different package, as static
    public void enableScissorFromGui(int guiX, int guiY, int guiWidth, int guiHeight) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        double scale = window.getGuiScale();

        int x = (int) (guiX * scale);
        int y = (int) (window.getHeight() - (guiY + guiHeight) * scale);
        int width = (int) (guiWidth * scale);
        int height = (int) (guiHeight * scale);

        RenderSystem.enableScissor(x, y, width, height);
    }

    //#endregion
    //#region Rendering pipeline
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderSimpleGenerationStats(guiGraphics, mouseX, mouseY);
        this.renderSideButtons(guiGraphics, mouseX, mouseY);
        if (hasDetailedGenerationData) {
            this.renderDetailedGenerationStats(guiGraphics, mouseX, mouseY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(ResourceLocation.parse("gridflow:textures/screens/flow_scope.png"), this.leftPos - 3, this.topPos, 0, 0, 256, 256, 256, 256);
        RenderSystem.disableBlend();
    }

    private void renderSideButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        sideButtons.clear();
        Function<String, String> capitalCase = a -> a.substring(0, 1).toUpperCase() + a.substring(1).toLowerCase();
        DynamicButton sortingDirectionButton = new DynamicButton(leftPos - 24, topPos + 6, 18, 18, "",
                side_button_sprites, sortingDirection.getResourceLocation(),
                () -> this.sortingDirection = SortingDirection.next(sortingDirection),
                null,
                List.of(
                        new ClientTextTooltip(Component.literal("Sorting direction").getVisualOrderText()),
                        new ClientTextTooltip(Component.literal(capitalCase.apply(sortingDirection.toString())).withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())
                )
        );
        sortingDirectionButton.render(guiGraphics, mouseX, mouseY);
        sideButtons.add(sortingDirectionButton);

        DynamicButton sortingTypeButton = new DynamicButton(leftPos - 24, topPos + 6 + SIDE_BUTTON_ROW_HEIGHT, 18, 18, "",
                side_button_sprites, sortingType.getResourceLocation(),
                () -> this.sortingType = SortingType.next(sortingType),
                null,
                List.of(
                        new ClientTextTooltip(Component.literal("Sorting Type").getVisualOrderText()),
                        new ClientTextTooltip(Component.literal(capitalCase.apply(sortingType.toString())).withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())
                )
        );
        sortingTypeButton.render(guiGraphics, mouseX, mouseY);
        sideButtons.add(sortingTypeButton);

        DynamicButton granularityButton = new DynamicButton(leftPos - 24, topPos + 6 + (SIDE_BUTTON_ROW_HEIGHT * 2), 18, 18, "",
                side_button_sprites, granularity.getResourceLocation(),
                () -> {
                    this.granularity = Granularity.next(granularity);
                    this.tickScheduler = new TickScheduler(granularity.getTickAmount());
                },
                () -> {
                    this.granularity = Granularity.prev(granularity);
                    this.tickScheduler = new TickScheduler(granularity.getTickAmount());
                    graph.setLoading(true);
                },
                List.of(
                        new ClientTextTooltip(Component.literal("Granularity").getVisualOrderText()),
                        new ClientTextTooltip(Component.literal(capitalCase.apply(granularity.toString())).withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())
                )
        );
        granularityButton.render(guiGraphics, mouseX, mouseY);
        sideButtons.add(granularityButton);

        DynamicButton styleButton = new DynamicButton(leftPos - 24, topPos + 6 + (SIDE_BUTTON_ROW_HEIGHT * 3), 18, 18, "",
                side_button_sprites, graph.lineStyle.getResourceLocation(),
                () -> {
                    graph.lineStyle = LineStyle.next(graph.lineStyle);
                },
                null,
                List.of(
                        new ClientTextTooltip(Component.literal("Line Style").getVisualOrderText()),
                        new ClientTextTooltip(Component.literal(capitalCase.apply(graph.lineStyle.toString())).withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())
                )
        );
        styleButton.render(guiGraphics, mouseX, mouseY);
        sideButtons.add(styleButton);

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // don't call super
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hasDetailedGenerationData)
            graph.renderTooltip(guiGraphics, mouseX, mouseY);
        else
            for (DynamicButton button : itemButtons) {
                button.renderTooltip(guiGraphics, mouseX, mouseY);
            }
        for (DynamicButton button : sideButtons) {
            button.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    //#endregion
}