package simplenet.packet.incoming;

import simplenet.client.Client;
import simplenet.packet.*;
import simplenet.server.Server;

import java.nio.*;
import java.nio.channels.AsynchronousSocketChannel;

@FunctionalInterface
public interface IncomingPacket extends Packet {

	/**
	 * A method that allows each {@link Packet}
	 * implementation to determine how its data
	 * should be read from its payload.
	 * <p>
	 * Any execution logic should be done here.
	 *
	 * @param channel
	 *      The {@link AsynchronousSocketChannel} that
     *      will handle this {@link IncomingPacket}.
     *      <p>
     *      If this {@link IncomingPacket} was
     *      received by a {@link Client}, then
     *      this parameter represents that {@link Client}'s
     *      backing {@link AsynchronousSocketChannel}.
     *      <p>
     *      Otherwise, if this {@link IncomingPacket} was
     *      received by the {@link Server}, then this parameter
     *      represents the backing {@link AsynchronousSocketChannel}
     *      of the {@link Client} that sent it.
	 * @param payload
	 *      A {@link ByteBuffer} that holds the
	 *      data belonging to this {@link Packet}.
	 */
	void read(AsynchronousSocketChannel channel, ByteBuffer payload);

}
