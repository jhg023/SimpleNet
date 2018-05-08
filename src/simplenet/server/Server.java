package simplenet.server;

import simplenet.Channeled;
import simplenet.Receiver;
import simplenet.client.Client;
import simplenet.server.listener.ServerListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @since November 1, 2017
 */
public final class Server extends Receiver implements Channeled {

	/**
	 * The backing {@link Channel} of the {@link Server}.
	 */
	private AsynchronousServerSocketChannel channel;

	/**
	 * The {@link Consumer} that is run when a {@link Client}
	 * successfully connects to this {@link Server}.
	 *
	 * TODO: Use {@link Collection<Consumer<AsynchronousSocketChannel>>} to
	 *       allow the user to add more {@link Consumer}s after this
	 *       {@link Server} has already been instantiated.
	 */
	private final Consumer<AsynchronousSocketChannel> consumer;

	/**
	 * Instantiates a new {@link Server} by attempting
	 * to open the backing {@link AsynchronousServerSocketChannel}.
	 *
	 * @throws IllegalStateException
	 *      If multiple {@link Server} instances are created.
	 */
	public Server(Consumer<AsynchronousSocketChannel> consumer) {
		if (channel != null) {
			throw new IllegalStateException("Multiple server instances are not allowed!");
		}

		try {
			channel = AsynchronousServerSocketChannel.open();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to open the channel!");
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
			channel.bind(new InetSocketAddress(address, port));
			channel.accept(this, new ServerListener());

			System.out.println(String.format("Successfully bound to %s:%d!", address, port));
		} catch (AlreadyBoundException e) {
			throw new IllegalStateException("A server is already running!");
		} catch (IOException e) {
			throw new IllegalStateException("Unable to bind the server!");
		}
	}

	/**
	 * Gets the {@link Consumer<AsynchronousSocketChannel>} that
	 * should be executed upon a successful {@link Client} connection
	 * to this {@link Server}.
	 *
	 * @return
	 *      A {@link Consumer<AsynchronousSocketChannel>}.
	 */
	public Consumer<AsynchronousSocketChannel> getConsumer() {
		return consumer;
	}

	/**
	 * Gets the backing {@link AsynchronousServerSocketChannel}
	 * of this {@link Server}.
	 *
	 * @return
	 *      This {@link Server}'s backing
	 *      {@link AsynchronousServerSocketChannel}.
	 */
	@Override
	public AsynchronousServerSocketChannel getChannel() {
		return channel;
	}

}
