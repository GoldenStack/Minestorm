package net.minestom.server.inventory.click;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.inventory.InventoryClickEvent;
import net.minestom.server.event.inventory.InventoryPostClickEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles different types of clicks by players in an inventory.
 * The inventory is provided to this handler in the case of handlers that don't have internal state and may manage
 * multiple inventories, but it's also possible to store the inventory yourself and control usages of it.
 */
public interface ClickHandler {

    /**
     * Handles the provided click from the given player, returning the results after it is applied. If the results are
     * null, this indicates that the click was cancelled or was otherwise not processed.
     *
     * @param inventory the clicked inventory
     * @param player the player that clicked
     * @param info the information about the player's click
     * @return the results of the click, or null if the click was cancelled or otherwise was not handled
     */
    default @Nullable Click.Result handleClick(@NotNull Inventory inventory, @NotNull Player player, @NotNull Click.Info info) {
        InventoryPreClickEvent preClickEvent = new InventoryPreClickEvent(player.getInventory(), inventory, player, info);
        EventDispatcher.call(preClickEvent);

        Click.Info newInfo = preClickEvent.getClickInfo();

        if (!preClickEvent.isCancelled()) {
            Click.Result changes = handle(newInfo, Click.Result.builder(inventory, player));

            InventoryClickEvent clickEvent = new InventoryClickEvent(player.getInventory(), inventory, player, newInfo, changes);
            EventDispatcher.call(clickEvent);

            if (!clickEvent.isCancelled()) {
                Click.Result newChanges = clickEvent.getChanges();
                newChanges.applyChanges(player, inventory);

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
     * Handles the provided click info that is of any type.
     * @param info the info, of unknown type
     * @param builder the click result builder for this click
     * @return the changes that were calculated
     */
    default @NotNull Click.Result handle(@NotNull Click.Info info, @NotNull Click.Result.Builder builder) {
        if (info instanceof Click.Info.Left left) {
            return left(left, builder);
        } else if (info instanceof Click.Info.Right right) {
            return right(right, builder);
        } else if (info instanceof Click.Info.Middle middle) {
            return middle(middle, builder);
        } else if (info instanceof Click.Info.LeftShift leftShift) {
            return leftShift(leftShift, builder);
        } else if (info instanceof Click.Info.RightShift rightShift) {
            return rightShift(rightShift, builder);
        } else if (info instanceof Click.Info.Double doubleClick) {
            return doubleClick(doubleClick, builder);
        } else if (info instanceof Click.Info.LeftDrag drag) {
            return leftDrag(drag, builder);
        } else if (info instanceof Click.Info.MiddleDrag drag) {
            return middleDrag(drag, builder);
        } else if (info instanceof Click.Info.RightDrag drag) {
            return rightDrag(drag, builder);
        } else if (info instanceof Click.Info.DropSlot drop) {
            return dropSlot(drop, builder);
        } else if (info instanceof Click.Info.LeftDropCursor drop) {
            return leftDropCursor(drop, builder);
        } else if (info instanceof Click.Info.MiddleDropCursor drop) {
            return middleDropCursor(drop, builder);
        } else if (info instanceof Click.Info.RightDropCursor drop) {
            return rightDropCursor(drop, builder);
        } else if (info instanceof Click.Info.HotbarSwap swap) {
            return hotbarSwap(swap, builder);
        } else if (info instanceof Click.Info.OffhandSwap swap) {
            return offhandSwap(swap, builder);
        } else if (info instanceof Click.Info.CreativeSetItem set) {
            return creativeSetItem(set, builder);
        } else if (info instanceof Click.Info.CreativeDropItem drop) {
            return creativeDropItem(drop, builder);
        } else {
            throw new IllegalArgumentException("Unknown click info " + info);
        }
    }

    @NotNull Click.Result left(@NotNull Click.Info.Left info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result right(@NotNull Click.Info.Right info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result middle(@NotNull Click.Info.Middle info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result leftShift(@NotNull Click.Info.LeftShift info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result rightShift(@NotNull Click.Info.RightShift info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result doubleClick(@NotNull Click.Info.Double info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result leftDropCursor(@NotNull Click.Info.LeftDropCursor info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result middleDropCursor(@NotNull Click.Info.MiddleDropCursor info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result rightDropCursor(@NotNull Click.Info.RightDropCursor info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result leftDrag(@NotNull Click.Info.LeftDrag info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result middleDrag(@NotNull Click.Info.MiddleDrag info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result rightDrag(@NotNull Click.Info.RightDrag info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result dropSlot(@NotNull Click.Info.DropSlot info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result hotbarSwap(@NotNull Click.Info.HotbarSwap info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result offhandSwap(@NotNull Click.Info.OffhandSwap info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result creativeSetItem(@NotNull Click.Info.CreativeSetItem info, @NotNull Click.Result.Builder builder);

    @NotNull Click.Result creativeDropItem(@NotNull Click.Info.CreativeDropItem info, @NotNull Click.Result.Builder builder);

}
