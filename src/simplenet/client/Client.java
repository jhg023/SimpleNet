package simplenet.client;

import simplenet.packet.*;
import simplenet.server.*;
import simplenet.utility.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @since November 1, 2017
 */
public final class Client extends Packetable {

	/**
	 * The backing {@link Channel} of a {@link Client}.
	 */
	private final AsynchronousSocketChannel channel;

	/**
	 * Instantiates a new {@link Client} by attempting
	 * to open the backing {@link AsynchronousSocketChannel}.
	 */
	public Client() {
		try {
			channel = AsynchronousSocketChannel.open();
			channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		} catch (IOException e) {
			throw new RuntimeException("Unable to open the channel!");
		}
	}

	/**
	 * Attempts to connect to a {@link Server} with a
	 * specific {@code address} and {@code port}.
	 *
	 * @param address
	 *      The IP address to connect to.
	 * @param port
	 *      The port to connect to {@code 0 <= port <= 65535}.
	 * @throws IllegalArgumentException
	 *      If {@code port} is less than 0 or greater than 65535.
	 * @throws AlreadyConnectedException
	 *      If a {@link Client} is already connected to any address/port.
	 */
	public void connect(String address, int port) {
		Objects.requireNonNull(address);

		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("The port must be between 0 and 65535!");
		}

		try {
			channel.connect(new InetSocketAddress(address, port), new Tuple<>(this, channel), Constants.CLIENT_LISTENER);
		} catch (AlreadyConnectedException e) {
			throw new IllegalStateException("This client is already connected!");
		}
	}

	/**
	 * Gets the backing {@link Channel} of this {@link Client}.
	 *
	 * @return
	 *      This {@link Client}'s backing {@link Channel}.
	 */
	public AsynchronousSocketChannel getChannel() {
		return channel;
	}

}
