package net.minestom.server.inventory.click.type;

import net.minestom.server.MinecraftServer;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.junit.jupiter.api.Test;

import static net.minestom.server.inventory.click.ClickUtils.assertClick;

public class InventoryOffhandSwapTest {

    static {
        MinecraftServer.init();
    }

    @Test
    public void testNoChanges() {
        assertClick(builder -> builder, new Click.Info.OffhandSwap(0), builder -> builder);
    }

    @Test
    public void testSwappedItems() {
        assertClick(
                builder -> builder.change(0, ItemStack.of(Material.DIRT)).change(PlayerInventoryUtils.OFF_HAND_SLOT, ItemStack.of(Material.STONE), true),
                new Click.Info.OffhandSwap(0),
                builder -> builder.change(0, ItemStack.of(Material.STONE)).change(PlayerInventoryUtils.OFF_HAND_SLOT, ItemStack.of(Material.DIRT), true)
        );
    }

}
