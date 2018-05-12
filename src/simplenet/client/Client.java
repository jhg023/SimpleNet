package simplenet.client;

import simplenet.channel.Channeled;
import simplenet.Receiver;
import simplenet.client.listener.ClientListener;
import simplenet.packet.Packet;
import simplenet.server.Server;
import simplenet.utility.IntPair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @since November 1, 2017
 */
public final class Client extends Receiver {

    /**
     * A single instance of {@link ClientListener} to handle
     * connections to a {@link Server}.
     */
    private static final ClientListener CLIENT_LISTENER = new ClientListener();

    /**
     * Whether or not new elements added {@code queue}
     * should be added to the front rather than the back.
     */
    private boolean prepend;

    /**
     * The {@link ByteBuffer} that will hold data
     * sent by the {@link Client} or {@link Server}.
     */
    private final ByteBuffer buffer;

    /**
     * The backing {@link Channel} of a {@link Client}.
     */
    private final AsynchronousSocketChannel channel;

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<ByteBuffer> outgoingPackets;

    /**
     * The {@link Deque} that keeps track of nested calls
     * to {@link #read(int, Consumer)} and assures that they
     * will complete in the expected order.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> stack;

    /**
     * The {@link Deque} used when requesting a certain
     * amount of bytes from the {@link Client} or {@link Server}.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> queue;

	/**
	 * Instantiates a new {@link Client} by attempting
	 * to open the backing {@link AsynchronousSocketChannel}
     * with a default buffer size of {@code 4096} bytes.
	 */
	public Client() {
        this(4096);
    }

    /**
     * Instantiates a new {@link Client} by attempting
     * to open the backing {@link AsynchronousSocketChannel}
     * with a provided buffer size in bytes.
     *
     * @param bufferSize
     *      The size of this {@link Client}'s buffer, in bytes.
     */
    public Client(int bufferSize) {
	    this(bufferSize, null);
    }

    /**
     * Instantiates a new {@link Client} with an existing
     * {@link AsynchronousSocketChannel} with a provided
     * buffer size in bytes.
     *
     * @param bufferSize
     *      The size of this {@link Client}'s buffer, in bytes.
     * @param channel
     *      The channel to back this {@link Client} with.
     */
    public Client(int bufferSize, AsynchronousSocketChannel channel) {
	    super(bufferSize);

        outgoingPackets = new ArrayDeque<>();
        buffer = ByteBuffer.allocateDirect(bufferSize);
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();

	    if (channel != null) {
            this.channel = channel;
            return;
        }

        try {
            this.channel = AsynchronousSocketChannel.open();
            this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel!");
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
			channel.connect(new InetSocketAddress(address, port), this, CLIENT_LISTENER);
		} catch (AlreadyConnectedException e) {
			throw new IllegalStateException("This client is already connected!");
		}
	}

    /**
     * Requests {@code n} bytes and accepts a {@link Consumer<ByteBuffer>}
     * holding the {@code n} bytes once received.
     *
     * @param n
     *      The number of bytes requested.
     * @param consumer
     *      Holds the operations that should be performed once
     *      the {@code n} bytes are received.
     */
    public void read(int n, Consumer<ByteBuffer> consumer) {
        if (prepend) {
            stack.addFirst(new IntPair<>(n, consumer));
        } else {
            queue.offer(new IntPair<>(n, consumer));
        }
    }

    /**
     * Calls {@link #read(int, Consumer)}, however once
     * finished, {@link #read(int, Consumer)} is called once
     * again with the same parameters; this loops indefinitely
     * whereas {@link #read(int, Consumer)} completes after
     * a single iteration.
     *
     * @param n
     *      The number of bytes requested.
     * @param consumer
     *      Holds the operations that should be performed once
     *      the {@code n} bytes are received.
     */
    public void readAlways(int n, Consumer<ByteBuffer> consumer) {
        read(n, new Consumer<>() {
            @Override
            public void accept(ByteBuffer buffer) {
                consumer.accept(buffer);
                read(n, this);
            }
        });
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

    /**
     * Gets the {@link Deque} that holds information
     * regarding requested bytes by this {@link Client}.
     *
     * @return
     *      A {@link Deque}.
     */
    public Deque<IntPair<Consumer<ByteBuffer>>> getQueue() {
        return queue;
    }

    /**
     * Gets the {@link Deque} that keeps track of nested
     * calls to {@link #read(int, Consumer)}.
     *
     * @return
     *      A {@link Deque}.
     */
    public Deque<IntPair<Consumer<ByteBuffer>>> getStack() {
        return stack;
    }

    /**
     * Gets the {@link ByteBuffer} of this {@link Client}.
     *
     * @return
     *      This {@link Client}'s buffer.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets whether or not new elements being added to
     * the {@code queue} should be added to the front
     * or the back.
     *
     * @param prepend
     *      A {@code boolean}.
     */
    public void setPrepend(boolean prepend) {
        this.prepend = prepend;
    }

}
