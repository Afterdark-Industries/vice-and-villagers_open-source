package com.trippycraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.trippycraft.registry.ModLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public final class AddSubstanceLootTableModifier extends LootModifier {
    public static final MapCodec<AddSubstanceLootTableModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance)
                    .and(ResourceLocation.CODEC.fieldOf("loot_table").forGetter(modifier -> modifier.lootTableId))
                    .apply(instance, AddSubstanceLootTableModifier::new));

    private final ResourceLocation lootTableId;

    public AddSubstanceLootTableModifier(LootItemCondition[] conditions, ResourceLocation lootTableId) {
        super(conditions);
        this.lootTableId = lootTableId;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (this.lootTableId.equals(context.getQueriedLootTableId())) {
            return generatedLoot;
        }

        LootTable injectedTable = context.getResolver()
                .lookupOrThrow(Registries.LOOT_TABLE)
                .getOrThrow(ResourceKey.create(Registries.LOOT_TABLE, this.lootTableId))
                .value();

        int initialSize = generatedLoot.size();
        injectedTable.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));

        for (int index = initialSize; index < generatedLoot.size(); index++) {
            AmbientLootItemState.apply(generatedLoot.get(index), context.getRandom());
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.ADD_SUBSTANCE_LOOT_TABLE.get();
    }
}
