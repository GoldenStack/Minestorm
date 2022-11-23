package net.minestom.server.inventory.view.idea2;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class Usage {

    public static void test(@NotNull Player player) {
        var inv = player.getInventory();
        var view = new PlayerInventoryView(inv);

        fill(view, Material.STONE);
        fill(view.armor(), Material.RED_WOOL);
        fill(view.crafting().input(), Material.ORANGE_WOOL);
        fill(view.crafting().result(), Material.YELLOW_WOOL);
        fill(view.offhand(), Material.GREEN_WOOL);
        fill(view.main().storage(), Material.BLUE_WOOL);
        fill(view.main().hotbar(), Material.PURPLE_WOOL);

    }

    static void fill(@NotNull InventoryView view, @NotNull Material material) {
        for (int i = 0; i < view.slots(); i++) {
            view.source().setItemStack(view.mapSlot(i), ItemStack.of(material, 1 + i));
        }
    }
}