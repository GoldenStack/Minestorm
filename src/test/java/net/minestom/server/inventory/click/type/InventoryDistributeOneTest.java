package net.minestom.server.inventory.click.type;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minestom.server.MinecraftServer;
import net.minestom.server.inventory.click.ClickInfo;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;

import static net.minestom.server.inventory.click.ClickUtils.assertClick;

public class InventoryDistributeOneTest {

    static {
        MinecraftServer.init();
    }

    @Test
    public void testNoCursor() {
        assertClick(builder -> builder, new ClickInfo.DragClick(IntList.of(0), false), builder -> builder);
    }

    @Test
    public void testDistributeNone() {
        assertClick(
                builder -> builder.cursor(ItemStack.of(Material.DIRT, 32)),
                new ClickInfo.DragClick(IntList.of(), false),
                builder -> builder
        );
    }

    @Test
    public void testDistributeOne() {
        assertClick(
                builder -> builder.cursor(ItemStack.of(Material.DIRT, 32)),
                new ClickInfo.DragClick(IntList.of(0), false),
                builder -> builder.change(0, ItemStack.of(Material.DIRT)).cursor(ItemStack.of(Material.DIRT, 31))
        );
    }

    @Test
    public void testDistributeExactlyEnough() {
        assertClick(
                builder -> builder.cursor(ItemStack.of(Material.DIRT, 2)),
                new ClickInfo.DragClick(IntList.of(0, 1), false),
                builder -> builder.change(0, ItemStack.of(Material.DIRT)).change(1, ItemStack.of(Material.DIRT)).cursor(ItemStack.of(Material.AIR))
        );
    }

    @Test
    public void testTooManySlots() {
        assertClick(
                builder -> builder.cursor(ItemStack.of(Material.DIRT, 2)),
                new ClickInfo.DragClick(IntList.of(0, 1, 2), false),
                builder -> builder.change(0, ItemStack.of(Material.DIRT)).change(1, ItemStack.of(Material.DIRT)).cursor(ItemStack.of(Material.AIR))
        );
    }

    @Test
    public void testDistributeOverExisting() {
        assertClick(
                builder -> builder.change(0, ItemStack.of(Material.DIRT, 16)).cursor(ItemStack.of(Material.DIRT, 32)),
                new ClickInfo.DragClick(IntList.of(0), false),
                builder -> builder.change(0, ItemStack.of(Material.DIRT, 17)).cursor(ItemStack.of(Material.DIRT, 31))
        );
    }

    @Test
    public void testDistributeOverFull() {
        assertClick(
                builder -> builder.change(0, ItemStack.of(Material.DIRT, 64)).cursor(ItemStack.of(Material.DIRT, 32)),
                new ClickInfo.DragClick(IntList.of(0), false),
                builder -> builder
        );
    }

}
