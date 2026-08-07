package com.agent314.colorbiome;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PaletteBrushZones {
    private static final int MAX_VERTICAL_DISTANCE = 10_000;

    private PaletteBrushZones() {
    }

    public static void apply(ServerPlayer player, BiomePalette vegetation, BiomePalette water, int above, int below,
            boolean toWorldCeiling, boolean toWorldFloor) {
        PaletteBrushSelection.getCompletedSelection(player).ifPresentOrElse(selection -> {
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }

            int safeAbove = clampDistance(above);
            int safeBelow = clampDistance(below);
            int minY = toWorldFloor ? level.getMinBuildHeight() : Math.max(level.getMinBuildHeight(), selection.minY() - safeBelow);
            int maxY = toWorldCeiling ? level.getMaxBuildHeight() - 1 : Math.min(level.getMaxBuildHeight() - 1, selection.maxY() + safeAbove);
            PaletteBrushZone zone = new PaletteBrushZone(selection.dimension().location(), selection.minX(), selection.maxX(),
                    minY, maxY, selection.minZ(), selection.maxZ(), vegetation, water);
            PaletteBrushSavedData.get(level.getServer()).addZone(zone);
            PacketDistributor.sendToPlayersInDimension(level, new AddPaletteBrushZonePayload(zone));
            player.sendSystemMessage(Component.literal("ColorBiome: palette applied to the selected area."));
        }, () -> player.sendSystemMessage(Component.literal("ColorBiome: select both corners before confirming the palette.")));
    }

    public static void syncTo(ServerPlayer player) {
        syncTo(player, player.level().dimension().location());
    }

    public static void syncTo(ServerPlayer player, ResourceLocation dimension) {
        PacketDistributor.sendToPlayer(player,
                new SyncPaletteBrushZonesPayload(PaletteBrushSavedData.get(player.server).zonesFor(dimension)));
    }

    private static int clampDistance(int distance) {
        return Math.max(0, Math.min(MAX_VERTICAL_DISTANCE, distance));
    }
}
