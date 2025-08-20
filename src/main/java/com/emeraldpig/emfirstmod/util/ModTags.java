package com.emeraldpig.emfirstmod.util;

import com.emeraldpig.emfirstmod.EmFirstMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_BISMUTH_TOOL = createTag("needs_bismuth_tool");
        public static final TagKey<Block> INCORRECT_FOR_BISMUTH_TOOL = createTag("incorrect_for_bismuth_tool");
        public static final TagKey<Block> VEINMINE_PICKAXE = createTag("veinmine_pickaxe");
        public static final TagKey<Block> VEINMINE_AXE = createTag("veinmine_axe");
        public static final TagKey<Block> AUTO_SMELTABLE = createTag("auto_smeltable");

        public static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(EmFirstMod.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> MAGICALLY_CONVERTABLE = createTag("magic_convertable_items");
        public static final TagKey<Item> VEINMINE_ENCHANTABLE = createTag("veinmine_enchantable");

        public static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(EmFirstMod.MOD_ID, name));
        }
    }
}