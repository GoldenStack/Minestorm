package net.minestom.server.inventory.view;

import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a view into an inventory via the manipulation of slot IDs. They aren't tied to any specific inventory, so it
 * must be provided with each usage of the view that would need an inventory.<br>
 * <b>All valid slot IDs, regardless of the context, should be greater than or equal to zero.</b>
 */
public interface InventoryView {

    /**
     * Creates a new view that provides a view into a contiguous section, with the external ID {@code min} being mapped
     * to the local ID {@code 0} and the external id {@code max} being mapped to {@code max - min}, and vice versa,
     * including all values in-between.<br>
     * <b>Importantly, the maximum value is inclusive - so, for example, providing a view into the first four values of
     * any inventory would be {@code InventoryView.contiguous(0, 3)}</b>
     * @param min the minimum slot value
     * @param max the maximum slot value
     * @return an inventory view providing a window into the provided range
     */
    static @NotNull InventoryView contiguous(int min, int max) {
        return new InventoryViewImpl.ContiguousFork(min, max);
    }

    /**
     * A generic interface for a view that has only one slot, and thus can have simple getters and setters that don't
     * require a slot to be specified.
     */
    interface Singular extends InventoryView {

        /**
         * Gets the item in the inventory. Because this interface assumes that inventories only have one slot, the local
         * slot ID can be omitted.
         * @param inventory the inventory to get the item of
         * @return the item in the inventory
         */
        default @NotNull ItemStack get(@NotNull AbstractInventory inventory) {
            return get(inventory, 0);
        }

        /**
         * Sets the item in the inventory to the provided item. Because this interface assumes that inventories only
         * have one slot, the local slot ID can be omitted.
         * @param inventory the inventory to set the item of
         * @param itemStack the item that the inventory should be set to
         */
        default void set(@NotNull AbstractInventory inventory, @NotNull ItemStack itemStack) {
            set(inventory, 0, itemStack);
        }

    }

    /**
     * Returns the size of this view, which is the number of slots that it has.<br>
     * This number must always be greater than or equal to zero, and it indicates that the local slot IDs of 0
     * (inclusive) to {@code size()} (exclusive) must be valid. With a size of zero, no IDs are valid.
     * @return the size of this view
     */
    int size();

    /**
     * Converts the local slot ID, which is between (inclusive) 0 and (exclusive) {@link #size()}, to a valid "external"
     * slot ID. Importantly, this resultant "external" ID may be converted further, so, when considering a tree-based
     * example, it should only convert it to an ID that would be valid to its parent.<br>
     * If the provided slot ID is valid, behaviour is undefined, but -1 should generally be returned.
     * @param slot the local slot ID to convert
     * @return the non-local slot ID
     */
    int localToExternal(int slot);

    /**
     * Creates a new view that provides a view into a contiguous section of this inventory, following the same mechanics
     * as {@link #contiguous(int, int)} except for that the new view's external IDs are equivalent to the local IDs for
     * this view.
     * @param min the minimum slot value
     * @param max the maximum slot value
     * @return an inventory view providing a window into the provided range of this inventory
     */
    default @NotNull InventoryView fork(int min, int max) {
        return new InventoryViewImpl.Joiner(this, contiguous(min, max));
    }

    /**
     * Gets the item at location of the provided local slot ID in the provided inventory.
     * @param inventory the inventory to get the item from
     * @param slot the specific slot to read
     * @return the item at the slot in the inventory
     */
    default @NotNull ItemStack get(@NotNull AbstractInventory inventory, int slot) {
        return inventory.getItemStack(localToExternal(slot));
    }

    /**
     * Sets the item at the location of the provided local slot ID in the provided inventory to the item.
     * @param inventory the inventory to set the item in
     * @param slot the specific slot to set
     * @param itemStack the item to set the slot to
     */
    default void set(@NotNull AbstractInventory inventory, int slot, @NotNull ItemStack itemStack) {
        inventory.setItemStack(localToExternal(slot), itemStack);
    }

}
