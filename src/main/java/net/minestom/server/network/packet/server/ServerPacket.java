package net.minestom.server.network.packet.server;

import net.minestom.server.adventure.ComponentHolder;
import net.minestom.server.entity.Player;

/**
 * Represents a packet which can be sent to a player using {@link Player#sendPacket(SendablePacket)}.
 * <p>
 * Packets are value-based, and should therefore not be reliant on identity.
 */
public sealed interface ServerPacket extends SendablePacket permits ServerPacket.Configuration, ServerPacket.Login, ServerPacket.Play, ServerPacket.Status {

    non-sealed interface Configuration extends ServerPacket {
    }

    non-sealed interface Status extends ServerPacket {
    }

    non-sealed interface Login extends ServerPacket {
    }

    non-sealed interface Play extends ServerPacket {
    }

    interface ComponentHolding extends ComponentHolder<ServerPacket> {
    }
}
