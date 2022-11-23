package net.minestom.server.inventory.view.idea1;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public record PlayerInventoryView(@NotNull AbstractInventory inventory) implements InventoryView {

    @Override
    public int slots() {
        return inventory.getSize();
    }

    @Override
    public int mapSlot(int slot) {
        return slot;
    }

    @Override
    public @NotNull AbstractInventory source() {
        return inventory;
    }

    public @NotNull Armor armor() {
        return new Armor();
    }

    public @NotNull Crafting crafting() {
        return new Crafting();
    }

    public @NotNull Offhand offhand() {
        return new Offhand();
    }

    public @NotNull Main main() {
        return new Main();
    }

    public class Armor implements InventoryView {

        @Override
        public int slots() {
            return 4;
        }

        @Override
        public int mapSlot(int slot) {
            return PlayerInventoryView.this.mapSlot(slot + 41);
        }

        @Override
        public @NotNull AbstractInventory source() {
            return PlayerInventoryView.this.source();
        }
    }

    public class Crafting implements InventoryView {

        @Override
        public int slots() {
            return 5;
        }

        @Override
        public int mapSlot(int slot) {
            System.out.println(slot);
            return PlayerInventoryView.this.mapSlot(slot + 36);
        }

        @Override
        public @NotNull AbstractInventory source() {
            return PlayerInventoryView.this.source();
        }

        public @NotNull Input input() {
            return new Input();
        }

        public @NotNull Result result() {
            return new Result();
        }

        public class Input implements InventoryView {

            @Override
            public int slots() {
                return 4;
            }

            @Override
            public int mapSlot(int slot) {
                return Crafting.this.mapSlot(slot + 1);
            }

            @Override
            public @NotNull AbstractInventory source() {
                return PlayerInventoryView.this.inventory;
            }
        }

        public class Result implements InventoryView {

            @Override
            public int slots() {
                return 1;
            }

            @Override
            public int mapSlot(int slot) {
                return Crafting.this.mapSlot(slot + 0);
            }

            @Override
            public @NotNull AbstractInventory source() {
                return PlayerInventoryView.this.inventory;
            }
        }

    }

    public class Offhand implements InventoryView {

        @Override
        public int slots() {
            return 1;
        }

        @Override
        public int mapSlot(int slot) {
            return PlayerInventoryView.this.mapSlot(slot + 45);
        }

        @Override
        public @NotNull AbstractInventory source() {
            return PlayerInventoryView.this.inventory;
        }
    }

    public class Main implements InventoryView {

        @Override
        public int slots() {
            return 36;
        }

        @Override
        public int mapSlot(int slot) {
            return PlayerInventoryView.this.mapSlot(slot + 0);
        }

        @Override
        public @NotNull AbstractInventory source() {
            return PlayerInventoryView.this.inventory;
        }

        public @NotNull Storage storage() {
            return new Storage();
        }

        public @NotNull Hotbar hotbar() {
            return new Hotbar();
        }

        public class Storage implements InventoryView {

            @Override
            public int slots() {
                return 27;
            }

            @Override
            public int mapSlot(int slot) {
                return Main.this.mapSlot(slot + 9);
            }

            @Override
            public @NotNull AbstractInventory source() {
                return PlayerInventoryView.this.inventory;
            }
        }

        public class Hotbar implements InventoryView {

            @Override
            public int slots() {
                return 9;
            }

            @Override
            public int mapSlot(int slot) {
                return Main.this.mapSlot(slot + 0);
            }

            @Override
            public @NotNull AbstractInventory source() {
                return PlayerInventoryView.this.inventory;
            }
        }

    }
}
