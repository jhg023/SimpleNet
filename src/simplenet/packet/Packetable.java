package simplenet.packet;

import simplenet.packet.incoming.*;
import simplenet.utility.*;

import java.nio.*;

/**
 * An {@code abstract} class representing an entity that
 * can receive {@link IncomingPacket}s and store them for
 * processing.
 * <p>
 * Each individual {@link IncomingPacket} must be registered
 * with this {@link Packetable} for it to be processed
 * immediately on arrival.
 */
public abstract class Packetable {

	/**
	 * An array of {@link IncomingPacket}s received by
	 * a {@link Packetable}.
	 */
	private final IncomingPacket[] packets;

	/**
	 * Instantiates a new {@link Packetable}.
	 */
	protected Packetable() {
		packets = new IncomingPacket[Constants.PACKET_LIMIT];
	}

	/**
	 * Registers an {@link IncomingPacket} to a {@link Packetable}.
	 * <p>
	 * If an {@link IncomingPacket} with {@code opcode} is received,
	 * then {@link IncomingPacket#read(ByteBuffer)} will be called
	 * for {@code packet}.
	 *
	 * @param opcode
	 *      The opcode to register {@code packet} to.
	 * @param packet
	 *      The packet to register.
	 * @return
	 *      The instance of this {@link Packetable} to
	 *      allow for method chaining.
	 * @throws IllegalArgumentException
	 *      If {@code opcode} is less than 0 or greater
	 *      than or equal to {@code packets.length}.
	 * @throws IllegalStateException
	 *      If {@code opcode} has already been registered.
	 */
	public Packetable register(int opcode, IncomingPacket packet) {
		if (opcode < 0 || opcode >= packets.length) {
			throw new IllegalArgumentException(String.format("opcode must be between 0 and %d (inclusive)", packets.length - 1));
		}

		if (packets[opcode] != null) {
			throw new IllegalStateException(String.format("opcode %d has already been registered!", opcode));
		}

		packets[opcode] = packet;
		return this;
	}

	/**
	 * Gets the array of {@link IncomingPacket}s where
	 * packets are registered to.
	 *
	 * @return
	 *      An array of {@link IncomingPacket}s.
	 */
	public IncomingPacket[] getPackets() {
		return packets;
	}

}
