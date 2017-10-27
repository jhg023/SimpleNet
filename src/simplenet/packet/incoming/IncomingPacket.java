package simplenet.packet.incoming;

import simplenet.packet.*;

import java.nio.*;

@FunctionalInterface
public interface IncomingPacket extends Packet {

	/**
	 * A method that allows each {@link Packet}
	 * implementation to determine how its data
	 * should be read from its payload.
	 * <p>
	 * Any execution logic should be done here.
	 *
	 * @param payload
	 *      A {@link ByteBuffer} that holds the
	 *      data belonging to this {@link Packet}.
	 */
	void read(ByteBuffer payload);

}
