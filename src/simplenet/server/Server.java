package simplenet.server;

import simplenet.client.*;
import simplenet.packet.incoming.*;
import simplenet.server.listener.*;
import simplenet.utility.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @since November 1, 2017
 */
public final class Server {

	/**
	 * The backing {@link Channel} of the {@link Server}.
	 */
	private AsynchronousServerSocketChannel server;

	/**
	 * An array of {@link IncomingPacket}s received by
	 * the {@link Server}.
	 */
	private final IncomingPacket[] packets = new IncomingPacket[256];

	/**
	 * Instantiates a new {@link Server} by attempting
	 * to open the backing {@link AsynchronousServerSocketChannel}.
	 *
	 * @throws IllegalStateException
	 *      If multiple {@link Server} instances are created.
	 */
	public Server() {
		if (server != null) {
			throw new IllegalStateException("Multiple server instances are not allowed!");
		}

		try {
			server = AsynchronousServerSocketChannel.open();
		} catch (IOException e) {
			throw new RuntimeException("Unable to open the channel!");
		}
	}

	/**
	 * Attempts to bind the {@link Server} to a
	 * specific {@code address} and {@code port}.
	 *
	 * @param address
	 *      The IP address to bind to.
	 * @param port
	 *      The port to bind to {@code 0 <= port <= 65535}.
	 * @throws IllegalArgumentException
	 *      If {@code port} is less than 0 or greater than 65535.
	 * @throws AlreadyBoundException
	 *      If a server is already running on any address/port.
	 * @throws RuntimeException
	 *      If the server is unable to be bound to a specific
	 *      address or port.
	 */
	public void bind(String address, int port) {
		Objects.requireNonNull(address);

		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("The port must be between 0 and 65535!");
		}

		try {
			server.bind(new InetSocketAddress(address, port));

			server.accept(new Tuple<>(this, server), new ServerListener());
		} catch (AlreadyBoundException e) {
			throw new IllegalStateException("A server is already running!");
		} catch (IOException e) {
			throw new RuntimeException("Unable to bind the server!");
		}
	}

	/**
	 * Registers an {@link IncomingPacket} to the {@link Server}.
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
	 *      The instance of this {@link Server} to
	 *      allow for method chaining.
	 * @throws IllegalArgumentException
	 *      If {@code opcode} is less than 0 or greater
	 *      than or equal to {@code packets.length}.
	 * @throws IllegalStateException
	 *      If {@code opcode} has already been registered.
	 */
	public Server register(int opcode, IncomingPacket packet) {
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
