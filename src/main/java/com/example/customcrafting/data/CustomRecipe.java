package com.example.customcrafting.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/**
 * POJO representing a single saved custom recipe.
 *
 * A recipe is position-sensitive (shaped): ingredient[0] = top-left, ingredient[8] = bottom-right.
 * This mirrors vanilla's shaped crafting where the pattern position matters.
 *
 * Item comparison uses ItemStack.isSameItemSameTags() which checks:
 *   - Same item type (registry ID)
 *   - Same NBT tags (enchantments, custom data, etc.)
 * This is the recommended approach per Forge docs on StrictNBTIngredient matching.
 *
 * Count is NOT checked for matching (1 diamond matches whether slot has 1 or 64).
 * This follows vanilla crafting behavior where you need at least 1 of each ingredient.
 */
public class CustomRecipe {
    public final ItemStack[] ingredients; // size 9
    public final ItemStack output;

    public CustomRecipe(ItemStack[] ingredients, ItemStack output) {
        this.ingredients = new ItemStack[9];
        for (int i = 0; i < 9; i++)
            this.ingredients[i] = (ingredients[i] != null) ? ingredients[i].copy() : ItemStack.EMPTY;
        this.output = output.copy();
    }

    /**
     * Check if a grid of 9 ItemStacks matches this recipe's ingredients.
     * Position-sensitive: slot i in the grid must match ingredient i in the recipe.
     *
     * Matching rules:
     *   - Empty recipe slot matches empty grid slot
     *   - Non-empty recipe slot matches if same item + same tags (ignoring count)
     *   - Grid slot must have count >= 1 (which is always true if non-empty)
     */
    public boolean matches(ItemStack[] grid) {
        if (grid == null || grid.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack required = this.ingredients[i];
            ItemStack actual = grid[i];
            if (actual == null) actual = ItemStack.EMPTY; // null guard
            if (required.isEmpty() && actual.isEmpty()) continue;
            if (required.isEmpty() != actual.isEmpty()) return false;
            // Both non-empty: check item type + NBT tags (not count)
            if (!ItemStack.isSameItemSameTags(required, actual)) return false;
        }
        return true;
    }

    /**
     * Check if this recipe is identical to another (for duplicate prevention).
     * Two recipes are duplicates if all 9 ingredients AND the output match exactly.
     */
    public boolean isDuplicateOf(CustomRecipe other) {
        if (other == null) return false;
        // Check output
        if (!ItemStack.isSameItemSameTags(this.output, other.output)) return false;
        if (this.output.getCount() != other.output.getCount()) return false;
        // Check all ingredients
        for (int i = 0; i < 9; i++) {
            ItemStack a = this.ingredients[i];
            ItemStack b = other.ingredients[i];
            if (a.isEmpty() && b.isEmpty()) continue;
            if (a.isEmpty() != b.isEmpty()) return false;
            if (!ItemStack.isSameItemSameTags(a, b)) return false;
        }
        return true;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : ingredients) list.add(stack.save(new CompoundTag()));
        tag.put("Ingredients", list);
        tag.put("Output", output.save(new CompoundTag()));
        return tag;
    }

    @Nullable
    public static CustomRecipe fromNbt(CompoundTag tag) {
        ListTag list = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        if (list.size() != 9) return null;
        ItemStack[] ingredients = new ItemStack[9];
        for (int i = 0; i < 9; i++) ingredients[i] = ItemStack.of(list.getCompound(i));
        ItemStack output = ItemStack.of(tag.getCompound("Output"));
        if (output.isEmpty()) return null;
        return new CustomRecipe(ingredients, output);
    }
}
