package net.minestom.server.inventory.view.idea1;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public interface InventoryView {

    int slots();

    int mapSlot(int slot);

    @NotNull AbstractInventory source();

}
