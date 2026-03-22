package com.example.customcrafting.screen;

import com.example.customcrafting.CustomCraftingMod;
import com.example.customcrafting.menu.CustomCraftingTableMenu;
import com.example.customcrafting.network.CheckRecipePacket;
import com.example.customcrafting.network.NetworkHandler;
import com.example.customcrafting.network.SaveRecipePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * GUI screen for the Custom Crafting Table.
 *
 * Key lesson from handoff (9.8): Button positioning MUST happen in init(),
 * not the constructor. leftPos and topPos are only valid after init() runs.
 *
 * GUI coordinates from handoff Section 12:
 *   Grid top-left: (30, 17), each cell +18px
 *   Output slot: (124, 35)
 *   Save button: (89, 60)
 *   Player inv: (8, 104)
 *   Hotbar: (8, 162)
 */
public class CustomCraftingTableScreen extends AbstractContainerScreen<CustomCraftingTableMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CustomCraftingMod.MOD_ID, "textures/gui/custom_crafting_table.png");

    // Track previous grid state to avoid spamming packets every tick
    private final ItemStack[] previousGrid = new ItemStack[9];
    // Counter for initial sync window: check for the first 5 ticks so that
    // server→client slot sync has time to arrive before we do the first recipe check.
    // After the window closes, we only send packets on actual grid changes.
    private int initialSyncTicksRemaining = 5;

    public CustomCraftingTableScreen(CustomCraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 186;              // 20px taller than vanilla for Save button
        this.inventoryLabelY = this.imageHeight - 94;

        // Initialize previous grid tracking
        for (int i = 0; i < 9; i++) previousGrid[i] = ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        // IMPORTANT: leftPos and topPos are guaranteed set before init() runs (lesson 9.8)
        this.addRenderableWidget(
                Button.builder(Component.literal("Save Recipe"), btn -> onSaveClicked())
                        .bounds(this.leftPos + 89, this.topPos + 60, 80, 20)
                        .build());
    }

    private void onSaveClicked() {
        ItemStack[] ingredients = new ItemStack[9];
        for (int i = 0; i < 9; i++)
            ingredients[i] = this.menu.getSlot(i).getItem().copy();
        ItemStack output = this.menu.getSlot(9).getItem().copy();

        if (output.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null)
                mc.player.sendSystemMessage(Component.literal("§cPut an item in the output slot before saving!"));
            return;
        }
        NetworkHandler.INSTANCE.sendToServer(new SaveRecipePacket(ingredients, output));
    }

    /**
     * Called every tick while the screen is open.
     * Detects grid changes and sends a CheckRecipePacket to the server
     * so it can match against saved recipes and auto-fill the output.
     *
     * We compare current grid to the previous snapshot to avoid sending
     * redundant packets every single tick.
     */
    @Override
    protected void containerTick() {
        super.containerTick();

        boolean changed = false;

        // During initial sync window, force a check each tick
        if (initialSyncTicksRemaining > 0) {
            initialSyncTicksRemaining--;
            changed = true;
        }

        // After initial window, only send packets when grid actually changes
        for (int i = 0; i < 9; i++) {
            ItemStack current = this.menu.getSlot(i).getItem();
            if (!ItemStack.matches(current, previousGrid[i])) {
                changed = true;
                previousGrid[i] = current.copy();
            }
        }

        if (changed) {
            // Send zero-payload signal — server reads its own container (security: BUG-1 fix)
            NetworkHandler.INSTANCE.sendToServer(new CheckRecipePacket());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
