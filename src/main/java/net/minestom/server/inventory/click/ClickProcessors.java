package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.Pair;
import net.minestom.server.inventory.TransactionOperator;
import net.minestom.server.inventory.TransactionType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.StackingRule;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Provides standard implementations of most click functions.
 */
public class ClickProcessors {

    private static final @NotNull StackingRule RULE = StackingRule.get();

    public static @NotNull Click.Result leftClick(int slot, @NotNull Click.Result.Builder builder) {
        ItemStack cursor = builder.getCursorItem();
        ItemStack clickedItem = builder.get(slot);

        Pair<ItemStack, ItemStack> pair = TransactionOperator.STACK_LEFT.apply(clickedItem, cursor);
        if (pair != null) { // Stackable items, combine their counts
            return builder.set(slot, pair.left()).cursor(pair.right()).build();
        } else if (!RULE.canBeStacked(cursor, clickedItem)) { // If they're unstackable, switch them
            return builder.set(slot, cursor).cursor(clickedItem).build();
        } else {
            return builder.build();
        }
    }

    public static @NotNull Click.Result rightClick(int slot, @NotNull Click.Result.Builder builder) {
        ItemStack cursor = builder.getCursorItem();
        ItemStack clickedItem = builder.get(slot);

        if (cursor.isAir() && clickedItem.isAir()) return builder.build(); // Both are air, no changes

        if (cursor.isAir()) { // Take half (rounded up) of the clicked item
            int newAmount = (int) Math.ceil(RULE.getAmount(clickedItem) / 2d);
            Pair<ItemStack, ItemStack> cursorSlot = TransactionOperator.stackLeftN(newAmount).apply(cursor, clickedItem);
            return cursorSlot == null ? builder.build() :
                    builder.cursor(cursorSlot.left()).set(slot, cursorSlot.right()).build();
        } else if (clickedItem.isAir() || RULE.canBeStacked(clickedItem, cursor)) { // Can add, transfer one over
            Pair<ItemStack, ItemStack> slotCursor = TransactionOperator.stackLeftN(1).apply(clickedItem, cursor);
            return slotCursor == null ? builder.build() :
                    builder.set(slot, slotCursor.left()).cursor(slotCursor.right()).build();
        } else { // Two existing of items of different types, so switch
            return builder.cursor(clickedItem).set(slot, cursor).build();
        }
    }

    public static @NotNull Click.Result middleClick(int slot, @NotNull Click.Result.Builder builder) {
        var item = builder.get(slot);
        if (builder.getCursorItem().isAir() && !item.isAir()) {
            return builder.cursor(RULE.apply(item, RULE.getMaxSize(item))).build();
        } else {
            return builder.build();
        }
    }

    public static @NotNull Click.Result shiftClick(int slot, @NotNull List<Integer> slots, @NotNull Click.Result.Builder builder) {
        ItemStack clicked = builder.get(slot);

        slots = new ArrayList<>(slots);
        slots.removeIf(i -> i == slot);

        ItemStack result = TransactionType.add(slots, slots).process(clicked, builder);

        if (!result.equals(clicked)) {
            builder.set(slot, result);
        }

        return builder.build();
    }

    public static @NotNull Click.Result doubleClick(@NotNull List<Integer> slots, @NotNull Click.Result.Builder builder) {
        var cursor = builder.getCursorItem();
        if (cursor.isAir()) return builder.build();

        slots = new ArrayList<>(slots);

        var unstacked = TransactionType.general(TransactionOperator.filter(TransactionOperator.STACK_RIGHT, (first, second) -> RULE.getAmount(first) < RULE.getAmount(first)), slots);
        var stacked = TransactionType.general(TransactionOperator.filter(TransactionOperator.STACK_RIGHT, (first, second) -> RULE.getAmount(first) == RULE.getAmount(first)), slots);
        var result = TransactionType.join(unstacked, stacked).process(cursor, builder);

        if (!result.equals(cursor)) {
            builder.cursor(result);
        }

        return builder.build();
    }

    public static @NotNull Click.Result dragClick(int countPerSlot, @NotNull List<Integer> slots, @NotNull Click.Result.Builder builder) {
        var cursor = builder.getCursorItem();
        if (cursor.isAir()) return builder.build();

        ItemStack result = TransactionType.general(TransactionOperator.stackLeftN(countPerSlot), slots).process(cursor, builder);

        if (!result.equals(cursor)) {
            builder.cursor(result);
        }

        return builder.build();
    }

    public static @NotNull Click.Result middleDragClick(@NotNull List<Integer> slots, @NotNull Click.Result.Builder builder) {
        var cursor = builder.getCursorItem();

        for (int slot : slots) {
            if (builder.get(slot).isAir()) {
                builder.set(slot, cursor);
            }
        }

        return builder.build();
    }

    public static @NotNull Click.Result dropFromCursor(int amount, @NotNull Click.Result.Builder builder) {
        var cursor = builder.getCursorItem();
        if (cursor.isAir()) return builder.build(); // Do nothing

        var pair = TransactionOperator.stackLeftN(amount).apply(ItemStack.AIR, cursor);
        if (pair == null) return builder.build();

        return builder.cursor(pair.right())
                .sideEffects(new Click.SideEffect.DropFromPlayer(pair.left()))
                .build();
    }

