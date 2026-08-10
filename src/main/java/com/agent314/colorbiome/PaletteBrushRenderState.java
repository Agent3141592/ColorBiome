package com.agent314.colorbiome;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class PaletteBrushRenderState {
    private static final List<PaletteBrushZone> ZONES = new ArrayList<>();
    private static boolean renderRefreshNeeded;

    private PaletteBrushRenderState() {
    }

    public static void addZone(PaletteBrushZone zone) {
        ZONES.add(zone);
        renderRefreshNeeded = true;
    }

    public static void replaceZones(List<PaletteBrushZone> zones) {
        ZONES.clear();
        ZONES.addAll(zones);
        renderRefreshNeeded = true;
    }

    public static void clear() {
        if (!ZONES.isEmpty()) {
            ZONES.clear();
            renderRefreshNeeded = true;
        }
    }

    public static BiomePalette vegetationAt(Level level, BlockPos position) {
        for (int index = ZONES.size() - 1; index >= 0; index--) {
            PaletteBrushZone zone = ZONES.get(index);
            if (zone.contains(level.dimension().identifier(), position.getX(), position.getY(), position.getZ())) {
                return zone.vegetation();
            }
        }
        return null;
    }

    public static BiomePalette waterAt(Level level, BlockPos position) {
        for (int index = ZONES.size() - 1; index >= 0; index--) {
            PaletteBrushZone zone = ZONES.get(index);
            if (zone.contains(level.dimension().identifier(), position.getX(), position.getY(), position.getZ())) {
                return zone.water();
            }
        }
        return null;
    }

    public static boolean consumeRenderRefreshNeeded() {
        boolean needed = renderRefreshNeeded;
        renderRefreshNeeded = false;
        return needed;
    }
}
