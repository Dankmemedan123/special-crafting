package com.example.customcrafting.block;

import com.example.customcrafting.ModBlockEntities;
import com.example.customcrafting.menu.CustomCraftingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CustomCraftingTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);

    public CustomCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUSTOM_CRAFTING_TABLE.get(), pos, state);
    }

    @Override public int getContainerSize() { return 10; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("container.customcrafting.custom_crafting_table");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CustomCraftingTableMenu(id, inventory, this);
    }
}
