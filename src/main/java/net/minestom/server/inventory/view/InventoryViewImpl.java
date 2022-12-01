package net.minestom.server.inventory.view;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class InventoryViewImpl {

    // Implements InventoryView.Singular so that it can be treated as it when needed
    //  (i.e. when we know that it must be singular, and so code duplication can be avoided)
    // This is safe because implementing Singular doesn't actually have any side effects; it just adds utility methods.
    //  (if Singular ever gets any actual side effects, this needs to be changed)
    record ContiguousFork(int min, int max) implements InventoryView.Singular {

        ContiguousFork {
            if (min < 0 || max < 0) {
                throw new IllegalArgumentException("Slot IDs cannot be negatively signed!");
            } else if (min > max) {
                throw new IllegalArgumentException("The minimum value cannot be greater than the maximum!");
            }
        }

        @Override
        public int size() {
            return max - min + 1; // add 1 because `max` is inclusive
        }

        @Override
        public int localToExternal(int slot) {
            if (!isValidLocal(slot)) {
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

        Joiner {
            if (child.size() > parent.size()) {
                throw new IllegalArgumentException("Children cannot be larger than their parents!");
            }
        }

        @Override
        public int size() {
            return child.size();
        }

        @Override
        public int localToExternal(int slot) {
            if (!child.isValidLocal(slot)) {
                return -1;
            }
            int parentLocal = child.localToExternal(slot);
            if (!parent.isValidLocal(parentLocal)) {
                return -1;
            }
            return parent.localToExternal(parentLocal);
        }

        @Override
        public boolean isValidLocal(int slot) {
            return child.isValidLocal(slot) && parent.isValidLocal(child.localToExternal(slot));
        }
    }

    record Union(@NotNull List<InventoryView> views, int size) implements InventoryView.Singular {

        Union(@NotNull List<InventoryView> views) {
            this(views, views.stream().mapToInt(InventoryView::size).sum());
        }

        Union {
            views = List.copyOf(views);
        }

        @Override
        public int localToExternal(int slot) {
            if (!isValidLocal(slot)) {
                return -1;
            }
            for (var view : views) {
                if (slot < view.size()) {
                    return view.localToExternal(slot);
                }
                slot -= view.size();
            }
            // this code should never be run because, at this point, `slot >= size()`
            // must be true, but we already verified that it's false at the start.
            return -1;
        }
    }

    record Arbitrary(@NotNull IntList slots) implements InventoryView.Singular {

        Arbitrary {
            slots = new IntImmutableList(slots);
            for (var slot : slots) {
                if (slot < 0) {
                    throw new IllegalArgumentException("Slot IDs cannot be negatively signed!");
                }
            }
        }

        @Override
        public int size() {
            return slots.size();
        }

        @Override
        public int localToExternal(int slot) {
            if (!isValidLocal(slot)) {
                return -1;
            }
            return slots.getInt(slot);
        }
    }

}
