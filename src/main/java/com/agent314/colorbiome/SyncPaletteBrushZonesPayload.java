package com.agent314.colorbiome;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPaletteBrushZonesPayload(List<PaletteBrushZone> zones) implements CustomPacketPayload {
    public static final Type<SyncPaletteBrushZonesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColorBiome.MODID, "sync_palette_brush_zones"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPaletteBrushZonesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.zones().size());
                payload.zones().forEach(zone -> PaletteBrushZone.write(buffer, zone));
            },
            buffer -> {
                int count = buffer.readVarInt();
                List<PaletteBrushZone> zones = new ArrayList<>(count);

                for (int index = 0; index < count; index++) {
                    zones.add(PaletteBrushZone.read(buffer));
                }

                return new SyncPaletteBrushZonesPayload(zones);
            });

    public SyncPaletteBrushZonesPayload {
        zones = List.copyOf(zones);
    }

    @Override
    public Type<SyncPaletteBrushZonesPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPaletteBrushZonesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PaletteBrushRenderState.replaceZones(payload.zones()));
    }
}
