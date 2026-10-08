package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import com.trippycraft.menu.BrewCauldronMenu;
import com.trippycraft.menu.CannabisPressMenu;
import com.trippycraft.menu.ChemistryTableMenu;
import com.trippycraft.menu.MortarAndPestleMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, TrippyCraft.MOD_ID);

    public static final Supplier<MenuType<MortarAndPestleMenu>> MORTAR_AND_PESTLE =
            MENU_TYPES.register("mortar_and_pestle", () -> IMenuTypeExtension.create(MortarAndPestleMenu::new));

    public static final Supplier<MenuType<CannabisPressMenu>> CANNABIS_PRESS =
            MENU_TYPES.register("cannabis_press", () -> IMenuTypeExtension.create(CannabisPressMenu::new));

    public static final Supplier<MenuType<ChemistryTableMenu>> CHEMISTRY_TABLE =
            MENU_TYPES.register("chemistry_table", () -> IMenuTypeExtension.create(ChemistryTableMenu::new));

    public static final Supplier<MenuType<AdvancedChemistryTableMenu>> ADVANCED_CHEMISTRY_TABLE =
            MENU_TYPES.register("advanced_chemistry_table", () -> IMenuTypeExtension.create(AdvancedChemistryTableMenu::new));

    public static final Supplier<MenuType<BrewCauldronMenu>> BREW_CAULDRON =
            MENU_TYPES.register("brew_cauldron", () -> IMenuTypeExtension.create(BrewCauldronMenu::new));

    private ModMenuTypes() {
    }
}
