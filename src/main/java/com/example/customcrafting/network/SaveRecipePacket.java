package com.example.customcrafting.network;

import com.example.customcrafting.data.CustomRecipe;
import com.example.customcrafting.data.CustomRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SaveRecipePacket {
    private final ItemStack[] ingredients; // exactly 9
    private final ItemStack output;

    public SaveRecipePacket(ItemStack[] ingredients, ItemStack output) {
        this.ingredients = ingredients;
        this.output = output;
    }

    public static void encode(SaveRecipePacket msg, FriendlyByteBuf buf) {
        for (ItemStack stack : msg.ingredients) buf.writeItem(stack);
        buf.writeItem(msg.output);
    }

    public static SaveRecipePacket decode(FriendlyByteBuf buf) {
        ItemStack[] ingredients = new ItemStack[9];
        for (int i = 0; i < 9; i++) ingredients[i] = buf.readItem();
        return new SaveRecipePacket(ingredients, buf.readItem());
    }

    public static void handle(SaveRecipePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Validate: must have output
            if (msg.output.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cCannot save a recipe with no output item!"));
                return;
            }

            // Validate: must have at least one ingredient
            boolean hasIngredient = false;
            for (ItemStack s : msg.ingredients) if (!s.isEmpty()) { hasIngredient = true; break; }
            if (!hasIngredient) {
                player.sendSystemMessage(Component.literal("§cCannot save a recipe with no ingredients!"));
                return;
            }

            CustomRecipeData data = CustomRecipeData.getOrCreate(player.server);
            CustomRecipe newRecipe = new CustomRecipe(msg.ingredients, msg.output);

            // addRecipe returns false if duplicate exists (dedup check built into CustomRecipeData)
            if (data.addRecipe(newRecipe)) {
                player.sendSystemMessage(Component.literal(
                        "§aRecipe saved! (" + data.getRecipeCount() + " recipes stored)"));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§eThis recipe already exists! Not saved."));
            }
        });
        ctx.setPacketHandled(true);
    }
}
