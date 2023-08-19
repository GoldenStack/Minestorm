package net.minestom.server.event.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.ClickInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Called before {@link InventoryClickEvent}, used to potentially cancel the click.
 */
public class InventoryPreClickEvent implements InventoryEvent, PlayerInstanceEvent, CancellableEvent {

    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private final Player player;
    private ClickInfo clickInfo;
    private boolean shouldUpdate;

    private boolean cancelled;

    public InventoryPreClickEvent(@NotNull PlayerInventory playerInventory, @NotNull Inventory inventory,
                                  @NotNull Player player, @NotNull ClickInfo clickInfo) {
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.player = player;
        this.clickInfo = clickInfo;
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
    public @NotNull ClickInfo getClickInfo() {
        return clickInfo;
    }

    /**
     * Updates the information about the click that occurred. This completely overrides the previous click, but it may
     * require the inventory to be updated.
     *
     * @param info the new click info
     */
    public void setClickInfo(@NotNull ClickInfo info) {
        this.clickInfo = info;
    }

    /**
     * Gets whether or not the inventory should be forced to update after this click.
     *
     * @return whether or not the inventory should update
     */
    public boolean shouldUpdate() {
        return shouldUpdate;
    }

    /**
     * Sets whether or not the inventory should be forced to update after this click. This should be used when changing
     * the click info of this event, as the client doesn't know that it has been changed.
     *
     * @param shouldUpdate whether or not the inventory should update
     */
    public void setShouldUpdate(boolean shouldUpdate) {
        this.shouldUpdate = shouldUpdate;
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
