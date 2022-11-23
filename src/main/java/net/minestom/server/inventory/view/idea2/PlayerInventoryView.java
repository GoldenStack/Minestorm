package net.minestom.server.inventory.view.idea2;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public record PlayerInventoryView(@NotNull AbstractInventory source) implements InventoryView {
    @Override
    public int slots() {
        return source.getSize();
    }

    @Override
    public int mapSlot(int slot) {
        return slot;
    }

    public @NotNull Armor armor() {
        return new Armor(this);
    }

    public @NotNull Crafting crafting() {
        return new Crafting(this);
    }

    public @NotNull Offhand offhand() {
        return new Offhand(this);
    }

    public @NotNull Main main() {
        return new Main(this);
    }

    public record Armor(@NotNull PlayerInventoryView parent) implements InventoryView.Tree {
        @Override
        public int slots() {
            return 4;
        }

        @Override
        public int mapSlot(int slot) {
            return parent.mapSlot(slot + 41);
        }
    }

    public record Crafting(@NotNull PlayerInventoryView parent) implements InventoryView.Tree {
        @Override
        public int slots() {
            return 5;
        }

        @Override
        public int mapSlot(int slot) {
            return parent.mapSlot(slot + 36);
        }

        public @NotNull Input input() {
            return new Input(this);
        }

        public @NotNull Result result() {
            return new Result(this);
        }

        public record Input(@NotNull Crafting parent) implements InventoryView.Tree {

            @Override
            public int slots() {
                return 4;
            }

            @Override
            public int mapSlot(int slot) {
                return parent.mapSlot(slot + 1);
            }
        }

        public record Result(@NotNull Crafting parent) implements InventoryView.Tree {
            @Override
            public int slots() {
                return 1;
            }

            @Override
            public int mapSlot(int slot) {
                return parent.mapSlot(slot + 0);
            }
        }
    }

    public record Offhand(@NotNull PlayerInventoryView parent) implements InventoryView.Tree {
        @Override
        public int slots() {
            return 1;
        }

        @Override
        public int mapSlot(int slot) {
            return parent.mapSlot(slot + 45);
        }
    }

    public record Main(@NotNull PlayerInventoryView parent) implements InventoryView.Tree {
        @Override
        public int slots() {
            return 36;
        }

        @Override
        public int mapSlot(int slot) {
            return parent.mapSlot(slot + 0);
        }

        public @NotNull Storage storage() {
            return new Storage(this);
        }

        public @NotNull Hotbar hotbar() {
            return new Hotbar(this);
        }

        public record Storage(@NotNull Main parent) implements InventoryView.Tree {
            @Override
            public int slots() {
                return 27;
            }

            @Override
            public int mapSlot(int slot) {
                return parent.mapSlot(slot + 9);
            }
        }

        public record Hotbar(@NotNull Main parent) implements InventoryView.Tree {
            @Override
            public int slots() {
                return 9;
            }

            @Override
            public int mapSlot(int slot) {
                return parent.mapSlot(slot + 0);
            }
        }
    }
}
