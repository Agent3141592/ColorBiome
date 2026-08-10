package com.agent314.colorbiome;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public record BiomePalette(ResourceKey<Biome> biomeKey) {
    public static final BiomePalette PLAINS = new BiomePalette(Biomes.PLAINS);

    public Identifier id() {
        return biomeKey.identifier();
    }

    public String serializedName() {
        return id().toString();
    }

    public Component displayName() {
        Identifier id = id();
        return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
    }

    public Biome biome(Level level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        return registry.getOptional(biomeKey).orElseGet(() -> registry.getOrThrow(Biomes.PLAINS).value());
    }

    public static BiomePalette fromLocation(Identifier id) {
        return new BiomePalette(ResourceKey.create(Registries.BIOME, id));
    }

    public static BiomePalette fromStoredId(String value) {
        Identifier id = Identifier.tryParse(value);
        return id == null ? PLAINS : fromLocation(id);
    }

    public static void write(RegistryFriendlyByteBuf buffer, BiomePalette palette) {
        buffer.writeIdentifier(palette.id());
    }

    public static BiomePalette read(RegistryFriendlyByteBuf buffer) {
        return fromLocation(buffer.readIdentifier());
    }
}
