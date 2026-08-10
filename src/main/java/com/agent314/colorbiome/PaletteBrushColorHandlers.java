package com.agent314.colorbiome;

import java.util.List;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class PaletteBrushColorHandlers {
    private PaletteBrushColorHandlers() {
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(vegetationTint(true)),
                Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.SHORT_GRASS, Blocks.POTTED_FERN,
                Blocks.LARGE_FERN, Blocks.TALL_GRASS, Blocks.SUGAR_CANE);
        event.register(List.of(vegetationTint(false)),
                Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
        event.register(List.of(waterTint()),
                Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON);
    }

    private static BlockTintSource vegetationTint(boolean grass) {
        return new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return grass ? GrassColor.getDefaultColor() : defaultFoliageColor(state);
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
                return vegetationColor(state, level, position, grass);
            }
        };
    }

    private static BlockTintSource waterTint() {
        return new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return -1;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
                return waterColor(level, position);
            }
        };
    }

    private static int vegetationColor(BlockState state, BlockAndTintGetter level, BlockPos position, boolean grass) {
        if (position != null) {
            ClientLevel world = Minecraft.getInstance().level;
            BiomePalette palette = world == null ? null : PaletteBrushRenderState.vegetationAt(world, position);
            if (palette != null) {
                return grass ? palette.biome(world).getGrassColor(position.getX(), position.getZ())
                        : palette.biome(world).getFoliageColor();
            }
            return grass ? BiomeColors.getAverageGrassColor(level, position) : BiomeColors.getAverageFoliageColor(level, position);
        }
        return grass ? GrassColor.getDefaultColor() : defaultFoliageColor(state);
    }

    private static int defaultFoliageColor(BlockState state) {
        if (state.is(Blocks.SPRUCE_LEAVES)) {
            return FoliageColor.FOLIAGE_EVERGREEN;
        }
        if (state.is(Blocks.BIRCH_LEAVES)) {
            return FoliageColor.FOLIAGE_BIRCH;
        }
        if (state.is(Blocks.MANGROVE_LEAVES)) {
            return FoliageColor.FOLIAGE_MANGROVE;
        }
        return FoliageColor.FOLIAGE_DEFAULT;
    }

    private static int waterColor(BlockAndTintGetter level, BlockPos position) {
        if (position != null) {
            ClientLevel world = Minecraft.getInstance().level;
            BiomePalette palette = world == null ? null : PaletteBrushRenderState.waterAt(world, position);
            return palette != null ? palette.biome(world).getWaterColor() : BiomeColors.getAverageWaterColor(level, position);
        }
        return -1;
    }
}
