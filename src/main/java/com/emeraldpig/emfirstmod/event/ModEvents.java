package com.emeraldpig.emfirstmod.event;

import com.emeraldpig.emfirstmod.EmFirstMod;
import com.emeraldpig.emfirstmod.enchantment.ModEnchantments;
import com.emeraldpig.emfirstmod.item.custom.HammerItem;
import com.emeraldpig.emfirstmod.util.ModTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import com.emeraldpig.emfirstmod.trade.TradeHelpers;

import java.util.*;

@EventBusSubscriber(modid = EmFirstMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {
    // Per-dimension reentrancy guard
    private static final java.util.Map<ServerLevel, LongOpenHashSet> HARVESTED = new WeakHashMap<>();
    private static LongOpenHashSet guard(ServerLevel lvl) {
        return HARVESTED.computeIfAbsent(lvl, k -> new LongOpenHashSet());
    }

    @SubscribeEvent
    public static void onHammerUsage(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer) {
            ServerLevel lvl = serverPlayer.serverLevel();
            BlockPos initialBlockPos = event.getPos();
            LongOpenHashSet g = guard(lvl);
            long initialKey = initialBlockPos.asLong();
            if (g.contains(initialKey)) return;

            for (BlockPos pos : HammerItem.getBlocksToBeDestroyed(1, initialBlockPos, serverPlayer)) {
                if (pos.equals(initialBlockPos) || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
                    continue;
                }

                long key = pos.asLong();
                if (!g.add(key)) continue;
                serverPlayer.gameMode.destroyBlock(pos);
                g.remove(key);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        var levelAcc = event.getLevel();
        if (!(levelAcc instanceof ServerLevel level)) return;

        // Block must be in your autosmelt tag
        var state = event.getState();
        if (state == null || !state.is(ModTags.Blocks.AUTO_SMELTABLE)) return;

        // Tool in hand (must have your AutoSmelt enchant)
        var breaker = event.getBreaker();
        if (!(breaker instanceof Player p)) return;
        ItemStack tool = p.getMainHandItem();
        if (tool.isEmpty()) return;

        // Lookup enchant by ResourceKey
        var enchReg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var autoSmeltHolder = enchReg.getHolder(ModEnchantments.AUTO_SMELT);
        if (autoSmeltHolder.isEmpty()
                || EnchantmentHelper.getItemEnchantmentLevel(autoSmeltHolder.get(), tool) <= 0) {
            return;
        }

        var recipes = level.getRecipeManager();

        // Replace each drop entity's stack with its smelted output (if a recipe exists)
        for (var ent : event.getDrops()) {
            var in = ent.getItem();
            if (in.isEmpty()) continue;

            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(in);
            var match = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
            if (match.isEmpty()) continue;

            var recipe = match.get().value();
            var outPerOne = recipe.getResultItem(level.registryAccess()).copy();
            if (outPerOne.isEmpty()) continue;

            outPerOne.setCount(outPerOne.getCount() * in.getCount());
            ent.setItem(outPerOne);
        }
    }

    private static void mergeInto(java.util.List<ItemStack> list, ItemStack add) {
        for (ItemStack ex : list) {
            if (ItemStack.isSameItemSameComponents(ex, add)) {
                int can = Math.min(add.getCount(), ex.getMaxStackSize() - ex.getCount());
                if (can > 0) {
                    ex.grow(can);
                    add.shrink(can);
                    if (add.isEmpty()) return;
                }
            }
        }
        list.add(add.copy());
    }




    @SubscribeEvent
    public static void onVeinMiner(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if (player instanceof ServerPlayer serverPlayer) {
            int enchantLevel = mainHandItem.getEnchantmentLevel(
                    event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(EmFirstMod.MOD_ID + ":vein_miner")))
            );
            if (enchantLevel <= 0) {
                return;
            }

            ServerLevel lvl = serverPlayer.serverLevel();
            BlockPos initialBlockPos = event.getPos();
            LongOpenHashSet g = guard(lvl);
            long initialKey = initialBlockPos.asLong();
            if (g.contains(initialKey)) return;

            TagKey<Block> tag;
            if (mainHandItem.is(ItemTags.PICKAXES)) {
                tag = ModTags.Blocks.VEINMINE_PICKAXE;
            } else if (mainHandItem.is(ItemTags.AXES)) {
                tag = ModTags.Blocks.VEINMINE_AXE;
            } else {
                return;
            }

            for (BlockPos pos : getBlocksToBeDestroyed(200, initialBlockPos, serverPlayer, tag)) {
                if (pos.equals(initialBlockPos) || !mainHandItem.isCorrectToolForDrops(event.getLevel().getBlockState(pos))) {
                    continue;
                }

                long key = pos.asLong();
                if (!g.add(key)) continue;
                serverPlayer.gameMode.destroyBlock(pos);
                g.remove(key);
            }
        }
    }





    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {

        TradeHelpers.addEnchantedBookTrade(
                event,
                VillagerProfession.LIBRARIAN,
                1,
                ModEnchantments.VEIN_MINER,
                1, 1,
                18,
                Items.BOOK, 1,   // Items.BOOK is an Item, but it implements ItemLike
                12, 5, 0.05f,
                0.05f
        );

        TradeHelpers.addEnchantedBookTrade(
                event,
                VillagerProfession.LIBRARIAN,
                1,
                ModEnchantments.AUTO_SMELT,
                1, 1,
                18,
                Items.BOOK, 1,   // Items.BOOK is an Item, but it implements ItemLike
                12, 5, 0.05f,
                0.05f
        );

        TradeHelpers.addEnchantedBookTrade(
                event,
                VillagerProfession.LIBRARIAN,
                1,
                ModEnchantments.LIGHTNING_STRIKER,
                1, 2,
                18,
                Items.BOOK, 1,   // Items.BOOK is an Item, but it implements ItemLike
                12, 5, 0.05f,
                0.05f
        );


    }

    // VEIN MINER HELPER
    private static final int[] NEIGH_OFFS;
    static {
        int[] tmp = new int[26 * 3];
        int k = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    tmp[k++] = dx; tmp[k++] = dy; tmp[k++] = dz;
                }
            }
        }
        NEIGH_OFFS = tmp;
    }

    public static List<BlockPos> getBlocksToBeDestroyed(int max, BlockPos start, ServerPlayer player, TagKey<Block> tag) {
        final int cap = max > 0 ? max : 200;
        final var level = player.serverLevel();

        final BlockState originState = level.getBlockState(start);
        if (!originState.is(tag)) return List.of();

        final Block originBlock = originState.getBlock();

        final long startKey = BlockPos.asLong(start.getX(), start.getY(), start.getZ());

        final var queue   = new LongArrayFIFOQueue(Math.min(cap, 512));
        final var visited = new LongOpenHashSet(Math.min(cap * 2, 4096));
        final var resultL = new LongArrayList(Math.min(cap, 2048));

        queue.enqueue(startKey);
        visited.add(startKey);
        resultL.add(startKey);

        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty() && resultL.size() < cap) {
            final long curKey = queue.dequeueLong();
            final int cx = BlockPos.getX(curKey);
            final int cy = BlockPos.getY(curKey);
            final int cz = BlockPos.getZ(curKey);

            for (int i = 0; i < NEIGH_OFFS.length; i += 3) {
                final int nx = cx + NEIGH_OFFS[i];
                final int ny = cy + NEIGH_OFFS[i + 1];
                final int nz = cz + NEIGH_OFFS[i + 2];

                m.set(nx, ny, nz);
                if (!level.isLoaded(m)) continue;

                final long nKey = BlockPos.asLong(nx, ny, nz);
                if (!visited.add(nKey)) continue;

                final BlockState s = level.getBlockState(m);
                // identical block AND in tag (strict)
                if (s.getBlock() != originBlock || !s.is(tag)) continue;

                queue.enqueue(nKey);
                resultL.add(nKey);
                if (resultL.size() >= cap) break;
            }
        }

        final var out = new ArrayList<BlockPos>(resultL.size());
        for (int i = 0; i < resultL.size(); i++) out.add(BlockPos.of(resultL.getLong(i)));
        return out;
    }
}
