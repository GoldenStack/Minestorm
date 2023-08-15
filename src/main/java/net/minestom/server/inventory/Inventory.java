package net.minestom.server.inventory;

import it.unimi.dsi.fastutil.ints.IntIterators;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickHandler;
import net.minestom.server.inventory.click.ClickPreprocessor;
import net.minestom.server.inventory.click.StandardClickHandler;
import net.minestom.server.network.packet.server.play.OpenWindowPacket;
import net.minestom.server.network.packet.server.play.WindowPropertyPacket;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an inventory which can be viewed by a collection of {@link Player}.
 * <p>
 * You can create one with {@link Inventory#Inventory(InventoryType, String)} or by making your own subclass.
 * It can then be opened using {@link Player#openInventory(AbstractInventory)}.
 */
public non-sealed class Inventory extends AbstractInventory {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    public static final @NotNull ClickHandler DEFAULT_HANDLER = new StandardClickHandler(
            (player, inventory, item, slot) -> {
                if (slot >= ClickPreprocessor.PLAYER_INVENTORY_OFFSET) {
                    return PlayerInventory.getInnerShiftClickSlots(player, inventory, item, slot);
                } else {
                    return IntIterators.fromTo(0, inventory.getSize());
                }
            },
            (player, inventory, item, slot) -> IntIterators.concat(
                    IntIterators.fromTo(0, inventory.getSize()),
                    PlayerInventory.getInnerDoubleClickSlots(player, inventory, item, slot)
            ));

    private final byte id;
    private final InventoryType inventoryType;
    private Component title;

    protected final ClickPreprocessor clickPreprocessor = new ClickPreprocessor(this);

    public Inventory(@NotNull InventoryType inventoryType, @NotNull Component title) {
        super(inventoryType.getSize());
        this.id = generateId();
        this.inventoryType = inventoryType;
        this.title = title;
    }

    public Inventory(@NotNull InventoryType inventoryType, @NotNull String title) {
        this(inventoryType, Component.text(title));
    }

    private static byte generateId() {
        return (byte) ID_COUNTER.updateAndGet(i -> i + 1 >= 128 ? 1 : i + 1);
    }

    /**
     * Gets the inventory type.
     *
     * @return the inventory type
     */
    public @NotNull InventoryType getInventoryType() {
        return inventoryType;
    }

    /**
     * Gets the inventory title.
     *
     * @return the inventory title
     */
    public @NotNull Component getTitle() {
        return title;
    }

    /**
     * Changes the inventory title.
     *
     * @param title the new inventory title
     */
    public void setTitle(@NotNull Component title) {
        this.title = title;

        // Reopen and update this inventory with the new title
        sendPacketToViewers(new OpenWindowPacket(getWindowId(), getInventoryType().getWindowType(), title));
        update();
    }

    /**
     * Gets this window id.
     * <p>
     * This is the id that the client will send to identify the affected inventory, mostly used by packets.
     *
     * @return the window id
     */
    @Override
    public byte getWindowId() {
        return id;
    }

    @Override
    public void handleOpen(@NotNull Player player) {
        player.sendPacket(new OpenWindowPacket(getWindowId(), inventoryType.getWindowType(), getTitle()));
        super.handleOpen(player);
    }

    @Override
    public @NotNull ClickPreprocessor getClickPreprocessor() {
        return clickPreprocessor;
    }

    @Override
    public @NotNull ClickHandler getClickHandler() {
        return DEFAULT_HANDLER;
    }

    /**
     * Sends a window property to all viewers.
     *
     * @param property the property to send
     * @param value    the value of the property
     * @see <a href="https://wiki.vg/Protocol#Window_Property">https://wiki.vg/Protocol#Window_Property</a>
     */
    protected void sendProperty(@NotNull InventoryProperty property, short value) {
        sendPacketToViewers(new WindowPropertyPacket(getWindowId(), property.getProperty(), value));
    }

}
