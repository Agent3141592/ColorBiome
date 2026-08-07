package com.agent314.colorbiome;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AddPaletteBrushZonePayload(PaletteBrushZone zone) implements CustomPacketPayload {
    public static final Type<AddPaletteBrushZonePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColorBiome.MODID, "add_palette_brush_zone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddPaletteBrushZonePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> PaletteBrushZone.write(buffer, payload.zone()),
            buffer -> new AddPaletteBrushZonePayload(PaletteBrushZone.read(buffer)));

    @Override
    public Type<AddPaletteBrushZonePayload> type() {
        return TYPE;
    }

    public static void handle(AddPaletteBrushZonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PaletteBrushRenderState.addZone(payload.zone()));
    }
}
