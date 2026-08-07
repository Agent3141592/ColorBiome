package com.agent314.colorbiome;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record PaletteBrushZone(ResourceLocation dimension, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
        BiomePalette vegetation, BiomePalette water) {
    public boolean contains(ResourceLocation currentDimension, int x, int y, int z) {
        return dimension.equals(currentDimension)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public static void write(RegistryFriendlyByteBuf buffer, PaletteBrushZone zone) {
        buffer.writeUtf(zone.dimension().toString());
        buffer.writeInt(zone.minX());
        buffer.writeInt(zone.maxX());
        buffer.writeInt(zone.minY());
        buffer.writeInt(zone.maxY());
        buffer.writeInt(zone.minZ());
        buffer.writeInt(zone.maxZ());
        BiomePalette.write(buffer, zone.vegetation());
        BiomePalette.write(buffer, zone.water());
    }

    public static PaletteBrushZone read(RegistryFriendlyByteBuf buffer) {
        return new PaletteBrushZone(
                ResourceLocation.parse(buffer.readUtf()),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                BiomePalette.read(buffer),
                BiomePalette.read(buffer));
    }
}
