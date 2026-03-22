# Custom Crafting Table Mod — Update Handoff

**Date:** March 22, 2026  
**Status:** All 4 open items from predecessor resolved. 16 Java source files (was 14). Ready for in-game testing.

---

## What Was Done

### 1. Recipe Execution (the big one) ✅

The table now actually crafts using saved recipes. Here's how it works:

**New files:**
- `menu/CustomResultSlot.java` — Custom output slot that consumes ingredients on `onTake()`
- `network/CheckRecipePacket.java` — Client→Server packet for real-time recipe matching

**Modified files:**
- `menu/CustomCraftingTableMenu.java` — Slot 9 now uses `CustomResultSlot`; added `activeRecipeMatch` flag, `checkRecipeMatch()`, `setActiveRecipeMatch()`, `getCraftingContainer()`
- `screen/CustomCraftingTableScreen.java` — Added `containerTick()` that detects grid changes and sends `CheckRecipePacket` to server
- `network/NetworkHandler.java` — Registers `CheckRecipePacket` as message ID 1

**Data flow:**
```
Player changes grid slot
        │
        ▼
CustomCraftingTableScreen.containerTick()   [CLIENT]
        │  Detects change vs previousGrid snapshot
        │  Sends CheckRecipePacket with current 9-slot grid
        ▼
CheckRecipePacket.handle()                  [SERVER]
        │  Loops CustomRecipeData.getRecipes()
        │  Calls recipe.matches(grid) — position-sensitive
        │  If match: sets output slot, flags activeRecipeMatch
        │  Syncs via ClientboundContainerSetSlotPacket (vanilla pattern)
        ▼
Player takes output item
        │
        ▼
CustomResultSlot.onTake()                   [SERVER]
        │  Checks activeRecipeMatch flag
        │  Shrinks each non-empty grid slot by 1
        │  Clears flag → slotsChanged cascades → re-checks
```

**Key design decisions (informed by Forge forum research):**
- Recipe matching is 100% server-authoritative. Client never sees saved recipes.
- `containerTick()` uses a snapshot diff to avoid packet spam (only sends when grid actually changes).
- `onTake()` for ingredient consumption mirrors vanilla `ResultSlot` behavior.
- `ItemStack.isSameItemSameTags()` for matching (checks item type + NBT, ignores count) — per Forge docs on `StrictNBTIngredient`.

---

### 2. Duplicate Recipe Prevention ✅

**Modified files:**
- `data/CustomRecipe.java` — Added `matches()` and `isDuplicateOf()` methods
- `data/CustomRecipeData.java` — `addRecipe()` now returns `boolean`; added `isDuplicate()` check
- `network/SaveRecipePacket.java` — Uses return value to give player yellow "already exists" feedback

**How it works:**  
`isDuplicateOf()` compares all 9 ingredient slots AND the output using `ItemStack.isSameItemSameTags()` plus output count. Two recipes are duplicates only if every position matches exactly.

---

### 3. Recipe Viewer / Command System ✅

**New files:**
- `command/CustomCraftingCommand.java` — Full Brigadier command tree

**Modified files:**
- `CustomCraftingMod.java` — Added `@SubscribeEvent` for `RegisterCommandsEvent` on the Forge event bus

**Commands:**
| Command | Permission | What it does |
|---------|-----------|--------------|
| `/customcrafting list` | Anyone | Shows all recipes with index numbers, ingredient names, and output |
| `/customcrafting count` | Anyone | Shows total recipe count |
| `/customcrafting remove <index>` | Op (level 2) | Removes a recipe by index |
| `/customcrafting clear` | Op (level 2) | Removes ALL recipes |

**Example output of `/customcrafting list`:**
```
=== Custom Crafting Recipes (3 total) ===
#0 Diamond + Diamond + Diamond → Diamond Sword x1
#1 Iron Ingot + Stick → Iron Pickaxe x1
#2 Gold Ingot → Gold Block x1
```

