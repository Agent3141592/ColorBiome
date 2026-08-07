package com.agent314.colorbiome;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ColorBiome.MODID, value = Dist.CLIENT)
public final class PaletteBrushClientEvents {
    private PaletteBrushClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (!event.getItemStack().is(ColorBiome.PALETTE_BRUSH.get())) {
            return;
        }

        Minecraft.getInstance().setScreen(new PaletteBrushScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
