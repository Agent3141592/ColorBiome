package com.agent314.colorbiome;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateBrushSettingsPayload(BiomePalette vegetation, BiomePalette water, int above, int below,
        boolean toWorldCeiling, boolean toWorldFloor) implements CustomPacketPayload {
    public static final Type<UpdateBrushSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColorBiome.MODID, "update_brush_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBrushSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                BiomePalette.write(buffer, payload.vegetation());
                BiomePalette.write(buffer, payload.water());
                buffer.writeVarInt(payload.above());
                buffer.writeVarInt(payload.below());
                buffer.writeBoolean(payload.toWorldCeiling());
                buffer.writeBoolean(payload.toWorldFloor());
            },
            buffer -> new UpdateBrushSettingsPayload(BiomePalette.read(buffer), BiomePalette.read(buffer),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<UpdateBrushSettingsPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateBrushSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PaletteBrushSettings.update(player, payload.vegetation(), payload.water(), payload.above(), payload.below(),
                        payload.toWorldCeiling(), payload.toWorldFloor());
            }
        });
    }
}
