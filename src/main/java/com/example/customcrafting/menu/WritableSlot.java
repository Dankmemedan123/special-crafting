package com.example.customcrafting.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WritableSlot extends Slot {
    public WritableSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
    @Override public boolean mayPlace(ItemStack stack) { return true; }
    @Override public boolean mayPickup(Player player) { return true; }
}
