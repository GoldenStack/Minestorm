package net.minestom.server.inventory.view;

import org.jetbrains.annotations.NotNull;

class InventoryViewImpl {

    record ContiguousFork(int min, int max) implements InventoryView {
        @Override
        public int size() {
            return min - max + 1; // add 1 because `max` is inclusive
        }

        @Override
        public int localToExternal(int slot) {
            return slot + min;
        }
    }

    // Joins two views together, essentially treating the child as the, well, child of the parent
    record Joiner(@NotNull InventoryView parent, @NotNull InventoryView child) implements InventoryView {
        @Override
        public int size() {
            return child.size();
        }

        @Override
        public int localToExternal(int slot) {
            return parent.localToExternal(child.localToExternal(slot));
        }
    }

}
