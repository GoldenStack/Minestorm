package net.minestom.server.inventory.click;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Preprocesses click packets for an inventory, turning them into {@link Click.Info} instances for further processing.
 */
public final class ClickPreprocessor {

    private final @NotNull Inventory inventory;

    private final Map<Player, IntList> leftDraggingMap = new ConcurrentHashMap<>();
    private final Map<Player, IntList> rightDraggingMap = new ConcurrentHashMap<>();
    private final Map<Player, IntList> creativeDragMap = new ConcurrentHashMap<>();

    public ClickPreprocessor(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    public void clearCache(@NotNull Player player) {
        leftDraggingMap.remove(player);
        rightDraggingMap.remove(player);
        creativeDragMap.remove(player);
    }

    private boolean validate(int slot) {
        return slot >= 0 && slot < inventory.getSize() + (inventory instanceof PlayerInventory ? 0 : PlayerInventoryUtils.INNER_SIZE);
    }

    /**
     * Processes the provided click packet, turning it into a {@link Click.Info}.
     * @param player the player clicking
     * @param packet the raw click packet
     * @return the information about the click, or nothing if there was no immediately usable information
     */
    public @Nullable Click.Info process(@NotNull Player player, @NotNull ClientClickWindowPacket packet) {
        final int originalSlot = packet.slot();
        final byte button = packet.button();
        final ClientClickWindowPacket.ClickType clickType = packet.clickType();

        final int slot = inventory instanceof PlayerInventory ? PlayerInventoryUtils.protocolToMinestom(originalSlot) : originalSlot;

        return switch (clickType) {
            case PICKUP -> {
                if (packet.slot() == -999) {
                    yield switch (button) {
                        case 0 -> new Click.Info.LeftDropCursor();
                        case 1 -> new Click.Info.RightDropCursor();
                        case 2 -> new Click.Info.MiddleDropCursor();
                        default -> null;
                    };
                }

                if (!validate(slot)) yield null;

                yield switch(button) {
                    case 0 -> new Click.Info.Left(slot);
                    case 1 -> new Click.Info.Right(slot);
                    default -> null;
                };
            }
            case QUICK_MOVE -> {
                if (!validate(slot)) yield null;
                yield button == 0 ? new Click.Info.LeftShift(slot) : new Click.Info.RightShift(slot);
            }
            case SWAP -> {
                if (!validate(slot)) {
                    yield null;
                } else if (button >= 0 && button < 9) {
                    yield new Click.Info.HotbarSwap(button, slot);
                } else if (button == 40) {
                    yield new Click.Info.OffhandSwap(slot);
                } else {
                    yield null;
                }
            }
            case CLONE -> (player.isCreative() && validate(slot)) ? new Click.Info.Middle(slot) : null;
            case THROW -> validate(slot) ? new Click.Info.DropSlot(slot, button == 1) : null;
            case QUICK_CRAFT -> {
                // Prevent invalid creative actions
                if (!player.isCreative() && (button == 8 || button == 9 || button == 10)) yield null;

                // Handle drag finishes
                if (button == 2) {
                    var list = leftDraggingMap.remove(player);
                    yield new Click.Info.LeftDrag(list != null ? list : IntLists.emptyList());
                } else if (button == 6) {
                    var list = rightDraggingMap.remove(player);
                    yield new Click.Info.RightDrag(list != null ? list : IntLists.emptyList());
                } else if (button == 10) {
                    var list = creativeDragMap.remove(player);
                    yield new Click.Info.MiddleDrag(list != null ? list : IntLists.emptyList());
                }

                // Handle intermediate state
                BiFunction<Player, IntList, IntList> addItem = (k, v) -> {
                    var v2 = v != null ? v : new IntArrayList();
                    if (validate(slot)) {
                        if (!v2.contains(slot)) {
                            v2.add(slot);
                        }
                    }
                    return v2;
                };

                switch (button) {
                    case 0 -> leftDraggingMap.remove(player);
                    case 4 -> rightDraggingMap.remove(player);
                    case 8 -> creativeDragMap.remove(player);

                    case 1 -> leftDraggingMap.compute(player, addItem);
                    case 5 -> rightDraggingMap.compute(player, addItem);
                    case 9 -> creativeDragMap.compute(player, addItem);
                }

                yield null;
            }
            case PICKUP_ALL -> validate(slot) ? new Click.Info.Double(slot) : null;
        };
    }

}
