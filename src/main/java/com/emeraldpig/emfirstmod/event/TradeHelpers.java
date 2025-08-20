package com.emeraldpig.emfirstmod.trade;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.Optional;

public final class TradeHelpers {
    private TradeHelpers() {}

    /**
     * General helper for *enchanted book* trades with per-roll chance.
     *
     * @param event        VillagerTradesEvent
     * @param profession   VillagerProfession
     * @param levelIndex   Villager level (1–5)
     * @param enchantKey   Enchantment key (custom or vanilla)
     * @param minLevel     Min enchant level (use same as max for exact)
     * @param maxLevel     Max enchant level
     * @param emeralds     Cost A: emerald count
     * @param costBItem    Optional cost B item (e.g., BOOK, LAPIS) – can be null to omit
     * @param costBCount   Cost B count (ignored if costBItem is null or <= 0)
     * @param maxUses      Max uses for the offer
     * @param villagerXp   XP granted to the villager
     * @param priceMult    Price multiplier
     * @param chance       Chance [0..1] that this trade appears on each roll
     */
    public static void addEnchantedBookTrade(
            VillagerTradesEvent event,
            VillagerProfession profession,
            int levelIndex,
            ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantKey,
            int minLevel, int maxLevel,
            int emeralds,
            Item costBItem, int costBCount,
            int maxUses, int villagerXp, float priceMult,
            float chance
    ) {
        if(event.getType() == profession) {
            event.getTrades().get(levelIndex).add((entity, rand) -> {
                if (!rollChance(rand, chance)) return null;

                int lvl = (minLevel == maxLevel) ? minLevel : Mth.nextInt(rand, minLevel, maxLevel);
                ItemStack result = makeEnchantedBook(event.getRegistryAccess(), enchantKey, lvl);

                ItemCost costA = new ItemCost(Items.EMERALD, emeralds);
                Optional<ItemCost> costB = (costBItem != null && costBCount > 0)
                        ? Optional.of(new ItemCost(costBItem, costBCount))
                        : Optional.empty();

                return new MerchantOffer(costA, costB, result, maxUses, villagerXp, priceMult);
            });
        }
    }


    /** Simple chance roll. */
    private static boolean rollChance(RandomSource rand, float chance) {
        if (chance <= 0f) return false;
        if (chance >= 1f) return true;
        return rand.nextFloat() < chance;
    }

    private static ItemStack makeEnchantedBook(
            RegistryAccess access,
            ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
            int level
    ) {

        var holder = access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, level)
        );
    }
}
