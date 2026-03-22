package com.example.customcrafting.menu;

import com.example.customcrafting.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/**
 * Menu for the Custom Crafting Table.
 *
 * Slot layout (from handoff Section 11):
 *   Slots 0-8:   3x3 crafting grid (WritableSlot)
 *   Slot  9:     output (CustomResultSlot)
 *   Slots 10-36: player main inventory
 *   Slots 37-45: hotbar
 *   Total: 46 slots
 *
 * Recipe execution architecture:
 *   - Client: containerTick() detects grid changes → sends CheckRecipePacket (zero-payload signal)
 *   - Server: CheckRecipePacket.handle() reads server container → matches against saved recipes
 *   - Server: if match found → sets slot 9, sets activeRecipeMatch flag, syncs via ClientboundContainerSetSlotPacket
 *   - Server: CustomResultSlot.onTake() checks flag → shrinks grid ingredients by 1 each
 *
 * IMPORTANT: activeRecipeMatch is ONLY managed by CheckRecipePacket.handle() and
 * CustomResultSlot.onTake(). It must NEVER be reset in slotsChanged() — doing so
 * creates a race condition that prevents ingredient consumption (see ERROR-AUDIT BUG-2).
 *
 * Key lesson from handoff (9.9): Two-constructor pattern is mandatory.
 */
public class CustomCraftingTableMenu extends AbstractContainerMenu {
    private final Container craftingContainer;

    /** True when the output slot was auto-filled by a matching recipe. */
    private boolean activeRecipeMatch = false;

    /**
     * Tracks what item was auto-set in the output slot by recipe matching.
     * Used to distinguish auto-set output from manually-placed items,
     * so we don't accidentally destroy player-placed items when clearing.
     * Null when no auto-set output exists.
     */
    @Nullable
    private ItemStack autoSetOutput = null;

    // Client-side constructor — called by MenuType factory (IForgeMenuType)
    public CustomCraftingTableMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(10));
    }

    // Server-side constructor — called by BlockEntity.createMenu
    public CustomCraftingTableMenu(int id, Inventory playerInv, Container container) {
        super(ModMenuTypes.CUSTOM_CRAFTING_TABLE.get(), id);
        this.craftingContainer = container;
        checkContainerSize(container, 10);
        container.startOpen(playerInv.player);

        // Slots 0-8: 3x3 crafting grid
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                this.addSlot(new WritableSlot(container, col + row * 3, 30 + col * 18, 17 + row * 18));

        // Slot 9: output slot (CustomResultSlot — handles ingredient consumption on take)
        this.addSlot(new CustomResultSlot(container, 9, 124, 35, container, this));

        // Slots 10-36: player main inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));

        // Slots 37-45: hotbar
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 162));
    }

    // ──── Recipe match state management ────
    // These are ONLY called by CheckRecipePacket.handle() and CustomResultSlot.onTake().
    // NEVER reset activeRecipeMatch in slotsChanged() — see ERROR-AUDIT BUG-2.

    public boolean hasActiveRecipeMatch() { return activeRecipeMatch; }
    public void setActiveRecipeMatch(boolean value) { activeRecipeMatch = value; }
    public void clearActiveRecipeMatch() { activeRecipeMatch = false; }
    public Container getCraftingContainer() { return craftingContainer; }

    @Nullable
    public ItemStack getAutoSetOutput() { return autoSetOutput; }
    public void setAutoSetOutput(@Nullable ItemStack stack) {
        this.autoSetOutput = (stack != null) ? stack.copy() : null;
    }

    @Override
    public boolean stillValid(Player player) { return craftingContainer.stillValid(player); }

    /**
     * Shift-click behavior — follows vanilla CraftingMenu pattern exactly.
     *
     * Slot indices (from handoff Section 11):
     *   0-9:   container (grid + output)
     *   10-45: player inventory (36 slots)
     *   Total: 46
     *
     * FIX for BUG-3: onTake() is called at the END for all slots,
     * matching vanilla's CraftingMenu.quickMoveStack pattern. The slot.onTake
     * call receives the remaining stack (post-move), which is correct —
     * CustomResultSlot.onTake doesn't use the stack parameter for logic,
     * only the activeRecipeMatch flag.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalCopy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalCopy = slotStack.copy();

            if (index < 10) {
                // Container → player inventory
                if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory → container (grid + output, slots 0-9)
                if (!this.moveItemStackTo(slotStack, 0, 10, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            // Nothing was actually moved
            if (slotStack.getCount() == originalCopy.getCount()) {
                return ItemStack.EMPTY;
            }

            // Notify the slot that items were taken — this triggers
            // CustomResultSlot.onTake() for slot 9, which consumes ingredients
            slot.onTake(player, slotStack);
        }

        return originalCopy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        craftingContainer.stopOpen(player);
    }
}
