package com.agent314.colorbiome;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class PaletteBrushScreen extends Screen {
    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 300;
    private static final int LEFT_PANEL_WIDTH = 190;
    private static final int GAP = 12;
    private static final int ROW_HEIGHT = 34;
    private static final int CONTROL_HEIGHT = 20;
    private static final int MAX_VERTICAL_DISTANCE = 10_000;

    private static BiomePalette lastVegetation = BiomePalette.PLAINS;
    private static BiomePalette lastWater = BiomePalette.PLAINS;
    private static int lastAbove = 64;
    private static int lastBelow = 64;
    private static boolean lastToWorldCeiling = true;
    private static boolean lastToWorldFloor = true;

    private BiomePalette vegetation = lastVegetation;
    private BiomePalette water = lastWater;
    private ActiveTarget activeTarget = ActiveTarget.VEGETATION;
    private boolean toWorldCeiling = lastToWorldCeiling;
    private boolean toWorldFloor = lastToWorldFloor;

    private List<BiomePalette> allBiomes = List.of(BiomePalette.PLAINS);
    private List<BiomePalette> visibleBiomes = allBiomes;

    private int panelLeft;
    private int panelTop;
    private int leftPanelLeft;
    private int rightPanelLeft;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int scrollOffset;

    private EditBox searchBox;
    private EditBox aboveBox;
    private EditBox belowBox;

    public PaletteBrushScreen() {
        super(Component.translatable("screen.colorbiome.palette_brush.title"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        int panelHeight = Math.min(PANEL_HEIGHT, height - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        leftPanelLeft = panelLeft + 12;
        rightPanelLeft = leftPanelLeft + LEFT_PANEL_WIDTH + GAP;

        int rightPanelWidth = panelWidth - LEFT_PANEL_WIDTH - GAP - 24;
        listLeft = rightPanelLeft + 10;
        listTop = panelTop + 62;
        listWidth = rightPanelWidth - 20;
        listHeight = panelHeight - 80;

        loadBiomes();

        searchBox = addRenderableWidget(new EditBox(font, listLeft, panelTop + 30, listWidth, 20,
                Component.translatable("screen.colorbiome.palette_brush.search")));
        searchBox.setHint(Component.translatable("screen.colorbiome.palette_brush.search"));
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            updateVisibleBiomes();
        });

        aboveBox = addRenderableWidget(new EditBox(font, heightFieldLeft(), aboveFieldTop(), heightFieldWidth(), 18,
                Component.translatable("screen.colorbiome.palette_brush.above")));
        aboveBox.setValue(Integer.toString(lastAbove));
        aboveBox.setFilter(this::isValidDistanceInput);

        belowBox = addRenderableWidget(new EditBox(font, heightFieldLeft(), belowFieldTop(), heightFieldWidth(), 18,
                Component.translatable("screen.colorbiome.palette_brush.below")));
        belowBox.setValue(Integer.toString(lastBelow));
        belowBox.setFilter(this::isValidDistanceInput);

        updateVisibleBiomes();
        updateHeightInputState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanels(graphics);
        renderControls(graphics, mouseX, mouseY);
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        renderBiomeList(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInVegetationControl(mouseX, mouseY)) {
            activeTarget = ActiveTarget.VEGETATION;
            return true;
        }

        if (isInWaterControl(mouseX, mouseY)) {
            activeTarget = ActiveTarget.WATER;
            return true;
        }

        if (isInCeilingToggle(mouseX, mouseY)) {
            toWorldCeiling = !toWorldCeiling;
            updateHeightInputState();
            return true;
        }

        if (isInFloorToggle(mouseX, mouseY)) {
            toWorldFloor = !toWorldFloor;
            updateHeightInputState();
            return true;
        }

        if (isInConfirmControl(mouseX, mouseY)) {
            lastVegetation = vegetation;
            lastWater = water;
            lastAbove = distanceFrom(aboveBox);
            lastBelow = distanceFrom(belowBox);
            lastToWorldCeiling = toWorldCeiling;
            lastToWorldFloor = toWorldFloor;
            sendSettings();
            onClose();
            return true;
        }

        if (isInList(mouseX, mouseY)) {
            int index = scrollOffset + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (index >= 0 && index < visibleBiomes.size()) {
                selectBiome(visibleBiomes.get(index));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInList(mouseX, mouseY)) {
            int direction = scrollY > 0 ? -3 : 3;
            scrollOffset = clamp(scrollOffset + direction, 0, maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadBiomes() {
        if (Minecraft.getInstance().level == null) {
            allBiomes = List.of(BiomePalette.PLAINS);
            visibleBiomes = allBiomes;
            return;
        }

        allBiomes = Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .registryKeySet()
                .stream()
                .map(BiomePalette::new)
                .sorted(Comparator.comparing(palette -> palette.id().toString()))
                .toList();

        if (!allBiomes.contains(vegetation)) {
            vegetation = BiomePalette.PLAINS;
        }
        if (!allBiomes.contains(water)) {
            water = BiomePalette.PLAINS;
        }

        visibleBiomes = allBiomes;
    }

    private void updateVisibleBiomes() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty()) {
            visibleBiomes = allBiomes;
        } else {
            visibleBiomes = allBiomes.stream()
                    .filter(palette -> matchesQuery(palette, query))
                    .toList();
        }

        scrollOffset = clamp(scrollOffset, 0, maxScroll());
    }

    private boolean matchesQuery(BiomePalette palette, String query) {
        return palette.id().toString().toLowerCase(Locale.ROOT).contains(query)
                || palette.displayName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void renderPanels(GuiGraphics graphics) {
        int panelRight = panelLeft + Math.min(PANEL_WIDTH, width - 24);
        int panelBottom = panelTop + Math.min(PANEL_HEIGHT, height - 24);
        int rightPanelRight = panelRight - 12;
        int rightPanelBottom = panelBottom - 12;
        int leftPanelRight = leftPanelLeft + LEFT_PANEL_WIDTH;

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xDD101010);
        graphics.fill(leftPanelLeft - 1, panelTop + 21, leftPanelRight + 1, panelBottom - 11, 0xFF606060);
        graphics.fill(leftPanelLeft, panelTop + 22, leftPanelRight, panelBottom - 12, 0xDD202020);
        graphics.fill(rightPanelLeft - 1, panelTop + 21, rightPanelRight + 1, rightPanelBottom + 1, 0xFF606060);
        graphics.fill(rightPanelLeft, panelTop + 22, rightPanelRight, rightPanelBottom, 0xDD202020);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 8, 0xFFFFFF);
        graphics.drawString(font, activeTarget.label(), leftPanelLeft + 10, panelTop + 32, 0xE0E0E0);
        graphics.drawString(font, Component.translatable("screen.colorbiome.palette_brush.height"), leftPanelLeft + 10,
                panelTop + 102, 0xC8C8C8);
        graphics.drawString(font, Component.translatable("screen.colorbiome.palette_brush.above"), leftPanelLeft + 10,
                aboveFieldTop() + 5, 0xE0E0E0);
        graphics.drawString(font, Component.translatable("screen.colorbiome.palette_brush.below"), leftPanelLeft + 10,
                belowFieldTop() + 5, 0xE0E0E0);
        graphics.drawString(font, Component.translatable("screen.colorbiome.palette_brush.biomes"), listLeft, panelTop + 16, 0xE0E0E0);
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        renderControl(graphics, vegetationLabel(), leftPanelLeft + 10, vegetationControlTop(), LEFT_PANEL_WIDTH - 20,
                activeTarget == ActiveTarget.VEGETATION, isInVegetationControl(mouseX, mouseY), 0xFF426D2A);
        renderControl(graphics, waterLabel(), leftPanelLeft + 10, waterControlTop(), LEFT_PANEL_WIDTH - 20,
                activeTarget == ActiveTarget.WATER, isInWaterControl(mouseX, mouseY), 0xFF2C5E74);
        renderToggle(graphics, Component.translatable("screen.colorbiome.palette_brush.to_world_ceiling"),
                leftPanelLeft + 10, ceilingToggleTop(), toWorldCeiling, isInCeilingToggle(mouseX, mouseY));
        renderToggle(graphics, Component.translatable("screen.colorbiome.palette_brush.to_world_floor"),
                leftPanelLeft + 10, floorToggleTop(), toWorldFloor, isInFloorToggle(mouseX, mouseY));
        renderControl(graphics, Component.translatable("screen.colorbiome.palette_brush.confirm"), leftPanelLeft + 35,
                confirmControlTop(), 120, false, isInConfirmControl(mouseX, mouseY), 0xFF50733C);
    }

    private void renderControl(GuiGraphics graphics, Component label, int left, int top, int controlWidth,
            boolean active, boolean hovered, int accentColor) {
        int borderColor = active ? accentColor : 0xFF505050;
        int background = hovered ? 0xFF303030 : 0xFF181818;
        if (active) {
            background = 0xFF252D20;
        }

        graphics.fill(left - 1, top - 1, left + controlWidth + 1, top + CONTROL_HEIGHT + 1, borderColor);
        graphics.fill(left, top, left + controlWidth, top + CONTROL_HEIGHT, background);
        graphics.drawCenteredString(font, label, left + controlWidth / 2, top + 6, 0xF0F0F0);
    }

    private void renderToggle(GuiGraphics graphics, Component label, int left, int top, boolean selected, boolean hovered) {
        int borderColor = selected ? 0xFF6A8A48 : 0xFF505050;
        int background = hovered ? 0xFF303030 : 0xFF181818;
        graphics.fill(left, top, left + 12, top + 12, borderColor);
        graphics.fill(left + 1, top + 1, left + 11, top + 11, background);
        if (selected) {
            graphics.drawString(font, "x", left + 3, top + 2, 0xE0E0E0);
        }
        graphics.drawString(font, label, left + 17, top + 2, 0xD0D0D0);
    }

    private void renderBiomeList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(listLeft - 1, listTop - 1, listLeft + listWidth + 1, listTop + listHeight + 1, 0xFF404040);
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xDD080808);

        if (visibleBiomes.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.colorbiome.palette_brush.empty"),
                    listLeft + listWidth / 2, listTop + listHeight / 2 - 4, 0xA0A0A0);
            return;
        }

        graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + listHeight);

        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            int index = scrollOffset + row;
            if (index >= visibleBiomes.size()) {
                break;
            }

            BiomePalette palette = visibleBiomes.get(index);
            int rowTop = listTop + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            boolean selectedVegetation = palette.equals(vegetation);
            boolean selectedWater = palette.equals(water);

            int background = hovered ? 0x66303030 : 0x33202020;
            if (selectedVegetation && selectedWater) {
                background = 0x884F5E24;
            } else if (selectedVegetation) {
                background = 0x88446A24;
            } else if (selectedWater) {
                background = 0x88305269;
            }

            int contentWidth = listWidth - 24;
            graphics.fill(listLeft + 2, rowTop + 2, listLeft + listWidth - 8, rowTop + ROW_HEIGHT - 2, background);
            graphics.drawString(font, trimToWidth(palette.displayName().getString(), contentWidth), listLeft + 8, rowTop + 6, 0xFFFFFF);
            graphics.drawString(font, trimToWidth(palette.id().toString(), contentWidth), listLeft + 8, rowTop + 19, 0xA8A8A8);
        }

        graphics.disableScissor();
        renderScrollBar(graphics);
    }

    private void renderScrollBar(GuiGraphics graphics) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        int barLeft = listLeft + listWidth - 6;
        int barHeight = Math.max(20, listHeight * visibleRows() / visibleBiomes.size());
        int barTop = listTop + (int) ((listHeight - barHeight) * (scrollOffset / (double) maxScroll));

        graphics.fill(barLeft, listTop + 2, barLeft + 4, listTop + listHeight - 2, 0x66404040);
        graphics.fill(barLeft, barTop, barLeft + 4, barTop + barHeight, 0xCCB0B0B0);
    }

    private void selectBiome(BiomePalette palette) {
        if (activeTarget == ActiveTarget.VEGETATION) {
            vegetation = palette;
        } else {
            water = palette;
        }
    }

    private Component vegetationLabel() {
        return choiceLabel("screen.colorbiome.palette_brush.vegetation", vegetation);
    }

    private Component waterLabel() {
        return choiceLabel("screen.colorbiome.palette_brush.water", water);
    }

    private Component choiceLabel(String translationKey, BiomePalette palette) {
        String text = Component.translatable(translationKey, palette.displayName()).getString();
        return Component.literal(trimToWidth(text, LEFT_PANEL_WIDTH - 36));
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return "";
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    private int visibleRows() {
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, visibleBiomes.size() - visibleRows());
    }

    private boolean isInList(double mouseX, double mouseY) {
        return mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight;
    }

    private boolean isInVegetationControl(double mouseX, double mouseY) {
        return isInControl(mouseX, mouseY, leftPanelLeft + 10, vegetationControlTop(), LEFT_PANEL_WIDTH - 20);
    }

    private boolean isInWaterControl(double mouseX, double mouseY) {
        return isInControl(mouseX, mouseY, leftPanelLeft + 10, waterControlTop(), LEFT_PANEL_WIDTH - 20);
    }

    private boolean isInConfirmControl(double mouseX, double mouseY) {
        return isInControl(mouseX, mouseY, leftPanelLeft + 35, confirmControlTop(), 120);
    }

    private boolean isInCeilingToggle(double mouseX, double mouseY) {
        return isInToggle(mouseX, mouseY, ceilingToggleTop());
    }

    private boolean isInFloorToggle(double mouseX, double mouseY) {
        return isInToggle(mouseX, mouseY, floorToggleTop());
    }

    private boolean isInControl(double mouseX, double mouseY, int left, int top, int controlWidth) {
        return mouseX >= left && mouseX < left + controlWidth && mouseY >= top && mouseY < top + CONTROL_HEIGHT;
    }

    private boolean isInToggle(double mouseX, double mouseY, int top) {
        return mouseX >= leftPanelLeft + 10 && mouseX < leftPanelLeft + LEFT_PANEL_WIDTH - 10
                && mouseY >= top && mouseY < top + 12;
    }

    private int vegetationControlTop() {
        return panelTop + 48;
    }

    private int waterControlTop() {
        return panelTop + 74;
    }

    private int confirmControlTop() {
        return panelTop + Math.min(PANEL_HEIGHT, height - 24) - 31;
    }

    private int aboveFieldTop() {
        return panelTop + 112;
    }

    private int belowFieldTop() {
        return panelTop + 135;
    }

    private int ceilingToggleTop() {
        return panelTop + 158;
    }

    private int floorToggleTop() {
        return panelTop + 174;
    }

    private int heightFieldLeft() {
        return leftPanelLeft + 59;
    }

    private int heightFieldWidth() {
        return LEFT_PANEL_WIDTH - 69;
    }

    private boolean isValidDistanceInput(String value) {
        return value.isEmpty() || value.matches("\\d{1,5}");
    }

    private int distanceFrom(EditBox field) {
        if (field == null || field.getValue().isEmpty()) {
            return 0;
        }
        try {
            return clamp(Integer.parseInt(field.getValue()), 0, MAX_VERTICAL_DISTANCE);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void updateHeightInputState() {
        if (aboveBox != null) {
            aboveBox.active = !toWorldCeiling;
        }
        if (belowBox != null) {
            belowBox.active = !toWorldFloor;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendSettings() {
        PacketDistributor.sendToServer(new UpdateBrushSettingsPayload(vegetation, water, distanceFrom(aboveBox),
                distanceFrom(belowBox), toWorldCeiling, toWorldFloor));
    }

    private enum ActiveTarget {
        VEGETATION("screen.colorbiome.palette_brush.active_vegetation"),
        WATER("screen.colorbiome.palette_brush.active_water");

        private final String translationKey;

        ActiveTarget(String translationKey) {
            this.translationKey = translationKey;
        }

        private Component label() {
            return Component.translatable(translationKey);
        }
    }
}
