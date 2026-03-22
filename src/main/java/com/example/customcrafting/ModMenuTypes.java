package com.example.customcrafting;

import com.example.customcrafting.menu.CustomCraftingTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CustomCraftingMod.MOD_ID);

    public static final RegistryObject<MenuType<CustomCraftingTableMenu>> CUSTOM_CRAFTING_TABLE =
            MENUS.register("custom_crafting_table",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) -> new CustomCraftingTableMenu(windowId, inv)));
}
