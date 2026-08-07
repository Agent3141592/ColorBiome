package com.agent314.colorbiome;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

public final class PaletteBrushEvents {
    private PaletteBrushEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !event.getItemStack().is(ColorBiome.PALETTE_BRUSH.get())) {
            return;
        }

        event.setCanceled(true);

        if (!event.getLevel().isClientSide()) {
            PaletteBrushSelection.setFirstCorner(event.getEntity(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ColorBiome.PALETTE_BRUSH.get())) {
            return;
        }

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.TRUE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaletteBrushZones.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaletteBrushZones.syncTo(player, event.getTo().location());
        }
    }
}
