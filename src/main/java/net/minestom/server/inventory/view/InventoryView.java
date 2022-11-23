package net.minestom.server.inventory.view;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.IntUnaryOperator;

public interface InventoryView {

    int slots();

    int mapSlot(int slot);

    @NotNull AbstractInventory source();

    default void clear() {
        for (int i = 0; i < slots(); i++) {
            setItemStack(i, ItemStack.AIR);
        }
    }

    default @NotNull ItemStack getItemStack(int slot) {
        return source().getItemStack(mapSlot(slot));
    }

    default void setItemStack(int slot, @NotNull ItemStack itemStack) {
        source().setItemStack(mapSlot(slot), itemStack);
    }

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

        @NotNull IntUnaryOperator localMapper();

        @Override
        default int mapSlot(int slot) {
            return parent().mapSlot(localMapper().applyAsInt(slot));
        }

        @Override
        default @NotNull AbstractInventory source() {
            return parent().source();
        }
    }

    static @NotNull InventoryView selectSlots(@NotNull InventoryView view, int @NotNull ... slots) {
        var availableSlots = new IntImmutableList(slots.clone());
        record SelectView(@NotNull InventoryView parent, @NotNull IntUnaryOperator localMapper, int slots) implements Tree {}
        return new SelectView(view, availableSlots::getInt, availableSlots.size());
    }

    static @NotNull InventoryView union(@NotNull InventoryView @NotNull ... views) {
        if (views.length == 0) {
            throw new IllegalArgumentException("Unions must have at least one view!");
        }
        var list = List.of(views);
        var size = list.stream().mapToInt(InventoryView::slots).sum();

        var source = list.get(0).source();
        for (var view : views) {
            if (view.source() != source) {
                throw new IllegalArgumentException("All views must have the same source!");
            }
        }

        record Union(@NotNull List<InventoryView> views, @NotNull AbstractInventory source, int slots) implements InventoryView {
            @Override
            public int mapSlot(int slot) {
                for (var view : views) {
                    if (slot < view.slots()) {
                        return view.mapSlot(slot);
                    }
                    slot -= view.slots();
                }
                return views.get(views.size() - 1).mapSlot(slot);
            }
        }

        return new Union(list, source, size);
    }

}
