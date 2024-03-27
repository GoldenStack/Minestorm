package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.ints.*;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Click {

    /**
     * Contains information about a click. These are equal to the packet slot IDs from <a href="https://wiki.vg/Inventory">the Minecraft protocol.</a>.
     * The inventory used should be known from context.
     */
    public sealed interface Info {

        record Left(int slot) implements Info {}
        record Right(int slot) implements Info {}
        record Middle(int slot) implements Info {} // Creative only

        record LeftShift(int slot) implements Info {}
        record RightShift(int slot) implements Info {}

        record Double(int slot) implements Info {}

        record LeftDrag(List<Integer> slots) implements Info {}
        record RightDrag(List<Integer> slots) implements Info {}
        record MiddleDrag(List<Integer> slots) implements Info {} // Creative only

        record LeftDropCursor() implements Info {}
        record RightDropCursor() implements Info {}
        record MiddleDropCursor() implements Info {}

        record DropSlot(int slot, boolean all) implements Info {}

        record HotbarSwap(int hotbarSlot, int clickedSlot) implements Info {}
        record OffhandSwap(int slot) implements Info {}

        record CreativeSetItem(int slot, @NotNull ItemStack item) implements Info {}
        record CreativeDropItem(@NotNull ItemStack item) implements Info {}

    }

    /**
     * Stores changes that occurred or will occur as the result of a click.
     * @param changes the map of changes that will occur to the inventory
     * @param playerInventoryChanges the map of changes that will occur to the player inventory
     * @param newCursorItem the player's cursor item after this click. Null indicates no change
     * @param sideEffects the side effects of this click
     */
    public record Result(@NotNull Map<Integer, ItemStack> changes, @NotNull Map<Integer, ItemStack> playerInventoryChanges,
                         @Nullable ItemStack newCursorItem, @Nullable Click.Result.SideEffects sideEffects) {

        public Result {
            changes = Map.copyOf(changes);
            playerInventoryChanges = Map.copyOf(playerInventoryChanges);
        }

        public static @NotNull Click.Result.Builder builder(@NotNull Inventory clickedInventory, @NotNull Player player) {
            return builder(clickedInventory, player.getInventory(), player.getInventory().getCursorItem());
        }

        public static @NotNull Click.Result.Builder builder(@NotNull Inventory clickedInventory, @NotNull Inventory playerInventory, @NotNull ItemStack cursor) {
            return new Builder(clickedInventory, playerInventory, cursor);
        }

        public static final class Builder implements Int2ObjectFunction<ItemStack> {
            private final @NotNull Inventory clickedInventory;
            private final @NotNull Inventory playerInventory;
            private final @NotNull ItemStack cursorItem;

            private final Int2ObjectMap<ItemStack> changes = new Int2ObjectArrayMap<>();
            private final Int2ObjectMap<ItemStack> playerInventoryChanges = new Int2ObjectArrayMap<>();
            private @Nullable ItemStack newCursorItem;

            private @Nullable Click.Result.SideEffects sideEffects;

            Builder(@NotNull Inventory clickedInventory, @NotNull Inventory playerInventory, @NotNull ItemStack cursor) {
                this.clickedInventory = clickedInventory;
                this.playerInventory = playerInventory;
                this.cursorItem = cursor;
            }

            public @NotNull Inventory clickedInventory() {
                return clickedInventory;
            }

            public @NotNull Inventory playerInventory() {
                return playerInventory;
            }

            public @NotNull ItemStack getCursorItem() {
                return cursorItem;
            }

            @Override
            public @NotNull ItemStack get(int slot) {
                if (slot >= clickedInventory.getSize()) {
                    int converted = PlayerInventoryUtils.protocolToMinestom(slot, clickedInventory);

                    return playerInventoryChanges.containsKey(converted) ?
                            playerInventoryChanges.get(converted) : playerInventory().getItemStack(converted);
                } else {
                    return changes.containsKey(slot) ?
                            changes.get(slot) : clickedInventory.getItemStack(slot);
                }
            }

            @Override
            public ItemStack put(int key, ItemStack value) {
                ItemStack get = get(key);
                change(key, value);
                return get;
            }

            public @NotNull Click.Result.Builder change(int slot, @NotNull ItemStack item) {
                if (slot >= clickedInventory.getSize()) {
                    change(PlayerInventoryUtils.protocolToMinestom(slot, clickedInventory), item, true);
                } else {
                    change(slot, item, false);
                }
                return this;
            }

            public @NotNull Click.Result.Builder change(int slot, @NotNull ItemStack item, boolean playerInventory) {
                (playerInventory ? playerInventoryChanges : changes).put(slot, item);
                return this;
            }

            public @NotNull Click.Result.Builder cursor(@Nullable ItemStack newCursorItem) {
                this.newCursorItem = newCursorItem;
                return this;
            }

            public @NotNull Click.Result.Builder sideEffects(@Nullable Click.Result.SideEffects sideEffects) {
                this.sideEffects = sideEffects;
                return this;
            }

            public @NotNull Click.Result build() {
                return new Result(
                        changes, playerInventoryChanges,
                        newCursorItem, sideEffects
                );
            }

        }

        /**
         * Applies the changes of this result to the player and the clicked inventory.
         * @param player the player who clicked
         * @param clickedInventory the inventory that was clicked in
         */
        public void applyChanges(@NotNull Player player, @NotNull Inventory clickedInventory) {
            for (var entry : changes.entrySet()) {
                clickedInventory.setItemStack(entry.getKey(), entry.getValue());
            }

            for (var entry : playerInventoryChanges.entrySet()) {
                player.getInventory().setItemStack(entry.getKey(), entry.getValue());
            }

            if (newCursorItem != null) {
                player.getInventory().setCursorItem(newCursorItem);
            }

            if (sideEffects != null) {
                sideEffects.apply(player, clickedInventory);
            }
        }

        /**
         * Represents side effects that may occur as the result of an inventory click.
         */
        public interface SideEffects {

            /**
             * A side effect that results in the player dropping an item.
             * @param item the dropped item
             */
            record DropFromPlayer(@NotNull ItemStack item) implements SideEffects {
                @Override
                public void apply(@NotNull Player player, @NotNull Inventory clickedInventory) {
                    player.dropItem(item);
                }
            }

            /**
             * Applies these side effects to the provided player and their open inventory.
             * @param player the player who clicked
             * @param clickedInventory the clicked inventory
             */
            void apply(@NotNull Player player, @NotNull Inventory clickedInventory);
        }
    }
}
