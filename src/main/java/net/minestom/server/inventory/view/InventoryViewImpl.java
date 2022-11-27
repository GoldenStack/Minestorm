package net.minestom.server.inventory.view;

import org.jetbrains.annotations.NotNull;

class InventoryViewImpl {

    // Implements InventoryView.Singular so that it can be treated as it when needed
    //  (i.e. when we know that it must be singular, and so code duplication can be avoided)
    // This is safe because implementing Singular doesn't actually have any side effects; it just adds utility methods.
    //  (if Singular ever gets any actual side effects, this needs to be changed)
    record ContiguousFork(int min, int max) implements InventoryView.Singular {
        @Override
        public int size() {
            return max - min + 1; // add 1 because `max` is inclusive
        }

        @Override
        public int localToExternal(int slot) {
            if (slot < 0 || slot >= size()) {
                return -1;
            }
            return slot + min;
        }
    }

    // Joins two views together, essentially treating the child as the, well, child of the parent
    // Implements InventoryView.Singular so that it can be treated as it when needed
    //  (i.e. when we know that it must be singular, and so code duplication can be avoided)
    // This is safe because implementing Singular doesn't actually have any side effects; it just adds utility methods.
    //  (if Singular ever gets any actual side effects, this needs to be changed)
    record Joiner(@NotNull InventoryView parent, @NotNull InventoryView child) implements InventoryView.Singular {
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
