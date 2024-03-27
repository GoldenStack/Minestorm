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
    default @Nullable ClickResult handleClick(@NotNull Inventory inventory, @NotNull Player player, @NotNull Click.Info info) {
        InventoryPreClickEvent preClickEvent = new InventoryPreClickEvent(player.getInventory(), inventory, player, info);
        EventDispatcher.call(preClickEvent);

        Click.Info newInfo = preClickEvent.getClickInfo();

        if (!preClickEvent.isCancelled()) {
            ClickResult changes = handle(newInfo, ClickResult.builder(player, inventory)).build();

            InventoryClickEvent clickEvent = new InventoryClickEvent(player.getInventory(), inventory, player, newInfo, changes);
            EventDispatcher.call(clickEvent);

            if (!clickEvent.isCancelled()) {
                ClickResult newChanges = clickEvent.getChanges();
                newChanges.applyChanges(player, inventory);

                var postClickEvent = new InventoryPostClickEvent(player, inventory, newInfo, newChanges);
                EventDispatcher.call(postClickEvent);

                if (!clickInfo.equals(newInfo) || !changes.equals(newChanges)) {
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
    default @NotNull ClickResult.Builder handle(@NotNull Click.Info info, @NotNull ClickResult.Builder builder) {
        if (info instanceof Click.Info.Left left) {
            left(left, builder);
        } else if (info instanceof Click.Info.Right right) {
            right(right, builder);
        } else if (info instanceof Click.Info.Middle middle) {
            middle(middle, builder);
        } else if (info instanceof Click.Info.LeftShift leftShift) {
            leftShift(leftShift, builder);
        } else if (info instanceof Click.Info.RightShift rightShift) {
            rightShift(rightShift, builder);
        } else if (info instanceof Click.Info.Double doubleClick) {
            doubleClick(doubleClick, builder);
        } else if (info instanceof Click.Info.LeftDrag drag) {
            leftDrag(drag, builder);
        } else if (info instanceof Click.Info.MiddleDrag drag) {
            middleDrag(drag, builder);
        } else if (info instanceof Click.Info.RightDrag drag) {
            rightDrag(drag, builder);
        } else if (info instanceof Click.Info.DropSlot drop) {
            dropSlot(drop, builder);
        } else if (info instanceof Click.Info.LeftDropCursor drop) {
            leftDropCursor(drop, builder);
        } else if (info instanceof Click.Info.MiddleDropCursor drop) {
            middleDropCursor(drop, builder);
        } else if (info instanceof Click.Info.RightDropCursor drop) {
            rightDropCursor(drop, builder);
        } else if (info instanceof Click.Info.HotbarSwap swap) {
            hotbarSwap(swap, builder);
        } else if (info instanceof Click.Info.OffhandSwap swap) {
            offhandSwap(swap, builder);
        } else if (info instanceof Click.Info.CreativeSetItem set) {
            creativeSetItem(set, builder);
        } else if (info instanceof Click.Info.CreativeDropItem drop) {
            creativeDropItem(drop, builder);
        }

        return builder;
    }

    void left(@NotNull Click.Info.Left info, @NotNull ClickResult.Builder builder);

    void right(@NotNull Click.Info.Right info, @NotNull ClickResult.Builder builder);

    void middle(@NotNull Click.Info.Middle info, @NotNull ClickResult.Builder builder);

    void leftShift(@NotNull Click.Info.LeftShift info, @NotNull ClickResult.Builder builder);

    void rightShift(@NotNull Click.Info.RightShift info, @NotNull ClickResult.Builder builder);

    void doubleClick(@NotNull Click.Info.Double info, @NotNull ClickResult.Builder builder);

    void leftDropCursor(@NotNull Click.Info.LeftDropCursor info, @NotNull ClickResult.Builder builder);

    void middleDropCursor(@NotNull Click.Info.MiddleDropCursor info, @NotNull ClickResult.Builder builder);

    void rightDropCursor(@NotNull Click.Info.RightDropCursor info, @NotNull ClickResult.Builder builder);

    void leftDrag(@NotNull Click.Info.LeftDrag info, @NotNull ClickResult.Builder builder);

    void middleDrag(@NotNull Click.Info.MiddleDrag info, @NotNull ClickResult.Builder builder);

    void rightDrag(@NotNull Click.Info.RightDrag info, @NotNull ClickResult.Builder builder);

    void dropSlot(@NotNull Click.Info.DropSlot info, @NotNull ClickResult.Builder builder);

    void hotbarSwap(@NotNull Click.Info.HotbarSwap info, @NotNull ClickResult.Builder builder);

    void offhandSwap(@NotNull Click.Info.OffhandSwap info, @NotNull ClickResult.Builder builder);

    void creativeSetItem(@NotNull Click.Info.CreativeSetItem info, @NotNull ClickResult.Builder builder);

    void creativeDropItem(@NotNull Click.Info.CreativeDropItem info, @NotNull ClickResult.Builder builder);

}
