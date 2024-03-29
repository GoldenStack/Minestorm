package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.inventory.InventoryClickEvent;
import net.minestom.server.event.inventory.InventoryPostClickEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
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
import java.util.function.BiFunction;

public final class Click {

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

        private final Map<Integer, List<Integer>> leftDraggingMap = new HashMap<>();
        private final Map<Integer, List<Integer>> rightDraggingMap = new HashMap<>();
        private final Map<Integer, List<Integer>> creativeDragMap = new HashMap<>();

        public Preprocessor(int size, boolean isPlayerInventory) {
            this.size = size;
            this.isPlayerInventory = isPlayerInventory;
        }

        public Preprocessor(@NotNull Inventory inventory) {
            this(inventory.getSize(), inventory instanceof PlayerInventory);
        }

        public void clearCache(int id) {
            leftDraggingMap.remove(id);
            rightDraggingMap.remove(id);
            creativeDragMap.remove(id);
        }

        private boolean validate(int slot) {
            return slot >= 0 && slot < size + (isPlayerInventory ? 0 : PlayerInventoryUtils.INNER_SIZE);
        }

        /**
         * Processes the provided click packet, turning it into a {@link Info}.
         * This will do simple verification of the packet before sending it to {@link #process(int, ClientClickWindowPacket.ClickType, int, byte)}.
         *
         * @param player the player clicking
         * @param packet the raw click packet
         * @return the information about the click, or nothing if there was no immediately usable information
         */
        public @Nullable Click.Info process(@NotNull Player player, @NotNull ClientClickWindowPacket packet) {
            final int originalSlot = packet.slot();
            final byte button = packet.button();
            final ClientClickWindowPacket.ClickType type = packet.clickType();

            int slot = isPlayerInventory ? PlayerInventoryUtils.protocolToMinestom(originalSlot) : originalSlot;
            if (originalSlot == -999) slot = -999;

            boolean creativeRequired = switch (type) {
                case CLONE -> true;
                case QUICK_CRAFT -> button == 8 || button == 9 || button == 10;
                default -> false;
            };

            if (creativeRequired && !player.isCreative()) return null;

            return process(player.getEntityId(), type, slot, button);
        }

