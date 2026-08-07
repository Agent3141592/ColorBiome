package com.agent314.colorbiome;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public record BiomePalette(ResourceKey<Biome> biomeKey) {
    public static final BiomePalette PLAINS = new BiomePalette(Biomes.PLAINS);

    public ResourceLocation id() {
        return biomeKey.location();
    }

    public String serializedName() {
        return id().toString();
    }

    public Component displayName() {
        ResourceLocation id = id();
        return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
    }

    public Biome biome(Level level) {
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        return registry.getOptional(biomeKey).orElseGet(() -> registry.getOrThrow(Biomes.PLAINS));
    }

    public static BiomePalette fromLocation(ResourceLocation id) {
        return new BiomePalette(ResourceKey.create(Registries.BIOME, id));
    }

    public static BiomePalette fromStoredId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? PLAINS : fromLocation(id);
    }

    public static void write(RegistryFriendlyByteBuf buffer, BiomePalette palette) {
        buffer.writeResourceLocation(palette.id());
    }

    public static BiomePalette read(RegistryFriendlyByteBuf buffer) {
        return fromLocation(buffer.readResourceLocation());
    }
}
