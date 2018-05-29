package simplenet;

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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import javax.crypto.Cipher;
import simplenet.packet.Packet;
import simplenet.receiver.Receiver;
import simplenet.utility.IntPair;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @since November 1, 2017
 */
public class Client extends Receiver<Runnable> {

    /**
     * The {@link CompletionHandler} used to process bytes
     * when they are received by this {@link Client}.
     */
    static class Listener implements CompletionHandler<Integer, Client> {

        /**
         * A {@code static} instance of this class to be reused.
         */
        private static final Listener INSTANCE = new Listener();

        @Override
        public void completed(Integer result, Client client) {
            var buffer = client.getBuffer().flip();
            var queue = client.getQueue();
            var peek = queue.pollLast();

            if (peek == null) {
                client.getChannel().read(buffer.flip().limit(buffer.capacity()), client, this);
                return;
            }

            client.setPrepend(true);

            boolean decrypt = client.decryption != null;

            int key;
            int size = buffer.remaining();
            var stack = client.getStack();

            while (size >= (key = peek.getKey())) {
                if (decrypt) {
                    try {
                        int position = buffer.position();
                        client.decryption.update(buffer.limit(buffer.position() + key), buffer.duplicate());
                        buffer.flip().position(position);
                    } catch (Exception e) {
                        throw new IllegalStateException("Exception occurred when decrypting:", e);
                    }
                }

                peek.getValue().accept(buffer);

                size -= key;

                while (!stack.isEmpty()) {
                    queue.offer(stack.poll());
                }

                if ((peek = queue.pollLast()) == null) {
                    break;
                }
            }

            client.setPrepend(false);

            if (peek != null) {
                queue.addFirst(peek);
            }

            if (size > 0) {
                buffer.compact();
            } else {
                buffer.flip();
            }

            client.getChannel().read(buffer.limit(buffer.capacity()), client, this);
        }

        @Override
        public void failed(Throwable t, Client client) {
            client.getDisconnectListeners().forEach(Runnable::run);
            client.close();
        }

        /**
         * Gets the single instance of this class.
         *
         * @return
         *      A {@link Listener}.
         */
        static Listener getInstance() {
            return INSTANCE;
        }
    }

    /**
     * The {@link CompletionHandler} used when this {@link Client}
     * connects to a {@link Server}.
     */
    private static final CompletionHandler<Void, Client> CLIENT_LISTENER = new CompletionHandler<>() {
        @Override
        public void completed(Void result, Client client) {
            client.getConnectionListeners().forEach(Runnable::run);
            client.getChannel().read(client.getBuffer(), client, Listener.getInstance());
        }

        @Override
        public void failed(Throwable exc, Client client) {

        }
    };

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
     * The {@link Cipher} used for {@link Packet} encryption.
     */
    private Cipher encryption;

    /**
     * The {@link Cipher} used for {@link Packet} decryption.
     */
    private Cipher decryption;

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<ByteBuffer> outgoingPackets;

