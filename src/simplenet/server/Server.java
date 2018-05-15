package simplenet.server;

import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collection;
import simplenet.Listener;
import simplenet.Receiver;
import simplenet.client.Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @since November 1, 2017
 */
public final class Server extends Receiver<Consumer<Client>> {

    private int maxClients = Integer.MAX_VALUE;

	/**
	 * The backing {@link Channel} of the {@link Server}.
	 */
	private AsynchronousServerSocketChannel channel;

    /**
     * The {@link Collection} of {@link Client}s that are
     * connected to this {@link Server}.
     */
	private final Collection<Client> clients;

	/**
	 * Instantiates a new {@link Server} by attempting
	 * to open the backing {@link AsynchronousServerSocketChannel}.
	 *
	 * @throws IllegalStateException
	 *      If multiple {@link Server} instances are created.
	 */
	public Server() {
	    this(4096);
    }

	public Server(int bufferSize) {
	    super(bufferSize);

        if (channel != null) {
            throw new IllegalStateException("Multiple server instances are not allowed!");
        }

        try {
            channel = AsynchronousServerSocketChannel.open();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel!");
        }

        clients = new ArrayList<>();
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

            final Listener listener = new Listener() {
                @Override
                public void failed(Throwable t, Client client) {
                    getDisconnectListeners().forEach(consumer -> consumer.accept(client));
                    client.close();
                }
            };

			channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Void attachment) {
                    if (clients.size() == maxClients) {
                        Server.this.channel.accept(null, this);
                        return;
                    }

                    var client = new Client(bufferSize, channel);

                    clients.add(client);

                    getConnectionListeners().forEach(consumer -> consumer.accept(client));

                    Server.this.channel.accept(null, this);

                    channel.read(client.getBuffer(), client, listener);
                }

                @Override
                public void failed(Throwable t, Void attachment) {

                }
            });

			System.out.println(String.format("Successfully bound to %s:%d!", address, port));
		} catch (AlreadyBoundException e) {
			throw new IllegalStateException("A server is already running!");
		} catch (IOException e) {
			throw new IllegalStateException("Unable to bind the server!");
		}
	}

    /**
     * Sets the maximum number of {@link Client}s that can be connected
     * to this {@link Server} at any given time.
     *
     * @param maxClients
     *      The maximum number of clients.
     */
	public void setMaxClients(int maxClients) {
	    this.maxClients = maxClients;
    }

	/**
	 * Gets the backing {@link Channel} of this {@link Server}.
	 *
	 * @return
	 *      A {@link Channel}.
	 */
	@Override
	public AsynchronousServerSocketChannel getChannel() {
		return channel;
	}

}
