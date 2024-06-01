package net.minestom.server.event.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.inventory.ClickUtils;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Called before {@link InventoryClickEvent}, used to potentially cancel the click.
 */
public class InventoryPreClickEvent implements InventoryEvent, PlayerInstanceEvent, CancellableEvent {

    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private final Player player;
    private Click.Info info;

    private boolean cancelled;

    public InventoryPreClickEvent(@NotNull PlayerInventory playerInventory, @NotNull Inventory inventory,
                                  @NotNull Player player, @NotNull Click.Info info) {
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.player = player;
        this.info = info;
    }

    /**
     * Gets the player who is trying to click on the inventory.
     *
     * @return the player who clicked
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Gets the info about the click that occurred. This is enough to fully describe the click.
     *
     * @return the click info
     */
    public @NotNull Click.Info getClickInfo() {
        return info;
    }

    /**
     * Updates the information about the click that occurred. This completely overrides the previous click, but it may
     * require the inventory to be updated.
     *
     * @param info the new click info
     */
    public void setClickInfo(@NotNull Click.Info info) {
        this.info = info;
    }

    /**
     * @return the click type of this click event
     * @see ClickUtils#getType(Click.Info)
     */
    public @NotNull Click.Type getType() {
        return ClickUtils.getType(info);
    }

    /**
     * @return whether or not the slot from {@link #getSlot()} is in the player inventory (as opposed to the clicked
     *         inventory)
     */
    public boolean isInPlayerInventory() {
        int raw = ClickUtils.getSlot(info);
        return inventory instanceof PlayerInventory || (raw != -1 && raw >= inventory.getSize());
    }

    /**
     * @return the slot that was clicked
     * @see ClickUtils#getSlot(Click.Info)
     */
    public int getSlot() {
        return PlayerInventoryUtils.protocolToMinestom(ClickUtils.getSlot(info), inventory.getSize());
    }

    /**
     * Gets the item that was clicked for this event.
     * @see ClickUtils#getItem(Click.Info, Inventory, PlayerInventory)
     */
    public @NotNull ItemStack getClickedItem() {
        return ClickUtils.getItem(info, inventory, playerInventory);
    }

    /**
     * Gets the player inventory that was involved with the click.
     *
     * @return the player inventory
     */
    public @NotNull PlayerInventory getPlayerInventory() {
        return playerInventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
