package net.minestom.server.inventory.view.idea2;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public interface InventoryView {

    int slots();

    int mapSlot(int slot);

    @NotNull AbstractInventory source();

    interface Tree extends InventoryView {

        @NotNull InventoryView parent();

        @Override
        default @NotNull AbstractInventory source() {
            return parent().source();
        }
    }

}
