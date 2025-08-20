package com.emeraldpig.emfirstmod.item;

import com.emeraldpig.emfirstmod.util.ModTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    public static final Tier BISMUTH = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_BISMUTH_TOOL,
            4000, 10f, 3f, 20, () -> Ingredient.of(ModItems.BISMUTH));
}