        /**
         * Processes a packet into click info.
         *
         * @param playerId the player id to use
         * @param type     the type of the click
         * @param slot     the clicked slot
         * @param button   the sent button
         * @return the information about the click, or nothing if there was no immediately usable information
         */
        public @Nullable Click.Info process(int playerId, @NotNull ClientClickWindowPacket.ClickType type, int slot, byte button) {
            return switch (type) {
                case PICKUP -> {
                    if (slot == -999) {
                        yield switch (button) {
                            case 0 -> new Info.LeftDropCursor();
                            case 1 -> new Info.RightDropCursor();
                            case 2 -> new Info.MiddleDropCursor();
                            default -> null;
                        };
                    }

                    if (!validate(slot)) yield null;

                    yield switch (button) {
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
                case CLONE -> validate(slot) ? new Info.Middle(slot) : null;
                case THROW -> validate(slot) ? new Info.DropSlot(slot, button == 1) : null;
                case QUICK_CRAFT -> {
                    // Handle drag finishes
                    if (button == 2) {
                        var list = leftDraggingMap.remove(playerId);
                        yield new Info.LeftDrag(list == null ? List.of() : List.copyOf(list));
                    } else if (button == 6) {
                        var list = rightDraggingMap.remove(playerId);
                        yield new Info.RightDrag(list == null ? List.of() : List.copyOf(list));
                    } else if (button == 10) {
                        var list = creativeDragMap.remove(playerId);
                        yield new Info.MiddleDrag(list == null ? List.of() : List.copyOf(list));
                    }

                    // Handle intermediate state
                    BiFunction<Integer, List<Integer>, List<Integer>> addItem = (k, v) -> {
                        List<Integer> v2 = v != null ? v : new ArrayList<>();
                        if (validate(slot)) {
                            if (!v2.contains(slot)) {
                                v2.add(slot);
                            }
                        }
                        return v2;
                    };

                    switch (button) {
                        case 0 -> leftDraggingMap.remove(playerId);
                        case 4 -> rightDraggingMap.remove(playerId);
                        case 8 -> creativeDragMap.remove(playerId);

                        case 1 -> leftDraggingMap.compute(playerId, addItem);
                        case 5 -> rightDraggingMap.compute(playerId, addItem);
                        case 9 -> creativeDragMap.compute(playerId, addItem);
                    }

                    yield null;
                }
                case PICKUP_ALL -> validate(slot) ? new Info.Double(slot) : null;
            };
        }

    }

    /**
     * Handles different types of clicks by players in an inventory.
     * The inventory is provided to this handler in the case of handlers that don't have internal state and may manage
     * multiple inventories, but it's also possible to store the inventory yourself and control usages of it.
     */
    public interface Processor {

        /**
         * Processes a click, returning a result. This will call events for the click.
         * @param inventory the clicked inventory (could be a player inventory)
         * @param player the player who clicked
         * @param info the click info describing the click
         * @return the click result, or null if the click did not occur
         */
        default @Nullable Click.Result handleClick(@NotNull Inventory inventory, @NotNull Player player, @NotNull Click.Info info) {
            InventoryPreClickEvent preClickEvent = new InventoryPreClickEvent(player.getInventory(), inventory, player, info);
            EventDispatcher.call(preClickEvent);

            Info newInfo = preClickEvent.getClickInfo();

            if (!preClickEvent.isCancelled()) {
                Result changes = processClick(inventory, player, newInfo);

                InventoryClickEvent clickEvent = new InventoryClickEvent(player.getInventory(), inventory, player, newInfo, changes);
                EventDispatcher.call(clickEvent);

                if (!clickEvent.isCancelled()) {
                    Result newChanges = clickEvent.getChanges();

                    Result.handle(newChanges, player, inventory);

                    var postClickEvent = new InventoryPostClickEvent(player, inventory, newInfo, newChanges);
                    EventDispatcher.call(postClickEvent);

                    if (!info.equals(newInfo) || !changes.equals(newChanges)) {
                        inventory.update(player);
                        if (inventory != player.getInventory()) {
                            player.getInventory().update(player);
                        }
                    }

                    return newChanges;
                }
            }

            inventory.update(player);
            if (inventory != player.getInventory()) {
                player.getInventory().update(player);
            }
            return null;
        }

        /**
         * Processes a click, returning a result. This should be a pure function with no side effects.
         * @param inventory the clicked inventory (could be a player inventory)
         * @param player the player who clicked
         * @param info the click info describing the click
         * @return the click result
         */
        @NotNull Click.Result processClick(@NotNull Inventory inventory, @NotNull Player player, @NotNull Click.Info info);

    }

    /**
     * Stores changes that occurred or will occur as the result of a click.
     * @param changes the map of changes that will occur to the inventory
     * @param playerInventoryChanges the map of changes that will occur to the player inventory
     * @param newCursorItem the player's cursor item after this click. Null indicates no change
     * @param sideEffects the side effects of this click
     */
    public record Result(@NotNull Map<Integer, ItemStack> changes, @NotNull Map<Integer, ItemStack> playerInventoryChanges,
                         @Nullable ItemStack newCursorItem, @Nullable Click.SideEffect sideEffects) {

        public Result {
            changes = Map.copyOf(changes);
            playerInventoryChanges = Map.copyOf(playerInventoryChanges);
        }

        static void handle(@NotNull Result result, @NotNull Player player, @NotNull Inventory inventory) {
            for (var entry : result.changes().entrySet()) {
                inventory.setItemStack(entry.getKey(), entry.getValue());
            }

            for (var entry : result.playerInventoryChanges().entrySet()) {
                player.getInventory().setItemStack(entry.getKey(), entry.getValue());
            }

            if (result.newCursorItem() != null) {
                player.getInventory().setCursorItem(result.newCursorItem());
            }

            if (result.sideEffects() instanceof SideEffect.DropFromPlayer drop) {
                for (ItemStack item : drop.items()) {
                    player.dropItem(item);
                }
            }
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

            private @Nullable Click.SideEffect sideEffects;

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
                    ItemStack recent = changes.get(slot);
                    return recent != null ? recent : clickedInventory.getItemStack(slot);
                }
            }

            public @NotNull ItemStack getPlayer(int slot) {
                ItemStack recent = playerInventoryChanges.get(slot);
                return recent != null ? recent : playerInventory.getItemStack(slot);
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

            public @NotNull Click.Result.Builder sideEffects(@Nullable Click.SideEffect sideEffects) {
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
    }

    /**
     * Represents side effects that may occur as the result of an inventory click.
     */
    public sealed interface SideEffect {

        record DropFromPlayer(@NotNull List<ItemStack> items) implements SideEffect {

            public DropFromPlayer {
                items = List.copyOf(items);
            }

            public DropFromPlayer(@NotNull ItemStack @NotNull ... items) {
                this(List.of(items));
            }
        }
    }
}
