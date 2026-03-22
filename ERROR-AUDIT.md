# Custom Crafting Table — Error Audit Report (UPDATED)

**Auditor:** Claude (session 2)  
**Date:** March 22, 2026  
**Files reviewed:** All 17 Java source files, 4 textures, all resource/build files  
**Status:** All critical and moderate issues FIXED. Minor concerns documented for testing.

---

## CRITICAL BUGS — ALL FIXED

### BUG-1: CheckRecipePacket trusted client-sent grid data (SECURITY EXPLOIT) ✅ FIXED
**File:** `network/CheckRecipePacket.java`  
**Was:** Server matched recipes against client-sent ItemStack[] grid data → item duplication exploit.  
**Fix:** Rewrote as zero-payload signal. Server reads its own `menu.getCraftingContainer()` for grid state. Client can no longer fabricate fake grids.

### BUG-2: slotsChanged() unconditionally reset activeRecipeMatch flag ✅ FIXED
**File:** `menu/CustomCraftingTableMenu.java`  
**Was:** `slotsChanged()` set `activeRecipeMatch = false` on every call → race condition could let player take output without consuming ingredients.  
**Fix:** Removed the entire `slotsChanged()` override. Flag is now ONLY managed by `CheckRecipePacket.handle()` (sets true) and `CustomResultSlot.onTake()` (clears).

### BUG-3: quickMoveStack could double-consume or miss-consume ingredients ✅ FIXED
**File:** `menu/CustomCraftingTableMenu.java`  
**Was:** `onTake()` was called after `moveItemStackTo()` had mutated the stack, and had special-case logic for slot 9 that was error-prone.  
**Fix:** Rewrote `quickMoveStack()` to follow vanilla's exact pattern: move items first, then call `slot.onTake(player, slotStack)` at the end for ALL slots. `CustomResultSlot.onTake()` doesn't use the stack parameter — it uses the `activeRecipeMatch` flag — so ordering is now safe.

---

## MODERATE ISSUES — ALL FIXED

### ISSUE-4: checkRecipeMatch() was dead code ✅ FIXED
**Was:** Never-called method in the menu class.  
**Fix:** Removed entirely in the menu rewrite.

### ISSUE-5: slotsChanged() body was dead code ✅ FIXED
**Was:** Built grid arrays but never did anything with them.  
**Fix:** Removed the entire `slotsChanged()` override.

### ISSUE-6: Output clearing could destroy manually-placed items ✅ FIXED
**File:** `network/CheckRecipePacket.java` + `menu/CustomCraftingTableMenu.java`  
**Was:** When recipe match ended, blindly cleared slot 9 even if player had swapped in their own item.  
**Fix:** Added `autoSetOutput` tracking field to the menu. `CheckRecipePacket.handle()` stores what it auto-set. On clear, it only removes the output if it still matches the auto-set value. Player-placed items are preserved.

### ISSUE-7: clearRecipes() called setDirty() N times ✅ FIXED
**File:** `data/CustomRecipeData.java` + `command/CustomCraftingCommand.java`  
**Was:** Loop of `removeRecipe(i)` each calling `setDirty()`.  
**Fix:** Added `clearAll()` method that calls `recipes.clear()` + one `setDirty()`. Command now uses it.

---

## MINOR CONCERNS — DOCUMENTED FOR TESTING

### CONCERN-8: Null guard on grid entries in matches() ✅ FIXED
**File:** `data/CustomRecipe.java`  
**Fix:** Added `if (actual == null) actual = ItemStack.EMPTY;` guard.

### CONCERN-9: No rate limiting on CheckRecipePacket — DEFERRED
A malicious client could spam the signal. Each one causes a loop through all saved recipes. Low-priority for now since the packet is zero-payload and the matching loop is lightweight for typical recipe counts (<100).  
**Future fix:** Add a per-player cooldown (max 1 check per 3 ticks).

### CONCERN-10: containerTick() initial sync timing ✅ FIXED
**File:** `screen/CustomCraftingTableScreen.java`  
**Was:** Single `firstTick` boolean meant the first recipe check could run before server→client slot sync arrived.  
**Fix:** Replaced with `initialSyncTicksRemaining = 5` counter. Sends check packets for the first 5 ticks, giving slot sync time to arrive.

### CONCERN-11: Same container passed as both result container and grid — DOCUMENTED
Not a bug, just an architectural note. Both `CustomResultSlot`'s parent container and its `craftingGrid` reference point to the same 10-slot `Container`. This works because slots 0-8 are grid and slot 9 is output, all in one container. Would break if architecture splits to separate containers — but no plans for that.

### CONCERN-12: GUI button has no visual backing in texture — COSMETIC
Standard MC `Button` widget overlaid on the texture. Functional but not pretty. Low priority cosmetic.

---

## COMPILATION VERIFICATION

All imports verified against Forge 1.20.1 APIs:
- `incrementStateId()` — confirmed `public` on `AbstractContainerMenu` ✓
- `containerId` — confirmed `public final int` on `AbstractContainerMenu` ✓
- `ItemStack.isSameItemSameTags()` — static, exists in 1.20.1 ✓
- `ItemStack.matches()` — static, exists in 1.20.1 ✓
- `ClientboundContainerSetSlotPacket` — exists in `net.minecraft.network.protocol.game` ✓
- `RegisterCommandsEvent` — exists in `net.minecraftforge.event` ✓
- `AbstractContainerScreen.containerTick()` — exists in 1.20.1 ✓
- Zero unused imports across all 17 files ✓
- All cross-file method references verified ✓

---

## REMAINING ITEMS FOR IN-GAME TESTING

1. **Save button overlap** — Button at `leftPos+89, topPos+60` is 80x20px. The GUI texture has a purple accent line at y=75. Verify these don't overlap visually.
2. **Multi-craft rapid clicking** — Click output slot rapidly with a matched recipe. Each click should consume exactly 1 set of ingredients.
3. **Shift-click output** — Shift-click slot 9 with active recipe. Ingredients should shrink by 1 and output should move to player inventory.
4. **Pre-placed items on reopen** — Place items in grid → close GUI → reopen. The 5-tick initial sync window should detect the pre-placed items and auto-match.
5. **Manual output placement while recipe matches** — With a recipe matched (output auto-filled), manually swap the output with a different item. Then change the grid so no recipe matches. The manually-placed item should NOT be cleared.
6. **Commands** — Test `/customcrafting list`, `count`, `remove 0`, `clear` with 0, 1, and many recipes.
7. **Duplicate save** — Save a recipe, then try to save the exact same recipe. Should get yellow "already exists" message.
8. **Server restart persistence** — Save recipes → restart server → recipes should still be there.
