package simplenet.server;

import simplenet.client.*;
import simplenet.packet.*;
import simplenet.server.listener.*;
import simplenet.utility.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @since November 1, 2017
 */
public final class Server extends Packetable {

	/**
	 * The backing {@link Channel} of the {@link Server}.
	 */
	private AsynchronousServerSocketChannel server;

	private final Consumer<AsynchronousSocketChannel> consumer;

	/**
	 * Instantiates a new {@link Server} by attempting
	 * to open the backing {@link AsynchronousServerSocketChannel}.
	 *
	 * @throws IllegalStateException
	 *      If multiple {@link Server} instances are created.
	 */
	public Server(Consumer<AsynchronousSocketChannel> consumer) {
		if (server != null) {
			throw new IllegalStateException("Multiple server instances are not allowed!");
		}

		try {
			AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

			server = AsynchronousServerSocketChannel.open(group);
		} catch (IOException e) {
			throw new RuntimeException("Unable to open the channel!");
		}

		this.consumer = consumer;
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

	public Consumer<AsynchronousSocketChannel> getConsumer() {
		return consumer;
	}

}