    /**
     * The {@link Deque} that keeps track of nested calls
     * to {@link Client#read(int, Consumer)} and assures that they
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
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     */
    public Client(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Instantiates a new {@link Client} with an existing
     * {@link AsynchronousSocketChannel} with a provided
     * buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     * @param channel    The channel to back this {@link Client} with.
     */
    public Client(int bufferSize, AsynchronousSocketChannel channel) {
        super(bufferSize);

        outgoingPackets = new ArrayDeque<>();
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();
        buffer = ByteBuffer.allocateDirect(bufferSize);

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

    public Client(Client client) {
        super(client);

        this.prepend = client.prepend;
        this.buffer = client.buffer;
        this.channel = client.channel;
        this.encryption = client.encryption;
        this.decryption = client.decryption;
        this.outgoingPackets = client.outgoingPackets;
        this.stack = client.stack;
        this.queue = client.queue;
    }

    /**
     * Attempts to connect to a {@link Server} with a
     * specific {@code address} and {@code port}.
     *
     * @param address The IP address to connect to.
     * @param port    The port to connect to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException  If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyConnectedException If a {@link Client} is already connected to any address/port.
     */
    public final void connect(String address, int port) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        try {
            channel.connect(new InetSocketAddress(address, port), this, CLIENT_LISTENER);
        } catch (AlreadyConnectedException e) {
            throw new IllegalStateException("This receiver is already connected!");
        }
    }

    /**
     * Requests {@code n} bytes and accepts a {@link Consumer<ByteBuffer>}
     * holding the {@code n} bytes once received.
     *
     * @param n        The number of bytes requested.
     * @param consumer A {@link Consumer<ByteBuffer>}.
     */
    public final void read(int n, Consumer<ByteBuffer> consumer) {
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
     * @param n        The number of bytes requested.
     * @param consumer Holds the operations that should be performed once
     *                 the {@code n} bytes are received.
     */
    public final void readAlways(int n, Consumer<ByteBuffer> consumer) {
        read(n, new Consumer<>() {
            @Override
            public void accept(ByteBuffer buffer) {
                consumer.accept(buffer);
                read(n, this);
            }
        });
    }

    /**
     * Requests a single {@code byte} and accepts a {@link Consumer}
     * with the {@code byte} when it is received.
     *
     * @param consumer A {@link Consumer<Byte>}.
     */
    public final void readByte(Consumer<Byte> consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get()));
    }

