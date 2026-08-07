package com.agent314.colorbiome;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PaletteBrushSavedData extends SavedData {
    private static final String FILE_ID = ColorBiome.MODID + "_palette_zones";
    private static final String ZONES_TAG = "zones";

    private final List<PaletteBrushZone> zones = new ArrayList<>();

    public static PaletteBrushSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    private static SavedData.Factory<PaletteBrushSavedData> factory() {
        return new SavedData.Factory<>(PaletteBrushSavedData::new, PaletteBrushSavedData::load);
    }

    private static PaletteBrushSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PaletteBrushSavedData data = new PaletteBrushSavedData();
        ListTag zonesTag = tag.getList(ZONES_TAG, Tag.TAG_COMPOUND);

        for (int index = 0; index < zonesTag.size(); index++) {
            data.zones.add(loadZone(zonesTag.getCompound(index)));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
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

    public List<PaletteBrushZone> zonesFor(ResourceLocation dimension) {
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
                ResourceLocation.parse(tag.getString("dimension")),
                tag.getInt("min_x"),
                tag.getInt("max_x"),
                tag.getInt("min_y"),
                tag.getInt("max_y"),
                tag.getInt("min_z"),
                tag.getInt("max_z"),
                BiomePalette.fromStoredId(tag.getString("vegetation")),
                BiomePalette.fromStoredId(tag.getString("water")));
    }
}
