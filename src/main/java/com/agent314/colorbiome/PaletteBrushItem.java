package com.agent314.colorbiome;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class PaletteBrushItem extends Item {
    public PaletteBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            PaletteBrushSelection.setSecondCorner(context.getPlayer(), context.getClickedPos());
        }

        return InteractionResult.SUCCESS;
    }
}
