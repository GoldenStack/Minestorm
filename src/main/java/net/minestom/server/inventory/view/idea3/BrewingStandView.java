package net.minestom.server.inventory.view.idea3;

import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public record BrewingStandView(@NotNull AbstractInventory source) implements InventoryView.Root {

    public @NotNull Results results() {
        return new Results(this, 3, 0);
    }
    public @NotNull Input input() {
        return new Input(this, 1, 3);
    }
    public @NotNull Fuel fuel() {
        return new Fuel(this, 1, 4);
    }

    public record Input(@NotNull BrewingStandView parent, int slots, int offset) implements OffsetTree {}
    public record Results(@NotNull BrewingStandView parent, int slots, int offset) implements OffsetTree {}
    public record Fuel(@NotNull BrewingStandView parent, int slots, int offset) implements OffsetTree {}

}
