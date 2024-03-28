package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

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
     * Preprocesses click packets for an inventory, turning them into {@link Info} instances for further processing.
     */
    public static final class Preprocessor {

        private final int size;
        private final boolean isPlayerInventory;

        private final Map<Player, List<Integer>> leftDraggingMap = new ConcurrentHashMap<>();
        private final Map<Player, List<Integer>> rightDraggingMap = new ConcurrentHashMap<>();
        private final Map<Player, List<Integer>> creativeDragMap = new ConcurrentHashMap<>();

        public Preprocessor(int size, boolean isPlayerInventory) {
            this.size = size;
            this.isPlayerInventory = isPlayerInventory;
        }

        public Preprocessor(@NotNull Inventory inventory) {
            this(inventory.getSize(), inventory instanceof PlayerInventory);
        }

        public void clearCache(@NotNull Player player) {
            leftDraggingMap.remove(player);
            rightDraggingMap.remove(player);
            creativeDragMap.remove(player);
        }

        private boolean validate(int slot) {
            return slot >= 0 && slot < size + (isPlayerInventory ? 0 : PlayerInventoryUtils.INNER_SIZE);
        }

        /**
         * Processes the provided click packet, turning it into a {@link Info}.
         * @param player the player clicking
         * @param packet the raw click packet
         * @return the information about the click, or nothing if there was no immediately usable information
         */
        public @Nullable Click.Info process(@NotNull Player player, @NotNull ClientClickWindowPacket packet) {
            final int originalSlot = packet.slot();
            final byte button = packet.button();
            final ClientClickWindowPacket.ClickType clickType = packet.clickType();

            final int slot = isPlayerInventory ? PlayerInventoryUtils.protocolToMinestom(originalSlot) : originalSlot;

            return switch (clickType) {
                case PICKUP -> {
                    if (originalSlot == -999) {
                        yield switch (button) {
                            case 0 -> new Info.LeftDropCursor();
                            case 1 -> new Info.RightDropCursor();
                            case 2 -> new Info.MiddleDropCursor();
                            default -> null;
                        };
                    }

                    if (!validate(slot)) yield null;

                    yield switch(button) {
                        case 0 -> new Info.Left(slot);
                        case 1 -> new Info.Right(slot);
                        default -> null;
                    };
                }
                case QUICK_MOVE -> {
                    if (!validate(slot)) yield null;
                    yield button == 0 ? new Info.LeftShift(slot) : new Info.RightShift(slot);
                }
                case SWAP -> {
                    if (!validate(slot)) {
                        yield null;
                    } else if (button >= 0 && button < 9) {
                        yield new Info.HotbarSwap(button, slot);
                    } else if (button == 40) {
                        yield new Info.OffhandSwap(slot);
                    } else {
                        yield null;
                    }
                }
                case CLONE -> (player.isCreative() && validate(slot)) ? new Info.Middle(slot) : null;
                case THROW -> validate(slot) ? new Info.DropSlot(slot, button == 1) : null;
                case QUICK_CRAFT -> {
                    // Prevent invalid creative actions
                    if (!player.isCreative() && (button == 8 || button == 9 || button == 10)) yield null;

                    // Handle drag finishes
                    if (button == 2) {
                        var list = leftDraggingMap.remove(player);
                        yield new Info.LeftDrag(list == null ? List.of() : List.copyOf(list));
                    } else if (button == 6) {
                        var list = rightDraggingMap.remove(player);
                        yield new Info.RightDrag(list == null ? List.of() : List.copyOf(list));
                    } else if (button == 10) {
                        var list = creativeDragMap.remove(player);
                        yield new Info.MiddleDrag(list == null ? List.of() : List.copyOf(list));
                    }

                    // Handle intermediate state
                    BiFunction<Player, List<Integer>, List<Integer>> addItem = (k, v) -> {
                        List<Integer> v2 = v != null ? v : new ArrayList<>();
                        if (validate(slot)) {
                            if (!v2.contains(slot)) {
                                v2.add(slot);
                            }
                        }
                        return v2;
                    };

                    switch (button) {
                        case 0 -> leftDraggingMap.remove(player);
                        case 4 -> rightDraggingMap.remove(player);
                        case 8 -> creativeDragMap.remove(player);

                        case 1 -> leftDraggingMap.compute(player, addItem);
                        case 5 -> rightDraggingMap.compute(player, addItem);
                        case 9 -> creativeDragMap.compute(player, addItem);
                    }

                    yield null;
                }
                case PICKUP_ALL -> validate(slot) ? new Info.Double(slot) : null;
            };
        }

    }

    /**
     * Stores changes that occurred or will occur as the result of a click.
     * @param changes the map of changes that will occur to the inventory
     * @param playerInventoryChanges the map of changes that will occur to the player inventory
     * @param newCursorItem the player's cursor item after this click. Null indicates no change
     * @param sideEffects the side effects of this click
     */
    public record Result(@NotNull Map<Integer, ItemStack> changes, @NotNull Map<Integer, ItemStack> playerInventoryChanges,
                         @Nullable ItemStack newCursorItem, @Nullable Click.Result.SideEffect sideEffects) {

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

            private final Map<Integer, ItemStack> changes = new HashMap<>();
            private final Map<Integer, ItemStack> playerInventoryChanges = new HashMap<>();
            private @Nullable ItemStack newCursorItem;

            private @Nullable Click.Result.SideEffect sideEffects;

            Builder(@NotNull Inventory clickedInventory, @NotNull Inventory playerInventory, @NotNull ItemStack cursor) {
                this.clickedInventory = clickedInventory;
                this.playerInventory = playerInventory;
                this.cursorItem = cursor;
            }

            public int clickedSize() {
                return clickedInventory.getSize();
            }

            public @NotNull ItemStack getCursorItem() {
                return newCursorItem == null ? cursorItem : newCursorItem;
            }

            @Override
            public @NotNull ItemStack get(int slot) {
                if (slot >= clickedSize()) {
                    int converted = PlayerInventoryUtils.protocolToMinestom(slot, clickedInventory.getSize());
                    return getPlayer(converted);
                } else {
                    return changes.containsKey(slot) ?
                            changes.get(slot) : clickedInventory.getItemStack(slot);
                }
            }

            public @NotNull ItemStack getPlayer(int slot) {
                return playerInventoryChanges.containsKey(slot) ?
                        playerInventoryChanges.get(slot) : this.playerInventory.getItemStack(slot);
            }

            @Override
            public ItemStack put(int key, ItemStack value) {
                ItemStack get = get(key);
                set(key, value);
                return get;
            }

            public @NotNull Click.Result.Builder set(int slot, @NotNull ItemStack item) {
                if (slot >= clickedInventory.getSize()) {
                    int converted = PlayerInventoryUtils.protocolToMinestom(slot, clickedInventory.getSize());
                    return setPlayer(converted, item);
                } else {
                    changes.put(slot, item);
                    return this;
                }
            }

            public @NotNull Click.Result.Builder setPlayer(int slot, @NotNull ItemStack item) {
                playerInventoryChanges.put(slot, item);
                return this;
            }

            public @NotNull Click.Result.Builder cursor(@Nullable ItemStack newCursorItem) {
                this.newCursorItem = newCursorItem;
                return this;
            }

            public @NotNull Click.Result.Builder sideEffects(@Nullable Click.Result.SideEffect sideEffects) {
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

            if (sideEffects instanceof SideEffect.DropFromPlayer drop) {
                for (ItemStack item : drop.items()) {
                    player.dropItem(item);
                }
            }
        }

        /**
         * Represents side effects that may occur as the result of an inventory click.
         */
        public sealed interface SideEffect {

            record DropFromPlayer(@NotNull List<ItemStack> items) implements SideEffect {
                public DropFromPlayer(@NotNull ItemStack @NotNull ... items) {
                    this(List.of(items));
                }
            }
        }
    }
}
