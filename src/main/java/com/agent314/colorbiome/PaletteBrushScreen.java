package com.agent314.colorbiome;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class PaletteBrushScreen extends Screen {
    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 300;
    private static final int LEFT_PANEL_WIDTH = 190;
    private static final int GAP = 12;
    private static final int ROW_HEIGHT = 34;
    private static final int BIOMES_PER_PAGE = 6;
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
    private int scrollOffset;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int leftPanelLeft;
    private int rightPanelLeft;
    private int rightPanelWidth;
    private int listLeft;
    private int listTop;
    private int listWidth;

    private EditBox searchBox;
    private EditBox aboveBox;
    private EditBox belowBox;
    private PaletteButton vegetationButton;
    private PaletteButton waterButton;
    private PaletteButton ceilingButton;
    private PaletteButton floorButton;
    private final PaletteButton[] biomeButtons = new PaletteButton[BIOMES_PER_PAGE];

    public PaletteBrushScreen() {
        super(Component.translatable("screen.colorbiome.palette_brush.title"));
    }

    @Override
    protected void init() {
        loadBiomes();

        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelHeight = Math.min(PANEL_HEIGHT, height - 24);
        panelTop = (height - panelHeight) / 2;
        leftPanelLeft = panelLeft + 12;
        rightPanelLeft = leftPanelLeft + LEFT_PANEL_WIDTH + GAP;
        rightPanelWidth = panelWidth - LEFT_PANEL_WIDTH - GAP - 24;
        listLeft = rightPanelLeft + 10;
        listTop = panelTop + 62;
        listWidth = rightPanelWidth - 20;

        vegetationButton = addPaletteButton(vegetationLabel(), leftPanelLeft + 10, panelTop + 48, LEFT_PANEL_WIDTH - 20, 20,
                button -> {
                    activeTarget = ActiveTarget.VEGETATION;
                    refreshSelectionButtons();
                });
        waterButton = addPaletteButton(waterLabel(), leftPanelLeft + 10, panelTop + 74, LEFT_PANEL_WIDTH - 20, 20,
                button -> {
                    activeTarget = ActiveTarget.WATER;
                    refreshSelectionButtons();
                });

        aboveBox = addRenderableWidget(new EditBox(font, leftPanelLeft + 69, panelTop + 112, LEFT_PANEL_WIDTH - 79, 18,
                Component.translatable("screen.colorbiome.palette_brush.above")));
        aboveBox.setValue(Integer.toString(lastAbove));
        aboveBox.setFilter(this::isValidDistanceInput);

        belowBox = addRenderableWidget(new EditBox(font, leftPanelLeft + 69, panelTop + 135, LEFT_PANEL_WIDTH - 79, 18,
                Component.translatable("screen.colorbiome.palette_brush.below")));
        belowBox.setValue(Integer.toString(lastBelow));
        belowBox.setFilter(this::isValidDistanceInput);

        ceilingButton = addPaletteButton(Component.empty(), leftPanelLeft + 10, panelTop + 157, LEFT_PANEL_WIDTH - 20, 12,
                button -> {
                    toWorldCeiling = !toWorldCeiling;
                    refreshHeightControls();
                });
        floorButton = addPaletteButton(Component.empty(), leftPanelLeft + 10, panelTop + 172, LEFT_PANEL_WIDTH - 20, 12,
                button -> {
                    toWorldFloor = !toWorldFloor;
                    refreshHeightControls();
                });

        addPaletteButton(Component.translatable("screen.colorbiome.palette_brush.confirm"), leftPanelLeft + 35,
                panelTop + panelHeight - 31, 120, 20, button -> {
                    rememberSettings();
                    sendSettings();
                    onClose();
                });

        searchBox = addRenderableWidget(new EditBox(font, listLeft, panelTop + 30, listWidth, 20,
                Component.translatable("screen.colorbiome.palette_brush.search")));
        searchBox.setHint(Component.translatable("screen.colorbiome.palette_brush.search"));
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            updateVisibleBiomes();
            refreshBiomeButtons();
        });

        for (int row = 0; row < BIOMES_PER_PAGE; row++) {
            int selectedRow = row;
            biomeButtons[row] = addPaletteButton(Component.empty(), listLeft, listTop + row * ROW_HEIGHT, listWidth - 8,
                    ROW_HEIGHT - 2, button -> selectBiome(selectedRow));
        }

        updateVisibleBiomes();
        refreshHeightControls();
        refreshSelectionButtons();
        refreshBiomeButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawPanels(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInList(mouseX, mouseY) && maxScroll() > 0) {
            int direction = scrollY > 0 ? -3 : 3;
            scrollOffset = clamp(scrollOffset + direction, 0, maxScroll());
            refreshBiomeButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawPanels(GuiGraphicsExtractor graphics) {
        int panelRight = panelLeft + Math.min(PANEL_WIDTH, width - 24);
        int panelBottom = panelTop + panelHeight;
        int leftPanelRight = leftPanelLeft + LEFT_PANEL_WIDTH;
        int rightPanelRight = rightPanelLeft + rightPanelWidth;

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xDD101010);
        drawPanel(graphics, leftPanelLeft, panelTop + 21, leftPanelRight, panelBottom - 12);
        drawPanel(graphics, rightPanelLeft, panelTop + 21, rightPanelRight, panelBottom - 12);
        drawPanel(graphics, listLeft, listTop, listLeft + listWidth, listTop + visibleRows() * ROW_HEIGHT - 2);
        drawScrollBar(graphics);
        graphics.centeredText(font, title, width / 2, panelTop + 8, 0xFFFFFFFF);
        graphics.text(font, activeTarget == ActiveTarget.VEGETATION
                ? Component.translatable("screen.colorbiome.palette_brush.active_vegetation")
                : Component.translatable("screen.colorbiome.palette_brush.active_water"), leftPanelLeft + 10, panelTop + 32, 0xFFE0E0E0);
        graphics.text(font, Component.translatable("screen.colorbiome.palette_brush.height"), leftPanelLeft + 10, panelTop + 102,
                0xFFC8C8C8);
        graphics.text(font, Component.translatable("screen.colorbiome.palette_brush.above"), leftPanelLeft + 10, panelTop + 117,
                0xFFE0E0E0);
        graphics.text(font, Component.translatable("screen.colorbiome.palette_brush.below"), leftPanelLeft + 10, panelTop + 140,
                0xFFE0E0E0);
        graphics.text(font, Component.translatable("screen.colorbiome.palette_brush.biomes"), listLeft, panelTop + 16, 0xFFE0E0E0);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF606060);
        graphics.fill(left, top, right, bottom, 0xDD202020);
    }

    private PaletteButton addPaletteButton(Component label, int x, int y, int buttonWidth, int buttonHeight,
            Button.OnPress onPress) {
        return addRenderableWidget(new PaletteButton(x, y, buttonWidth, buttonHeight, label, onPress));
    }

    private void loadBiomes() {
        if (Minecraft.getInstance().level == null) {
            allBiomes = List.of(BiomePalette.PLAINS);
            visibleBiomes = allBiomes;
            return;
        }

        allBiomes = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME).registryKeySet().stream()
                .map(BiomePalette::new).sorted(Comparator.comparing(palette -> palette.id().toString())).toList();

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
        visibleBiomes = query.isEmpty() ? allBiomes
                : allBiomes.stream().filter(palette -> matchesQuery(palette, query)).toList();
        scrollOffset = clamp(scrollOffset, 0, maxScroll());
    }

    private boolean matchesQuery(BiomePalette palette, String query) {
        return palette.id().toString().toLowerCase(Locale.ROOT).contains(query)
                || palette.displayName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void refreshSelectionButtons() {
        vegetationButton.setMessage(vegetationLabel());
        vegetationButton.setAccent(activeTarget == ActiveTarget.VEGETATION ? 0xFF426D2A : 0);
        waterButton.setMessage(waterLabel());
        waterButton.setAccent(activeTarget == ActiveTarget.WATER ? 0xFF2C5E74 : 0);
    }

    private void refreshHeightControls() {
        ceilingButton.setMessage(toggleLabel("screen.colorbiome.palette_brush.to_world_ceiling", toWorldCeiling));
        ceilingButton.setAccent(toWorldCeiling ? 0xFF6A8A48 : 0);
        floorButton.setMessage(toggleLabel("screen.colorbiome.palette_brush.to_world_floor", toWorldFloor));
        floorButton.setAccent(toWorldFloor ? 0xFF6A8A48 : 0);
        aboveBox.active = !toWorldCeiling;
        belowBox.active = !toWorldFloor;
    }

    private void refreshBiomeButtons() {
        int start = scrollOffset;
        for (int row = 0; row < BIOMES_PER_PAGE; row++) {
            int index = start + row;
            PaletteButton button = biomeButtons[row];
            button.visible = row < visibleRows() && index < visibleBiomes.size();
            if (button.visible) {
                BiomePalette palette = visibleBiomes.get(index);
                button.setMessage(Component.literal(trimToWidth(palette.displayName().getString(), listWidth - 24)));
                button.setDetail(trimToWidth(palette.id().toString(), listWidth - 24));
                button.setAccent(palette.equals(vegetation) ? 0xFF426D2A : palette.equals(water) ? 0xFF2C5E74 : 0);
            }
        }
    }

    private void selectBiome(int row) {
        int index = scrollOffset + row;
        if (index >= visibleBiomes.size()) {
            return;
        }
        if (activeTarget == ActiveTarget.VEGETATION) {
            vegetation = visibleBiomes.get(index);
        } else {
            water = visibleBiomes.get(index);
        }
        refreshSelectionButtons();
        refreshBiomeButtons();
    }

    private Component vegetationLabel() {
        return Component.literal("Vegetation: " + trimToWidth(vegetation.displayName().getString(), LEFT_PANEL_WIDTH - 36));
    }

    private Component waterLabel() {
        return Component.literal("Water: " + trimToWidth(water.displayName().getString(), LEFT_PANEL_WIDTH - 36));
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.literal((value ? "[x] " : "[ ] ") + Component.translatable(key).getString());
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = font.width("...");
        return maxWidth <= ellipsisWidth ? "" : font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    private int visibleRows() {
        int listHeight = panelHeight - 80;
        return Math.min(BIOMES_PER_PAGE, Math.max(1, listHeight / ROW_HEIGHT));
    }

    private int maxScroll() {
        return Math.max(0, visibleBiomes.size() - visibleRows());
    }

    private boolean isInList(double mouseX, double mouseY) {
        int listHeight = visibleRows() * ROW_HEIGHT - 2;
        return mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight;
    }

    private void drawScrollBar(GuiGraphicsExtractor graphics) {
        if (maxScroll() == 0) {
            return;
        }
        int listHeight = visibleRows() * ROW_HEIGHT - 2;
        int barLeft = listLeft + listWidth - 6;
        int barHeight = Math.max(20, listHeight * visibleRows() / visibleBiomes.size());
        int barTop = listTop + (int) ((listHeight - barHeight) * (scrollOffset / (double) maxScroll()));
        graphics.fill(barLeft, listTop + 2, barLeft + 4, listTop + listHeight - 2, 0x66404040);
        graphics.fill(barLeft, barTop, barLeft + 4, barTop + barHeight, 0xCCB0B0B0);
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

    private void rememberSettings() {
        lastVegetation = vegetation;
        lastWater = water;
        lastAbove = distanceFrom(aboveBox);
        lastBelow = distanceFrom(belowBox);
        lastToWorldCeiling = toWorldCeiling;
        lastToWorldFloor = toWorldFloor;
    }

    private void sendSettings() {
        ClientPacketDistributor.sendToServer(new UpdateBrushSettingsPayload(vegetation, water, distanceFrom(aboveBox),
                distanceFrom(belowBox), toWorldCeiling, toWorldFloor));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class PaletteButton extends Button {
        private int accentColor;
        private String detail = "";

        private PaletteButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        private void setAccent(int accentColor) {
            this.accentColor = accentColor;
        }

        private void setDetail(String detail) {
            this.detail = detail;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = active && isHoveredOrFocused();
            int borderColor = accentColor != 0 ? accentColor : hovered ? 0xFF7A7A7A : 0xFF505050;
            int background = active ? hovered ? 0xFF303030 : 0xFF181818 : 0xFF252525;
            graphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);

            if (detail.isEmpty()) {
                graphics.centeredText(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2,
                        getY() + (getHeight() - 8) / 2, active ? 0xFFF0F0F0 : 0xFFA0A0A0);
            } else {
                graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 8, getY() + 5, 0xFFFFFFFF);
                graphics.text(Minecraft.getInstance().font, detail, getX() + 8, getY() + 18, 0xFFA8A8A8);
            }
        }
    }

    private enum ActiveTarget {
        VEGETATION,
        WATER
    }
}
