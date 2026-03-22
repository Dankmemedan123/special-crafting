package com.example.customcrafting;

import com.example.customcrafting.block.CustomCraftingTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CustomCraftingMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<CustomCraftingTableBlockEntity>> CUSTOM_CRAFTING_TABLE =
            BLOCK_ENTITIES.register("custom_crafting_table", () ->
                    BlockEntityType.Builder
                            .of(CustomCraftingTableBlockEntity::new, ModBlocks.CUSTOM_CRAFTING_TABLE.get())
                            .build(null));
}
