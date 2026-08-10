package com.agent314.colorbiome;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PaletteBrushSavedData extends SavedData {
    private static final String FILE_ID = ColorBiome.MODID + "_palette_zones";
    private static final String ZONES_TAG = "zones";
    private static final SavedDataType<PaletteBrushSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ColorBiome.MODID, FILE_ID), PaletteBrushSavedData::new,
            CompoundTag.CODEC.xmap(PaletteBrushSavedData::load, PaletteBrushSavedData::save));

    private final List<PaletteBrushZone> zones = new ArrayList<>();

    public static PaletteBrushSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static PaletteBrushSavedData load(CompoundTag tag) {
        PaletteBrushSavedData data = new PaletteBrushSavedData();
        ListTag zonesTag = tag.getListOrEmpty(ZONES_TAG);

        for (int index = 0; index < zonesTag.size(); index++) {
            data.zones.add(loadZone(zonesTag.getCompoundOrEmpty(index)));
        }

        return data;
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag zonesTag = new ListTag();

        for (PaletteBrushZone zone : zones) {
            zonesTag.add(saveZone(zone));
        }

        tag.put(ZONES_TAG, zonesTag);
        return tag;
    }

    public void addZone(PaletteBrushZone zone) {
        zones.add(zone);
        setDirty();
    }

    public List<PaletteBrushZone> zonesFor(Identifier dimension) {
        return zones.stream()
                .filter(zone -> zone.dimension().equals(dimension))
                .toList();
    }

    private static CompoundTag saveZone(PaletteBrushZone zone) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", zone.dimension().toString());
        tag.putInt("min_x", zone.minX());
        tag.putInt("max_x", zone.maxX());
        tag.putInt("min_y", zone.minY());
        tag.putInt("max_y", zone.maxY());
        tag.putInt("min_z", zone.minZ());
        tag.putInt("max_z", zone.maxZ());
        tag.putString("vegetation", zone.vegetation().serializedName());
        tag.putString("water", zone.water().serializedName());
        return tag;
    }

    private static PaletteBrushZone loadZone(CompoundTag tag) {
        return new PaletteBrushZone(
                Identifier.parse(tag.getStringOr("dimension", "minecraft:overworld")),
                tag.getIntOr("min_x", 0), tag.getIntOr("max_x", 0),
                tag.getIntOr("min_y", 0), tag.getIntOr("max_y", 0),
                tag.getIntOr("min_z", 0), tag.getIntOr("max_z", 0),
                BiomePalette.fromStoredId(tag.getStringOr("vegetation", "minecraft:plains")),
                BiomePalette.fromStoredId(tag.getStringOr("water", "minecraft:plains")));
    }
}
