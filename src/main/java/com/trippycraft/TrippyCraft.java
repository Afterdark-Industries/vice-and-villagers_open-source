package com.trippycraft;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.mojang.logging.LogUtils;
import com.trippycraft.client.ClientBootstrap;
import com.trippycraft.block.OpiumPoppyBlock;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModFeatures;
import com.trippycraft.registry.ModBlockEntities;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.economy.CustomerSampleEvents;
import com.trippycraft.economy.BartenderService;
import com.trippycraft.economy.CustomerVillagerEvents;
import com.trippycraft.economy.DealerVillageEvents;
import com.trippycraft.economy.DealerVillagerEvents;
import com.trippycraft.economy.MarketEvents;
import com.trippycraft.economy.MarketVillagerEvents;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.registry.ModCreativeTabs;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModIngredientSerializers;
import com.trippycraft.registry.ModLootModifiers;
import com.trippycraft.registry.ModMenuTypes;
import com.trippycraft.registry.ModRecipeSerializers;
import com.trippycraft.registry.ModSounds;
import com.trippycraft.registry.ModStructureProcessors;
import com.trippycraft.registry.ModVillagers;
import com.trippycraft.substance.ModCommands;
import com.trippycraft.substance.SubstanceEvents;
import com.trippycraft.substance.mdma.MdmaSocialEffects;
import com.trippycraft.world.AcaciaHarvestEvents;
import com.trippycraft.world.ErgotInfectionEvents;
import com.trippycraft.world.VillageBarEvents;
import com.trippycraft.world.TavernAmbienceService;
import com.trippycraft.world.TavernJukeboxEvents;
import com.trippycraft.world.TavernNightlifeService;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

@net.neoforged.fml.common.Mod(TrippyCraft.MOD_ID)
public final class TrippyCraft {
    public static final String MOD_ID = "trippycraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrippyCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(TrippyConfig::onLoad);
        modEventBus.addListener(TrippyClientConfig::onLoad);
        ModBlocks.BLOCKS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModVillagers.POI_TYPES.register(modEventBus);
        ModVillagers.PROFESSIONS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "koka_powder"),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "cocaine_powder")
        );
        ModItems.ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "mdma_tablet"),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "mdma_100mg")
        );
        ModItems.ITEMS.register(modEventBus);
        ModIngredientSerializers.INGREDIENT_SERIALIZERS.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModLootModifiers.GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        ModStructureProcessors.STRUCTURE_PROCESSORS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModNetwork.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.register(modEventBus);
        }

        IEventBus gameEventBus = NeoForge.EVENT_BUS;
        gameEventBus.addListener(SubstanceEvents::onPlayerTickPost);
        gameEventBus.addListener(SubstanceEvents::onPlayerLoadFromFile);
        gameEventBus.addListener(SubstanceEvents::onPlayerSaveToFile);
        gameEventBus.addListener(SubstanceEvents::onPlayerClone);
        gameEventBus.addListener(SubstanceEvents::onPlayerLoggedIn);
        gameEventBus.addListener(MarketEvents::onPlayerLoadFromFile);
        gameEventBus.addListener(MarketEvents::onPlayerSaveToFile);
        gameEventBus.addListener(MarketEvents::onPlayerClone);
        gameEventBus.addListener(MarketEvents::onPlayerLoggedOut);
        gameEventBus.addListener(SubstanceEvents::onPlayerRespawn);
        gameEventBus.addListener(SubstanceEvents::onPlayerChangedDimension);
        gameEventBus.addListener(SubstanceEvents::onCanPlayerSleep);
        gameEventBus.addListener(SubstanceEvents::onPlayerWakeUp);
        gameEventBus.addListener(SubstanceEvents::onLivingEntityUseItemFinish);
        gameEventBus.addListener(SubstanceEvents::onServerStopped);
        gameEventBus.addListener(MarketEvents::onServerStopped);
        gameEventBus.addListener(DealerVillagerEvents::onEntityInteract);
        gameEventBus.addListener(MdmaSocialEffects::onEntityInteract);
        gameEventBus.addListener(CustomerVillagerEvents::onEntityInteract);
        gameEventBus.addListener(MarketVillagerEvents::onEntityInteract);
        gameEventBus.addListener(ModCommands::register);
        gameEventBus.addListener(ErgotInfectionEvents::onLevelTickPost);
        gameEventBus.addListener(CustomerSampleEvents::onLevelTickPost);
        gameEventBus.addListener(DealerVillageEvents::onLevelTickPost);
        gameEventBus.addListener(VillageBarEvents::onLevelTickPost);
        gameEventBus.addListener(TavernNightlifeService::onLevelTickPost);
        gameEventBus.addListener(TavernAmbienceService::onLevelTickPost);
        gameEventBus.addListener(TavernAmbienceService::onPlayerLoggedOut);
        gameEventBus.addListener(TavernAmbienceService::onServerStopped);
        gameEventBus.addListener(TavernJukeboxEvents::onRightClickBlock);
        gameEventBus.addListener(OpiumPoppyBlock::onRightClickBlock);
        gameEventBus.addListener(BartenderService::onEntityTickPost);
        gameEventBus.addListener(ErgotInfectionEvents::onCropGrowPost);
        gameEventBus.addListener(ErgotInfectionEvents::onBlockPlaced);
        gameEventBus.addListener(AcaciaHarvestEvents::onBlockToolModification);
        gameEventBus.addListener(AcaciaHarvestEvents::onBlockBreak);
        gameEventBus.addListener(ErgotInfectionEvents::onBlockBreak);

        modContainer.registerConfig(ModConfig.Type.COMMON, TrippyConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TrippyClientConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Vice and Villagers experience engine booted for abstract, fictionalized gameplay effects.");
    }
}