    /**
     * Calls {@link #readByte(Consumer)}, however once
     * finished, {@link #readByte(Consumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readByte(Consumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link Consumer<Byte>}.
     */
    public final void readByteAlways(Consumer<Byte> consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get()));
    }

    /**
     * Requests a single {@code char} and accepts a {@link Consumer}
     * with the {@code char} when it is received.
     *
     * @param consumer A {@link Consumer<Character>}.
     */
    public final void readChar(Consumer<Character> consumer) {
        read(Character.BYTES, buffer -> consumer.accept(buffer.getChar()));
    }

    /**
     * Calls {@link #readChar(Consumer)}, however once
     * finished, {@link #readChar(Consumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readChar(Consumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link Consumer<Character>}.
     */
    public final void readCharAlways(Consumer<Character> consumer) {
        readAlways(Character.BYTES, buffer -> consumer.accept(buffer.getChar()));
    }

    /**
     * Requests a single {@code short} and accepts a {@link Consumer}
     * with the {@code short} when it is received.
     *
     * @param consumer A {@link Consumer<Short>}.
     */
    public final void readShort(Consumer<Short> consumer) {
        read(Short.BYTES, buffer -> consumer.accept(buffer.getShort()));
    }

    /**
     * Calls {@link #readShort(Consumer)}, however once
     * finished, {@link #readShort(Consumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readShort(Consumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link Consumer<Short>}.
     */
    public final void readShortAlways(Consumer<Short> consumer) {
        readAlways(Short.BYTES, buffer -> consumer.accept(buffer.getShort()));
    }

    /**
     * Requests a single {@code int} and accepts a {@link Consumer}
     * with the {@code int} when it is received.
     *
     * @param consumer An {@link IntConsumer}.
     */
    public final void readInt(IntConsumer consumer) {
        read(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()));
    }

    /**
     * Calls {@link #readInt(IntConsumer)}, however once
     * finished, {@link #readInt(IntConsumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readInt(IntConsumer)} completes after
     * a single iteration.
     *
     * @param consumer An {@link IntConsumer}.
     */
    public final void readIntAlways(IntConsumer consumer) {
        readAlways(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()));
    }

    /**
     * Requests a single {@code float} and accepts a {@link Consumer}
     * with the {@code float} when it is received.
     *
     * @param consumer A {@link Consumer<Float>}.
     */
    public final void readFloat(Consumer<Float> consumer) {
        read(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()));
    }

    /**
     * Calls {@link #readFloat(Consumer)}, however once
     * finished, {@link #readFloat(Consumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readFloat(Consumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link Consumer<Float>}.
     */
    public final void readFloatAlways(Consumer<Float> consumer) {
        readAlways(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()));
    }

    /**
     * Requests a single {@code long} and accepts a {@link Consumer}
     * with the {@code long} when it is received.
     *
     * @param consumer A {@link LongConsumer}.
     */
    public final void readLong(LongConsumer consumer) {
        read(Long.BYTES, buffer -> consumer.accept(buffer.getLong()));
    }

    /**
     * Calls {@link #readLong(LongConsumer)}, however once
     * finished, {@link #readLong(LongConsumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readLong(LongConsumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link LongConsumer}.
     */
    public final void readLongAlways(LongConsumer consumer) {
        readAlways(Long.BYTES, buffer -> consumer.accept(buffer.getLong()));
    }

    /**
     * Requests a single {@code double} and accepts a {@link Consumer}
     * with the {@code double} when it is received.
     *
     * @param consumer A {@link DoubleConsumer}.
     */
    public final void readDouble(DoubleConsumer consumer) {
        read(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()));
    }

    /**
     * Calls {@link #readDouble(DoubleConsumer)}, however once
     * finished, {@link #readDouble(DoubleConsumer)} is called once
     * again with the same parameter; this loops indefinitely
     * whereas {@link #readDouble(DoubleConsumer)} completes after
     * a single iteration.
     *
     * @param consumer A {@link DoubleConsumer}.
     */
    public final void readDoubleAlways(DoubleConsumer consumer) {
        readAlways(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()));
    }

    /**
     * Flushes any queued {@link Packet}s held within
     * the internal {@link Queue}.
     * <p>
     * Any {@link Packet}s queued after the call to
     * {@code Client#flush()} will not be flushed until
     * it is called again.
     */
    public final void flush() {
        flush(outgoingPackets.size());
    }

    private void flush(int i) {
        if (i == 0) {
            return;
        }

        ByteBuffer raw = outgoingPackets.poll();

        if (raw == null) {
            throw new IllegalStateException("An outgoing packet is null!");
        }

        if (encryption != null) {
            try {
                encryption.doFinal(raw, raw.duplicate());
                raw.flip();
            } catch (Exception e) {
                throw new IllegalStateException("Exception occurred when encrypting:", e);
            }
        }

        channel.write(raw, null, new CompletionHandler<>() {
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
     * Gets the {@link Deque} that holds information
     * regarding requested bytes by this {@link Client}.
     *
     * @return A {@link Deque}.
     */
    private Deque<IntPair<Consumer<ByteBuffer>>> getQueue() {
        return queue;
    }

    /**
     * Gets the {@link Deque} that keeps track of nested
     * calls to {@link Client#read(int, Consumer)}.
     *
     * @return A {@link Deque}.
     */
    private Deque<IntPair<Consumer<ByteBuffer>>> getStack() {
        return stack;
    }

    /**
     * Gets the {@link Queue} that manages outgoing
     * {@link Packet}s before writing them to the
     * {@link Channel}.
     *
     * @return A {@link Queue}.
     */
    public Queue<ByteBuffer> getOutgoingPackets() {
        return outgoingPackets;
    }

    /**
     * Gets the backing {@link Channel} of this {@link Client}.
     *
     * @return This {@link Client}'s backing channel.
     */
    @Override
    public final AsynchronousSocketChannel getChannel() {
        return channel;
    }

    /**
     * Gets the {@link ByteBuffer} of this {@link Client}.
     *
     * @return This {@link Client}'s buffer.
     */
    ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets whether or not new elements being added to
     * the {@code queue} should be added to the front
     * or the back.
     *
     * @param prepend A {@code boolean}.
     */
    private void setPrepend(boolean prepend) {
        this.prepend = prepend;
    }

    public void setEncryption(Cipher encryption) {
        Objects.requireNonNull(encryption);

        if (!encryption.getAlgorithm().endsWith("NoPadding")) {
            throw new IllegalArgumentException("The cipher cannot have any padding!");
        }

        this.encryption = encryption;
    }

    public void setDecryption(Cipher decryption) {
        Objects.requireNonNull(decryption);

        if (!decryption.getAlgorithm().endsWith("NoPadding")) {
            throw new IllegalArgumentException("The cipher cannot have any padding!");
        }

        this.decryption = decryption;
    }

}
