package simplenet.client;

import simplenet.Channeled;
import simplenet.Listener;
import simplenet.Receiver;
import simplenet.client.listener.ClientListener;
import simplenet.packet.Packet;
import simplenet.server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @since November 1, 2017
 */
public final class Client extends Receiver implements Channeled {

    /**
     * A single instance of {@link Listener} to handle
     * connections to a {@link Server}.
     */
    private static final ClientListener CLIENT_LISTENER = new ClientListener();

	/**
	 * The backing {@link Channel} of a {@link Client}.
	 */
	private final AsynchronousSocketChannel channel;

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
	private final Queue<ByteBuffer> outgoingPackets;

	/**
	 * Instantiates a new {@link Client} by attempting
	 * to open the backing {@link AsynchronousSocketChannel}.
	 */
	public Client() {
        this(4096);
    }

    public Client(int bufferSize) {
	    super(bufferSize);

        try {
            channel = AsynchronousSocketChannel.open();
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel!");
        }

        outgoingPackets = new ArrayDeque<>();
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

		if (port < 0 || port > 65_535) {
			throw new IllegalArgumentException("The port must be between 0 and 65535!");
		}

		try {
			channel.connect(new InetSocketAddress(address, port), this, CLIENT_LISTENER);
		} catch (AlreadyConnectedException e) {
			throw new IllegalStateException("This client is already connected!");
		}
	}

    /**
     * Flushes any queued {@link Packet}s held within
     * the internal {@link Queue}.
     * <p>
     * Any {@link Packet}s queued after the call to
     * {@code Client#flush()} will not be flushed until
     * it is called again.
     */
	public void flush() {
	    flush(outgoingPackets.size());
    }

    private void flush(int i) {
	    if (i == 0) {
	        return;
        }

        channel.write(outgoingPackets.poll(), null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                flush(i - 1);
            }

            @Override
            public void failed(Throwable t, Object attachment) {
                t.printStackTrace();
            }
        });
    }

    /**
     * Gets the {@link Queue} that manages outgoing
     * {@link Packet}s before writing them to the
     * {@link Channel}.
     *
     * @return
     *      A {@link Queue}.
     */
	public Queue<ByteBuffer> getOutgoingPackets() {
	    return outgoingPackets;
    }

	/**
	 * Gets the backing {@link Channel} of this {@link Client}.
	 *
	 * @return
	 *      This {@link Client}'s backing channel.
	 */
	@Override
	public AsynchronousSocketChannel getChannel() {
		return channel;
	}

}
