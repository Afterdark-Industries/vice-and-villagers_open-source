package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.CustomerService;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoMetLegacyTradeGameTests {
    private HoMetLegacyTradeGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void customerInventoryCountsAndRemovesLegacyAndCurrentBottles(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "homet-trade-test"));
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(0, new ItemStack(ModItems.HO_MET_5MG.get(), 3));
        inventory.setItem(1, new ItemStack(ModItems.HO_MET_25MG.get(), 2));
        inventory.setItem(2, new ItemStack(ModItems.HO_MET_10MG.get(), 7));
        inventory.setItem(3, new ItemStack(Items.EMERALD, 9));
        inventory.setItem(40, new ItemStack(ModItems.HO_MET_5MG.get()));

        Item requested = DealerRegistries.product("four_ho_met_5mg").orElseThrow().item().get();
        helper.assertTrue(requested == ModItems.HO_MET_25MG.get(), "A saved legacy customer product did not resolve to its current format");
        helper.assertTrue(count(player, requested) == 6, "The customer must count current and matching legacy bottles across the inventory");
        helper.assertTrue(remove(player, requested, 5) == 5, "The customer failed to remove the requested quantity from mixed legacy/current stock");
        helper.assertTrue(inventory.getItem(0).isEmpty() && inventory.getItem(1).isEmpty(), "The matching first two inventory stacks were not consumed");
        helper.assertTrue(count(player, requested) == 1, "A sale removed more than requested or missed the remaining offhand bottle");
        helper.assertTrue(remove(player, requested, 8) == 1 && count(player, requested) == 0,
                "A partial sale must remove only available bottles and report its actual quantity");
        helper.assertTrue(inventory.getItem(2).is(ModItems.HO_MET_10MG.get()) && inventory.getItem(2).getCount() == 7,
                "A customer sale consumed the wrong legacy bottle tier");
        helper.assertTrue(inventory.getItem(3).is(Items.EMERALD) && inventory.getItem(3).getCount() == 9,
                "A customer sale consumed unrelated inventory");
        helper.assertTrue(count(player, Items.EMERALD) == 9 && remove(player, Items.EMERALD, 4) == 4
                && inventory.getItem(3).getCount() == 5, "Unrelated exact-item inventory matching changed");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacyTradeMatchingKeepsAllFourBottleTiersSeparate(GameTestHelper helper) {
        List<Item> legacy = List.of(ModItems.HO_MET_5MG.get(), ModItems.HO_MET_10MG.get(),
                ModItems.HO_MET_20MG.get(), ModItems.HO_MET_40MG.get());
        List<Item> current = List.of(ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(),
                ModItems.HO_MET_250MG.get(), ModItems.HO_MET_500MG.get());
        for (int tier = 0; tier < legacy.size(); tier++) {
            ItemStack oldBottle = new ItemStack(legacy.get(tier));
            helper.assertTrue(DealerRegistries.matchesProductItem(oldBottle, current.get(tier)), "A corresponding legacy bottle was rejected");
            helper.assertTrue(DealerRegistries.productFor(oldBottle).orElseThrow().item().get() == current.get(tier),
                    "Legacy samples and customer inventory resolved to different products");
            for (int other = 0; other < current.size(); other++) {
                if (other != tier) {
                    helper.assertFalse(DealerRegistries.matchesProductItem(oldBottle, current.get(other)), "A legacy bottle matched another strength tier");
                }
            }
            helper.assertTrue(DealerRegistries.matchesProductItem(new ItemStack(current.get(tier)), current.get(tier)),
                    "Current bottle identity matching changed");
            helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(current.get(tier)), legacy.get(tier)),
                    "Legacy compatibility must only map old inventory to current requested products");
        }
        helper.assertFalse(DealerRegistries.matchesProductItem(ItemStack.EMPTY, Items.AIR), "Empty inventory matched a product");
        helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(ModItems.HO_MET_POWDER.get()), ModItems.HO_MET_25MG.get()),
                "Powder was incorrectly accepted as bottled product");
        helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(ModItems.TWO_CB_25MG.get()), ModItems.HO_MET_25MG.get()),
                "A different substance was accepted as bottled product");
        helper.succeed();
    }

    private static int count(ServerPlayer player, Item item) {
        return invokeInventoryMethod("countItem", new Class<?>[]{ServerPlayer.class, Item.class}, player, item);
    }

    private static int remove(ServerPlayer player, Item item, int amount) {
        return invokeInventoryMethod("removeItem", new Class<?>[]{ServerPlayer.class, Item.class, int.class}, player, item, amount);
    }

    private static int invokeInventoryMethod(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CustomerService.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (int) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not exercise the real customer inventory method " + name, exception);
        }
    }
}
