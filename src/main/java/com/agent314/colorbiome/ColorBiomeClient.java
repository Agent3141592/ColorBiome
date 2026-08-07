package com.agent314.colorbiome;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ColorBiome.MODID, dist = Dist.CLIENT)
public class ColorBiomeClient {
    public ColorBiomeClient(IEventBus modEventBus) {
        modEventBus.addListener(PaletteBrushColorHandlers::registerBlockColors);
        NeoForge.EVENT_BUS.addListener(ColorBiomeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ColorBiomeClient::onClientLoggingIn);
        NeoForge.EVENT_BUS.addListener(ColorBiomeClient::onClientLoggingOut);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (PaletteBrushRenderState.consumeRenderRefreshNeeded() && Minecraft.getInstance().levelRenderer != null) {
            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PaletteBrushRenderState.clear();
    }

    static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PaletteBrushRenderState.clear();
    }
}
