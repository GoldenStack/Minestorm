package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Click {

    /**
     * Contains information about a click. These are equal to the packet slot IDs from <a href="https://wiki.vg/Inventory">the Minecraft protocol.</a>.
     * The inventory used should be known from context.
     */
    public sealed interface Info {

        record Left(int slot) implements Info {}
        record Right(int slot) implements Info {}
        record Middle(int slot) implements Info {} // Creative only

        record LeftShift(int slot) implements Info {}
        record RightShift(int slot) implements Info {}

        record Double(int slot) implements Info {}

        record LeftDrag(@NotNull IntList slots) implements Info {}
        record RightDrag(@NotNull IntList slots) implements Info {}
        record MiddleDrag(@NotNull IntList slots) implements Info {} // Creative only

        record LeftDropCursor() implements Info {}
        record RightDropCursor() implements Info {}
        record MiddleDropCursor() implements Info {}

        record DropSlot(int slot, boolean all) implements Info {}

        record HotbarSwap(int hotbarSlot, int clickedSlot) implements Info {}
        record OffhandSwap(int slot) implements Info {}

        record CreativeSetItem(int slot, @NotNull ItemStack item) implements Info {}
        record CreativeDropItem(@NotNull ItemStack item) implements Info {}

    }
}
