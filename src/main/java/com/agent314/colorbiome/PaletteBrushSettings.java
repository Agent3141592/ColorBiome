package com.agent314.colorbiome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public final class PaletteBrushSettings {
    private static final Map<UUID, Settings> SETTINGS = new HashMap<>();

    private PaletteBrushSettings() {
    }

    public static void update(ServerPlayer player, BiomePalette vegetation, BiomePalette water, int above, int below,
            boolean toWorldCeiling, boolean toWorldFloor) {
        if (player.getMainHandItem().is(ColorBiome.PALETTE_BRUSH.get())
                || player.getOffhandItem().is(ColorBiome.PALETTE_BRUSH.get())) {
            SETTINGS.put(player.getUUID(), new Settings(vegetation, water, above, below, toWorldCeiling, toWorldFloor));
            PaletteBrushZones.apply(player, vegetation, water, above, below, toWorldCeiling, toWorldFloor);
        }
    }

    public record Settings(BiomePalette vegetation, BiomePalette water, int above, int below, boolean toWorldCeiling,
            boolean toWorldFloor) {
    }
}
