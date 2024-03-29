package net.minestom.server.inventory.type;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.ContainerInventory;
import net.minestom.server.inventory.InventoryProperty;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class FurnaceInventory extends ContainerInventory {

    /**
     * Client prediction appears to disallow shift clicking into furnace inventories.<br>
     * Instead:
     * - Shift clicks in the inventory go to the player inventory like normal
     * - Shift clicks in the hotbar go to the storage
     * - Shift clicks in the storage go to the hotbar
     */
    public static final @NotNull Click.Processor FURNACE_HANDLER = ClickProcessors.standard(
            (builder, item, slot) -> {
                int size = builder.clickedSize();
                if (slot < size) {
                    return PlayerInventory.getInnerShiftClickSlots(builder);
                } else if (slot < size + 27) {
                    return IntStream.range(27, 36).map(i -> i + size);
                } else {
                    return IntStream.range(0, 27).map(i -> i + size);
                }
            },
            (builder, item, slot) -> IntStream.concat(
                    IntStream.range(0, builder.clickedSize()),
                    PlayerInventory.getInnerDoubleClickSlots(builder)
            ));

    private short remainingFuelTick;
    private short maximumFuelBurnTime;
    private short progressArrow;
    private short maximumProgress;

    public FurnaceInventory(@NotNull Component title) {
        super(InventoryType.FURNACE, title);
    }

    public FurnaceInventory(@NotNull String title) {
        super(InventoryType.FURNACE, title);
    }

    @Override
    public @Nullable Click.Result handleClick(@NotNull Player player, @NotNull Click.Info info) {
        return FURNACE_HANDLER.handleClick(this, player, info);
    }

    /**
     * Represents the amount of tick until the fire icon come empty.
     *
     * @return the amount of tick until the fire icon come empty
     */
    public short getRemainingFuelTick() {
        return remainingFuelTick;
    }

    /**
     * Represents the amount of tick until the fire icon come empty.
     *
     * @param remainingFuelTick the amount of tick until the fire icon is empty
     */
    public void setRemainingFuelTick(short remainingFuelTick) {
        this.remainingFuelTick = remainingFuelTick;
        sendProperty(InventoryProperty.FURNACE_FIRE_ICON, remainingFuelTick);
    }

    public short getMaximumFuelBurnTime() {
        return maximumFuelBurnTime;
    }

    public void setMaximumFuelBurnTime(short maximumFuelBurnTime) {
        this.maximumFuelBurnTime = maximumFuelBurnTime;
        sendProperty(InventoryProperty.FURNACE_MAXIMUM_FUEL_BURN_TIME, maximumFuelBurnTime);
    }

    public short getProgressArrow() {
        return progressArrow;
    }

    public void setProgressArrow(short progressArrow) {
        this.progressArrow = progressArrow;
        sendProperty(InventoryProperty.FURNACE_PROGRESS_ARROW, progressArrow);
    }

    public short getMaximumProgress() {
        return maximumProgress;
    }

    public void setMaximumProgress(short maximumProgress) {
        this.maximumProgress = maximumProgress;
        sendProperty(InventoryProperty.FURNACE_MAXIMUM_PROGRESS, maximumProgress);
    }
}
