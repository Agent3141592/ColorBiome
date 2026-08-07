package com.agent314.colorbiome;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ColorBiome.MODID)
public class ColorBiome {
    public static final String MODID = "colorbiome";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<PaletteBrushItem> PALETTE_BRUSH = ITEMS.registerItem("palette_brush", PaletteBrushItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PALETTE_BRUSH_TAB = CREATIVE_MODE_TABS.register("colorbiome", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.colorbiome"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> PALETTE_BRUSH.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(PALETTE_BRUSH.get()))
            .build());

    public ColorBiome(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPayloadHandlers);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(PaletteBrushEvents.class);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UpdateBrushSettingsPayload.TYPE, UpdateBrushSettingsPayload.STREAM_CODEC,
                UpdateBrushSettingsPayload::handle);
        registrar.playToClient(AddPaletteBrushZonePayload.TYPE, AddPaletteBrushZonePayload.STREAM_CODEC,
                AddPaletteBrushZonePayload::handle);
        registrar.playToClient(SyncPaletteBrushZonesPayload.TYPE, SyncPaletteBrushZonesPayload.STREAM_CODEC,
                SyncPaletteBrushZonesPayload::handle);
    }

}
