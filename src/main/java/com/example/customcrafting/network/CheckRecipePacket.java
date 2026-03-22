package com.example.customcrafting.network;

import com.example.customcrafting.data.CustomRecipe;
import com.example.customcrafting.data.CustomRecipeData;
import com.example.customcrafting.menu.CustomCraftingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client → Server signal: "my grid changed, please re-check recipes."
 *
 * SECURITY: This is a zero-payload signal. The server reads the grid
 * from its own authoritative container, NEVER from client-sent data.
 * This prevents item duplication exploits from fabricated packets.
 *
 * The server checks against saved recipes and syncs the output slot back
 * via ClientboundContainerSetSlotPacket (same pattern as vanilla CraftingMenu).
 */
public class CheckRecipePacket {

    public CheckRecipePacket() {}

    public static void encode(CheckRecipePacket msg, FriendlyByteBuf buf) {
        // Zero-payload signal — write a dummy byte so the codec has something to work with
        buf.writeByte(0);
    }

    public static CheckRecipePacket decode(FriendlyByteBuf buf) {
        buf.readByte(); // consume dummy byte
        return new CheckRecipePacket();
    }

    public static void handle(CheckRecipePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Verify player has our menu open
            if (!(player.containerMenu instanceof CustomCraftingTableMenu menu)) return;

            Container container = menu.getCraftingContainer();
            CustomRecipeData data = CustomRecipeData.getOrCreate(player.server);

            // Read the grid from the SERVER's container — never trust client data
            ItemStack[] serverGrid = new ItemStack[9];
            boolean hasAny = false;
            for (int i = 0; i < 9; i++) {
                serverGrid[i] = container.getItem(i);
                if (!serverGrid[i].isEmpty()) hasAny = true;
            }

            if (hasAny) {
                for (CustomRecipe recipe : data.getRecipes()) {
                    if (recipe.matches(serverGrid)) {
                        // Match found! Set output and flag the menu
                        ItemStack outputCopy = recipe.output.copy();
                        container.setItem(9, outputCopy);
                        menu.setActiveRecipeMatch(true);
                        // Store what we auto-set so we can distinguish from manual placement
                        menu.setAutoSetOutput(outputCopy.copy());

                        // Sync output slot (index 9) to the client
                        player.connection.send(new ClientboundContainerSetSlotPacket(
                                menu.containerId,
                                menu.incrementStateId(),
                                9,
                                outputCopy));
                        return;
                    }
                }
            }

            // No match found.
            // Only clear the output if it was auto-set by US (not manually placed by player).
            if (menu.hasActiveRecipeMatch()) {
                ItemStack currentOutput = container.getItem(9);
                ItemStack autoSet = menu.getAutoSetOutput();

                // Only clear if the output still matches what we auto-set
                // If player swapped it for something else, leave their item alone
                if (autoSet != null && ItemStack.isSameItemSameTags(currentOutput, autoSet)) {
                    container.setItem(9, ItemStack.EMPTY);
                    player.connection.send(new ClientboundContainerSetSlotPacket(
                            menu.containerId,
                            menu.incrementStateId(),
                            9,
                            ItemStack.EMPTY));
                }
                menu.clearActiveRecipeMatch();
                menu.setAutoSetOutput(null);
            }
        });
        ctx.setPacketHandled(true);
    }
}