---

### 4. Texture Generation ✅

All 4 PNG textures regenerated from the handoff's Python color palette:
- `custom_crafting_table_top.png` — 16×16 purple grid
- `custom_crafting_table_side.png` — 16×16 wood with purple stripes
- `custom_crafting_table_front.png` — 16×16 wood with glowing rune circle
- `custom_crafting_table.png` — 256×256 GUI background with proper slot layout

---

## Complete File Inventory (16 Java + 12 resource + 5 build = 33 files)

### Java source files:
| # | File | Status |
|---|------|--------|
| 1 | `CustomCraftingMod.java` | **Modified** — added RegisterCommandsEvent handler |
| 2 | `ModBlocks.java` | Unchanged |
| 3 | `ModBlockEntities.java` | Unchanged |
| 4 | `ModMenuTypes.java` | Unchanged |
| 5 | `ClientSetup.java` | Unchanged |
| 6 | `block/CustomCraftingTableBlock.java` | Unchanged |
| 7 | `block/CustomCraftingTableBlockEntity.java` | Unchanged |
| 8 | `menu/WritableSlot.java` | Unchanged |
| 9 | `menu/CustomResultSlot.java` | **NEW** — output slot with ingredient consumption |
| 10 | `menu/CustomCraftingTableMenu.java` | **Modified** — recipe match tracking, CustomResultSlot |
| 11 | `screen/CustomCraftingTableScreen.java` | **Modified** — containerTick() for recipe checking |
| 12 | `network/NetworkHandler.java` | **Modified** — registers CheckRecipePacket |
| 13 | `network/SaveRecipePacket.java` | **Modified** — duplicate feedback |
| 14 | `network/CheckRecipePacket.java` | **NEW** — client→server recipe matching |
| 15 | `data/CustomRecipe.java` | **Modified** — matches() + isDuplicateOf() |
| 16 | `data/CustomRecipeData.java` | **Modified** — isDuplicate(), removeRecipe() |
| 17 | `command/CustomCraftingCommand.java` | **NEW** — /customcrafting commands |

---

## What Still Needs Testing

1. **GUI slot alignment** — The Save button at `leftPos+89, topPos+60` may overlap the arrow graphic. If it does, move it to `topPos+80`.
2. **Shift-click with active recipe** — `quickMoveStack` calls `slot.onTake()` when moving from the output slot with an active match. Verify ingredients shrink correctly.
3. **Multi-craft** — Rapidly clicking output with a matched recipe should correctly consume 1 set of ingredients per click.
4. **Packet ordering** — `CheckRecipePacket` fires from `containerTick()`. If there's latency, the output slot may flicker. Consider adding a small debounce (2-3 tick cooldown).
5. **Edge case: manual output placement** — If a player manually places an item in slot 9 while no recipe matches, the item should stay. Verify `CheckRecipePacket` doesn't clear manually-placed items (it only clears when `hasActiveRecipeMatch()` was previously true).

---

## Lessons Carried Forward (all 9 original + 3 new)

**Original 9 from predecessor:** All still apply. See original handoff Sections 9.1–9.9.

**New lessons from this session:**

### 10. Recipe matching must be server-authoritative
Never send the saved recipe list to the client. Instead, send the grid state C→S and let the server respond with the output. This prevents recipe duplication exploits.

### 11. Use containerTick() + snapshot diff for change detection
Don't send a packet every tick. Compare current grid against a `previousGrid[]` snapshot and only send when something actually changed. This avoids unnecessary network traffic.

### 12. RegisterCommandsEvent fires on FORGE bus, not mod bus
Per Forge docs: `RegisterCommandsEvent` is fired on `MinecraftForge.EVENT_BUS`. Using `@SubscribeEvent` on a class registered to the mod bus will silently fail. The mod entry point class is registered to the Forge bus via `MinecraftForge.EVENT_BUS.register(this)` in the constructor.
