package com.example.customcrafting.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Output slot for the Custom Crafting Table.
 *
 * Design rationale (from Forge forums + vanilla ResultSlot source):
 * - mayPlace returns true so players can still manually place items when defining recipes
 * - onTake() is the hook where we consume ingredients, mirroring vanilla's ResultSlot pattern
 * - The parent menu is responsible for setting the "matched recipe" flag and populating this slot
 *
 * When a matched recipe exists and the player takes the output:
 *   1. Each non-empty ingredient slot (0-8) is shrunk by 1
 *   2. The menu re-checks for recipe matches (via slotsChanged cascade)
 */
public class CustomResultSlot extends Slot {
    private final Container craftingGrid;
    private final CustomCraftingTableMenu parentMenu;

    public CustomResultSlot(Container resultContainer, int slotIndex, int x, int y,
                            Container craftingGrid, CustomCraftingTableMenu parentMenu) {
        super(resultContainer, slotIndex, x, y);
        this.craftingGrid = craftingGrid;
        this.parentMenu = parentMenu;
    }

    // Allow placement so players can still manually define output for saving recipes
    @Override
    public boolean mayPlace(ItemStack stack) { return true; }

    @Override
    public boolean mayPickup(Player player) { return true; }

    @Override
    public void onTake(Player player, ItemStack stack) {
        // Only consume ingredients if the menu flagged an active recipe match
        if (parentMenu.hasActiveRecipeMatch()) {
            for (int i = 0; i < 9; i++) {
                ItemStack ingredient = craftingGrid.getItem(i);
                if (!ingredient.isEmpty()) {
                    ingredient.shrink(1);
                    if (ingredient.isEmpty()) {
                        craftingGrid.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
            // Clear the match flag — slotsChanged() will re-evaluate
            parentMenu.clearActiveRecipeMatch();
        }
        super.onTake(player, stack);
    }
}
