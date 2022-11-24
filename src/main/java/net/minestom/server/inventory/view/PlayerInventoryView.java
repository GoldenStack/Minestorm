package net.minestom.server.inventory.view;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public record PlayerInventoryView(@NotNull AbstractInventory source) implements InventoryView.Root {

    public @NotNull Armor armor() {
        return new Armor(this, 4, slot -> slot + 41);
    }
    public @NotNull Crafting crafting() {
        return new Crafting(this, 5, slot -> slot + 36);
    }
    public @NotNull Offhand offhand() {
        return new Offhand(this, 1, slot -> slot + 45);
    }
    public @NotNull Main main() {
        return new Main(this, 36, slot -> slot + 0);
    }

    public record Armor(@NotNull PlayerInventoryView parent, int slots, @NotNull SlotMapper localMapper) implements Node {}

    public record Crafting(@NotNull PlayerInventoryView parent, int slots, @NotNull SlotMapper localMapper) implements Node {
        public @NotNull Input input() {
            return new Input(this, 4, slot -> slot + 1);
        }
        public @NotNull Result result() {
            return new Result(this, 1, slot -> slot + 0);
        }

        public record Input(@NotNull Crafting parent, int slots, @NotNull SlotMapper localMapper) implements Node {}
        public record Result(@NotNull Crafting parent, int slots, @NotNull SlotMapper localMapper) implements Node {}
    }

    public record Offhand(@NotNull PlayerInventoryView parent, int slots, @NotNull SlotMapper localMapper) implements Node {}

    public record Main(@NotNull PlayerInventoryView parent, int slots, @NotNull SlotMapper localMapper) implements Node {
        public @NotNull Storage storage() {
            return new Storage(this, 27, slot -> slot + 9);
        }
        public @NotNull Hotbar hotbar() {
            return new Hotbar(this, 9, slot -> slot + 0);
        }

        public record Storage(@NotNull Main parent, int slots, @NotNull SlotMapper localMapper) implements Node {}
        public record Hotbar(@NotNull Main parent, int slots, @NotNull SlotMapper localMapper) implements Node {}
    }
}