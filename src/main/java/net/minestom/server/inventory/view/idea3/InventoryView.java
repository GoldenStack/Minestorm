package net.minestom.server.inventory.view.idea3;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public interface InventoryView {

    int slots();

    int mapSlot(int slot);

    @NotNull AbstractInventory source();

    interface Root extends InventoryView {
        @Override
        default int slots() {
            return source().getSize();
        }

        @Override
        default int mapSlot(int slot) {
            return slot;
        }
    }

    interface Tree extends InventoryView {

        @NotNull InventoryView parent();

        @Override
        default @NotNull AbstractInventory source() {
            return parent().source();
        }
    }

    interface OffsetTree extends Tree {

        int offset();

        @Override
        default int mapSlot(int slot) {
            return parent().mapSlot(slot + offset());
        }
    }

}
