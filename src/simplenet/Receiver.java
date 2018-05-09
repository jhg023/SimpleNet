package simplenet;

import simplenet.client.Client;
import simplenet.server.Server;
import simplenet.utility.IntPair;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.function.Consumer;

public class Receiver {

    /**
     * The {@link ByteBuffer} that will hold data
     * sent by the {@link Client} or {@link Server}.
     */
    private ByteBuffer buffer;

    /**
     * The {@link Deque} used when requesting a certain
     * amount of bytes from the {@link Client} or {@link Server}.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> queue;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    private final Collection<Consumer<AsynchronousSocketChannel>> connectListeners;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    private final Collection<Consumer<AsynchronousSocketChannel>> disconnectListeners;

    protected Receiver() {
        buffer = ByteBuffer.allocateDirect(128);
        queue = new ArrayDeque<>();
        connectListeners = new ArrayList<>();
        disconnectListeners = new ArrayList<>();
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
        read(n, consumer, false);
    }

    private void read(int n, Consumer<ByteBuffer> consumer, boolean always) {
        if (buffer.capacity() < n) {
            buffer = ByteBuffer.allocateDirect(n).put(buffer).flip();
        }

        /*if (always) {
            queue.addFirst(new IntPair<>(n, consumer));
        } else {
            queue.offer(new IntPair<>(n, consumer));
        }*/

        queue.offer(new IntPair<>(n, consumer));
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
                read(n, this, true);
            }
        }, true);
    }

    /**
     * Registers a listener that fires when a {@link Client} connects
     * to a {@link Server}.
     * <p>
     * This listener is able to be used by both the {@link Client}
     * and {@link Server}, but can be independent of one-another.
     * <p>
     * When calling this method more than once, multiple listeners
     * are registered.
     *
     * @param consumer
     *      A {@link Consumer<AsynchronousSocketChannel>}.
     */
    public void onConnect(Consumer<AsynchronousSocketChannel> consumer) {
        connectListeners.add(consumer);
    }

    /**
     * Registers a listener that fires when a {@link Client}
     * disconnects from a {@link Server}.
     * <p>
     * This listener is able to be used by both the {@link Client}
     * and {@link Server}, but can be independent of one-another.
     * <p>
     * When calling this method more than once, multiple listeners
     * are registered.
     *
     * @param consumer
     *      A {@link Consumer<AsynchronousSocketChannel>}.
     */
    public void onDisconnect(Consumer<AsynchronousSocketChannel> consumer) {
        disconnectListeners.add(consumer);
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} connects to a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<Consumer<AsynchronousSocketChannel>> getConnectionListeners() {
        return connectListeners;
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} disconnects from a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<Consumer<AsynchronousSocketChannel>> getDisconnectListeners() {
        return disconnectListeners;
    }

    /**
     * Gets the {@link Queue} that holds information
     * regarding requested bytes by this {@link Client}.
     *
     * @return
     *      This {@link Client}'s queue.
     */
    public Deque<IntPair<Consumer<ByteBuffer>>> getQueue() {
        return queue;
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

}
