package com.example.customcrafting.command;

import com.example.customcrafting.data.CustomRecipe;
import com.example.customcrafting.data.CustomRecipeData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Adds /customcrafting commands for recipe management and viewing.
 *
 * Per Forge docs (RegisterCommandsEvent):
 *   "Commands are rebuilt whenever ReloadableServerResources is recreated."
 *   Register on the Forge event bus (MinecraftForge.EVENT_BUS), NOT the mod bus.
 *
 * Subcommands:
 *   /customcrafting list         - Show all saved recipes with index numbers
 *   /customcrafting remove <id>  - Remove a recipe by its index (op only)
 *   /customcrafting clear        - Remove ALL recipes (op only)
 *   /customcrafting count        - Show total number of saved recipes
 */
public class CustomCraftingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("customcrafting")
                // /customcrafting list
                .then(Commands.literal("list")
                    .executes(ctx -> listRecipes(ctx.getSource())))
                // /customcrafting count
                .then(Commands.literal("count")
                    .executes(ctx -> countRecipes(ctx.getSource())))
                // /customcrafting remove <index> (requires op level 2)
                .then(Commands.literal("remove")
                    .requires(src -> src.hasPermission(2))
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .executes(ctx -> removeRecipe(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "index")))))
                // /customcrafting clear (requires op level 2)
                .then(Commands.literal("clear")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> clearRecipes(ctx.getSource())))
        );
    }

    private static int listRecipes(CommandSourceStack source) {
        if (source.getServer() == null) {
            source.sendFailure(Component.literal("Command must be run on a server."));
            return 0;
        }

        CustomRecipeData data = CustomRecipeData.getOrCreate(source.getServer());
        List<CustomRecipe> recipes = data.getRecipes();

        if (recipes.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No custom recipes saved yet."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "§6=== Custom Crafting Recipes (" + recipes.size() + " total) ==="), false);

        for (int i = 0; i < recipes.size(); i++) {
            final int index = i;
            CustomRecipe recipe = recipes.get(i);
            MutableComponent line = Component.literal("§e#" + index + " §f");

            // Build ingredient summary: show non-empty slots
            StringBuilder ingredientStr = new StringBuilder();
            int ingredientCount = 0;
            for (int j = 0; j < 9; j++) {
                ItemStack ing = recipe.ingredients[j];
                if (!ing.isEmpty()) {
                    if (ingredientCount > 0) ingredientStr.append(" + ");
                    ingredientStr.append(ing.getHoverName().getString());
                    if (ing.getCount() > 1) ingredientStr.append(" x").append(ing.getCount());
                    ingredientCount++;
                }
            }

            // Output
            String outputStr = recipe.output.getHoverName().getString();
            if (recipe.output.getCount() > 1) outputStr += " x" + recipe.output.getCount();

            line.append(Component.literal(ingredientStr.toString()));
            line.append(Component.literal(" §7→ §a" + outputStr));

            source.sendSuccess(() -> line, false);
        }

        return recipes.size();
    }

    private static int countRecipes(CommandSourceStack source) {
        if (source.getServer() == null) {
            source.sendFailure(Component.literal("Command must be run on a server."));
            return 0;
        }
        CustomRecipeData data = CustomRecipeData.getOrCreate(source.getServer());
        int count = data.getRecipeCount();
        source.sendSuccess(() -> Component.literal(
                "§6" + count + " custom recipe(s) saved."), false);
        return count;
    }

    private static int removeRecipe(CommandSourceStack source, int index) {
        if (source.getServer() == null) {
            source.sendFailure(Component.literal("Command must be run on a server."));
            return 0;
        }

        CustomRecipeData data = CustomRecipeData.getOrCreate(source.getServer());
        if (data.removeRecipe(index)) {
            source.sendSuccess(() -> Component.literal(
                    "§aRemoved recipe #" + index + ". (" + data.getRecipeCount() + " remaining)"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                    "§cInvalid recipe index: " + index + ". Use /customcrafting list to see valid indices."));
            return 0;
        }
    }

    private static int clearRecipes(CommandSourceStack source) {
        if (source.getServer() == null) {
            source.sendFailure(Component.literal("Command must be run on a server."));
            return 0;
        }

        CustomRecipeData data = CustomRecipeData.getOrCreate(source.getServer());
        int count = data.clearAll();

        source.sendSuccess(() -> Component.literal(
                "§aCleared all " + count + " custom recipe(s)."), true);
        return count;
    }
}
