package com.agent314.colorbiome;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PaletteBrushSelection {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private PaletteBrushSelection() {
    }

    public static void setFirstCorner(Player player, BlockPos position) {
        SELECTIONS.put(player.getUUID(), new Selection(player.level().dimension(), position.immutable(), null));
        player.sendSystemMessage(Component.literal("ColorBiome: corner 1 set: x=" + position.getX() + " z=" + position.getZ()));
    }

    public static void setSecondCorner(Player player, BlockPos position) {
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null) {
            player.sendSystemMessage(Component.literal("ColorBiome: set corner 1 with left click first."));
            return;
        }

        if (!selection.dimension().equals(player.level().dimension())) {
            player.sendSystemMessage(Component.literal("ColorBiome: corner 1 is in another dimension. Set it again with left click."));
            return;
        }

        int width = Math.abs(position.getX() - selection.firstCorner().getX()) + 1;
        int depth = Math.abs(position.getZ() - selection.firstCorner().getZ()) + 1;

        SELECTIONS.put(player.getUUID(), new Selection(selection.dimension(), selection.firstCorner(), position.immutable()));

        player.sendSystemMessage(Component.literal("ColorBiome: corner 2 set: x=" + position.getX() + " z=" + position.getZ()));
        player.sendSystemMessage(Component.literal("ColorBiome: selected surface area: " + width + " x " + depth + " blocks."));
    }

    public static Optional<SurfaceSelection> getCompletedSelection(Player player) {
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || selection.secondCorner() == null) {
            return Optional.empty();
        }

        return Optional.of(new SurfaceSelection(
                selection.dimension(),
                Math.min(selection.firstCorner().getX(), selection.secondCorner().getX()),
                Math.max(selection.firstCorner().getX(), selection.secondCorner().getX()),
                Math.min(selection.firstCorner().getY(), selection.secondCorner().getY()),
                Math.max(selection.firstCorner().getY(), selection.secondCorner().getY()),
                Math.min(selection.firstCorner().getZ(), selection.secondCorner().getZ()),
                Math.max(selection.firstCorner().getZ(), selection.secondCorner().getZ())));
    }

    private record Selection(ResourceKey<Level> dimension, BlockPos firstCorner, BlockPos secondCorner) {
    }

    public record SurfaceSelection(ResourceKey<Level> dimension, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    }
}
