package com.example.customcrafting.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/**
 * Persists all custom recipes to: world/data/custom_crafting_recipes.dat
 *
 * Key lesson from handoff (9.4): MUST call setDirty() after any mutation.
 * Without it, Forge won't write data to disk and changes are lost on restart.
 */
public class CustomRecipeData extends SavedData {
    private static final String DATA_NAME = "custom_crafting_recipes";
    private final List<CustomRecipe> recipes = new ArrayList<>();

    public CustomRecipeData() {}

    /**
     * Adds a recipe only if no duplicate exists.
     * @return true if added, false if duplicate was found
     */
    public boolean addRecipe(CustomRecipe recipe) {
        if (isDuplicate(recipe)) return false;
        recipes.add(recipe);
        setDirty(); // Lesson 9.4: MUST call setDirty() or data won't persist
        return true;
    }

    /**
     * Check if an identical recipe already exists.
     * Uses CustomRecipe.isDuplicateOf() which compares all 9 ingredients
     * and the output by item type + NBT tags + output count.
     */
    public boolean isDuplicate(CustomRecipe recipe) {
        for (CustomRecipe existing : recipes) {
            if (existing.isDuplicateOf(recipe)) return true;
        }
        return false;
    }

    /**
     * Remove a recipe by index (for admin commands).
     * @return true if removed, false if index out of range
     */
    public boolean removeRecipe(int index) {
        if (index < 0 || index >= recipes.size()) return false;
        recipes.remove(index);
        setDirty();
        return true;
    }

    /**
     * Remove ALL recipes in one operation. Calls setDirty() exactly once
     * instead of N times (one per recipe).
     * @return the number of recipes that were cleared
     */
    public int clearAll() {
        int count = recipes.size();
        if (count > 0) {
            recipes.clear();
            setDirty();
        }
        return count;
    }

    public List<CustomRecipe> getRecipes() { return Collections.unmodifiableList(recipes); }
    public int getRecipeCount() { return recipes.size(); }

    public static CustomRecipeData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(CustomRecipeData::load, CustomRecipeData::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (CustomRecipe r : recipes) list.add(r.toNbt());
        tag.put("Recipes", list);
        return tag;
    }

    public static CustomRecipeData load(CompoundTag tag) {
        CustomRecipeData data = new CustomRecipeData();
        ListTag list = tag.getList("Recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CustomRecipe r = CustomRecipe.fromNbt(list.getCompound(i));
            if (r != null) data.recipes.add(r);
        }
        return data;
    }
}
