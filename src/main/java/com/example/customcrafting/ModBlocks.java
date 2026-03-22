package com.example.customcrafting;

import com.example.customcrafting.block.CustomCraftingTableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CustomCraftingMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CustomCraftingMod.MOD_ID);

    public static final RegistryObject<Block> CUSTOM_CRAFTING_TABLE =
            BLOCKS.register("custom_crafting_table", CustomCraftingTableBlock::new);
    public static final RegistryObject<Item> CUSTOM_CRAFTING_TABLE_ITEM =
            ITEMS.register("custom_crafting_table",
                    () -> new BlockItem(CUSTOM_CRAFTING_TABLE.get(), new Item.Properties()));
}
