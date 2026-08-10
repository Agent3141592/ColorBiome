package com.agent314.colorbiome.mixin;

import com.agent314.colorbiome.BiomePalette;
import com.agent314.colorbiome.PaletteBrushRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true)
    private static void colorbiome$overrideGrassColor(BlockAndTintGetter level, BlockPos position,
            CallbackInfoReturnable<Integer> callback) {
        BiomePalette palette = vegetationPaletteAt(position);
        if (palette != null) {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            callback.setReturnValue(palette.biome(clientLevel).getGrassColor(position.getX(), position.getZ()));
        }
    }

    @Inject(method = "getAverageFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void colorbiome$overrideFoliageColor(BlockAndTintGetter level, BlockPos position,
            CallbackInfoReturnable<Integer> callback) {
        BiomePalette palette = vegetationPaletteAt(position);
        if (palette != null) {
            callback.setReturnValue(palette.biome(Minecraft.getInstance().level).getFoliageColor());
        }
    }

    @Inject(method = "getAverageDryFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void colorbiome$overrideDryFoliageColor(BlockAndTintGetter level, BlockPos position,
            CallbackInfoReturnable<Integer> callback) {
        BiomePalette palette = vegetationPaletteAt(position);
        if (palette != null) {
            callback.setReturnValue(palette.biome(Minecraft.getInstance().level).getDryFoliageColor());
        }
    }

    @Inject(method = "getAverageWaterColor", at = @At("HEAD"), cancellable = true)
    private static void colorbiome$overrideWaterColor(BlockAndTintGetter level, BlockPos position,
            CallbackInfoReturnable<Integer> callback) {
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return;
        }

        BiomePalette palette = PaletteBrushRenderState.waterAt(clientLevel, position);
        if (palette != null) {
            callback.setReturnValue(palette.biome(clientLevel).getWaterColor());
        }
    }

    private static BiomePalette vegetationPaletteAt(BlockPos position) {
        ClientLevel clientLevel = Minecraft.getInstance().level;
        return clientLevel == null ? null : PaletteBrushRenderState.vegetationAt(clientLevel, position);
    }
}