    public static @NotNull Click.Result dropFromSlot(int slot, int amount, @NotNull Click.Result.Builder builder) {
        var item = builder.get(slot);
        if (item.isAir()) return builder.build(); // Do nothing

        var pair = TransactionOperator.stackLeftN(amount).apply(ItemStack.AIR, item);
        if (pair == null) return builder.build();

        return builder.set(slot, pair.right())
                .sideEffects(new Click.SideEffect.DropFromPlayer(pair.left()))
                .build();
    }

    /**
     * Handles clicks, given a shift click provider and a double click provider.<br>
     * When shift clicks or double clicks need to be handled, the slots provided from the relevant handler will be
     * checked in their given order.<br>
     * For example, double clicking will collect items of the same type as the cursor; the slots provided by the double
     * click slot provider will be checked sequentially and used if they have the same type as
     * @param shiftClickSlots the shift click slot supplier
     * @param doubleClickSlots the double click slot supplier
     */
    public static @NotNull Click.Processor standard(@NotNull SlotSuggestor shiftClickSlots, @NotNull SlotSuggestor doubleClickSlots) {
        // Ugly solution, but temporary
        return (inventory, player, info) -> {
            Click.Result.Builder builder = Click.Result.builder(inventory, player);
            if (info instanceof Click.Info.Left left) {
                return leftClick(left.slot(), builder);
            } else if (info instanceof Click.Info.Right right) {
                return rightClick(right.slot(), builder);
            } else if (info instanceof Click.Info.Middle middle) {
                return middleClick(middle.slot(), builder);
            } else if (info instanceof Click.Info.LeftShift leftShift) {
                List<Integer> slots = shiftClickSlots.getList(builder, builder.get(leftShift.slot()), leftShift.slot());
                return shiftClick(leftShift.slot(), slots, builder);
            } else if (info instanceof Click.Info.RightShift rightShift) {
                List<Integer> slots = shiftClickSlots.getList(builder, builder.get(rightShift.slot()), rightShift.slot());
                return shiftClick(rightShift.slot(), slots, builder);
            } else if (info instanceof Click.Info.Double doubleClick) {
                List<Integer> slots = doubleClickSlots.getList(builder, builder.get(doubleClick.slot()), doubleClick.slot());
                return doubleClick(slots, builder);
            } else if (info instanceof Click.Info.LeftDrag drag) {
                int cursorAmount = RULE.getAmount(builder.getCursorItem());
                int amount = (int) Math.floor(cursorAmount / (double) drag.slots().size());
                return dragClick(amount, drag.slots(), builder);
            } else if (info instanceof Click.Info.RightDrag drag) {
                return dragClick(1, drag.slots(), builder);
            } else if (info instanceof Click.Info.MiddleDrag drag) {
                return middleDragClick(drag.slots(), builder);
            } else if (info instanceof Click.Info.DropSlot drop) {
                var item = builder.get(drop.slot());
                return dropFromSlot(drop.slot(), drop.all() ? RULE.getAmount(item) : 1, builder);
            } else if (info instanceof Click.Info.LeftDropCursor) {
                return dropFromCursor(builder.getCursorItem().amount(), builder);
            } else if (info instanceof Click.Info.MiddleDropCursor) {
                return builder.build();
            } else if (info instanceof Click.Info.RightDropCursor) {
                return dropFromCursor(1, builder);
            } else if (info instanceof Click.Info.HotbarSwap swap) {
                var hotbarItem = builder.getPlayer(swap.hotbarSlot());
                var selectedItem = builder.get(swap.clickedSlot());
                if (hotbarItem.equals(selectedItem)) return builder.build();

                return builder.setPlayer(swap.hotbarSlot(), selectedItem).set(swap.clickedSlot(), hotbarItem).build();
            } else if (info instanceof Click.Info.OffhandSwap swap) {
                var offhandItem = builder.getPlayer(PlayerInventoryUtils.OFF_HAND_SLOT);
                var selectedItem = builder.get(swap.slot());
                if (offhandItem.equals(selectedItem)) return builder.build();

                return builder.setPlayer(PlayerInventoryUtils.OFF_HAND_SLOT, selectedItem).set(swap.slot(), offhandItem).build();
            } else if (info instanceof Click.Info.CreativeSetItem set) {
                return builder.set(set.slot(), set.item()).build();
            } else if (info instanceof Click.Info.CreativeDropItem drop) {
                return builder.sideEffects(new Click.SideEffect.DropFromPlayer(drop.item())).build();
            } else {
                throw new IllegalArgumentException("Unknown click info " + info);
            }
        };
    }

    /**
     * A generic interface for providing options for clicks like shift clicks and double clicks.<br>
     * This addresses the issue of certain click operations only being able to interact with certain slots: for example,
     * shift clicking an item out of an inventory can only put it in the player's inner inventory slots, and will never
     * put the item anywhere else in the inventory or the player's inventory.<br>
     */
    @FunctionalInterface
    public interface SlotSuggestor {

        /**
         * Suggests slots to be used for this operation.
         * @param builder the result builder
         * @param item the item clicked
         * @param slot the slot of the clicked item
         * @return the list of slots, in order of priority, to be used for this operation
         */
        @NotNull IntStream get(@NotNull Click.Result.Builder builder, @NotNull ItemStack item, int slot);

        default @NotNull List<Integer> getList(@NotNull Click.Result.Builder builder, @NotNull ItemStack item, int slot) {
            return get(builder, item, slot).boxed().toList();
        }
    }

}
