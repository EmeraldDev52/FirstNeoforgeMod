package com.emeraldpig.emfirstmod.datagen;

import com.emeraldpig.emfirstmod.EmFirstMod;
import com.emeraldpig.emfirstmod.item.ModItems;
import com.emeraldpig.emfirstmod.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, EmFirstMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.MAGICALLY_CONVERTABLE)
                .add(ModItems.BISMUTH.get())
                .add(Items.COMPASS);

        tag(ItemTags.SWORDS)
                .add(ModItems.BISMUTH_SWORD.get());

        tag(ItemTags.PICKAXES)
                .add(ModItems.BISMUTH_PICKAXE.get());

        tag(ItemTags.SHOVELS)
                .add(ModItems.BISMUTH_SHOVEL.get());

        tag(ItemTags.AXES)
                .add(ModItems.BISMUTH_AXE.get());

        tag(ItemTags.HOES)
                .add(ModItems.BISMUTH_HOE.get());

        tag(ModTags.Items.VEINMINE_ENCHANTABLE)
                .addTag(ItemTags.PICKAXES)
                .addTag(ItemTags.AXES);

        tag(net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", "enchantable/mining")))
                .addTag(ModTags.Items.VEINMINE_ENCHANTABLE);

    }
}
