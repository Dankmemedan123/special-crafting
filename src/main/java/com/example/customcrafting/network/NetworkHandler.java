package com.example.customcrafting.network;

import com.example.customcrafting.CustomCraftingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel INSTANCE;

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(CustomCraftingMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

        int id = 0;

        INSTANCE.registerMessage(id++, SaveRecipePacket.class,
                SaveRecipePacket::encode, SaveRecipePacket::decode, SaveRecipePacket::handle);

        INSTANCE.registerMessage(id++, CheckRecipePacket.class,
                CheckRecipePacket::encode, CheckRecipePacket::decode, CheckRecipePacket::handle);
    }
}
