package com.example.customcrafting;

import com.example.customcrafting.command.CustomCraftingCommand;
import com.example.customcrafting.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CustomCraftingMod.MOD_ID)
public class CustomCraftingMod {
    public static final String MOD_ID = "customcrafting";

    public CustomCraftingMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    // RegisterCommandsEvent fires on the FORGE event bus (not mod bus)
    // Per Forge docs: commands are rebuilt whenever ReloadableServerResources is recreated
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CustomCraftingCommand.register(event.getDispatcher());
    }
}
