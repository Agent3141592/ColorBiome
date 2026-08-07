package com.agent314.colorbiome.mixin;

import com.agent314.colorbiome.BiomePalette;
import com.agent314.colorbiome.PaletteBrushRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
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
}
