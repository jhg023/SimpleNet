package simplenet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplenet.channel.Channeled;
import simplenet.packet.Packet;
import simplenet.receiver.Receiver;
import simplenet.utility.IntPair;
import simplenet.utility.exposed.ByteConsumer;
import simplenet.utility.exposed.CharConsumer;
import simplenet.utility.exposed.FloatConsumer;
import simplenet.utility.exposed.ShortConsumer;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Client extends Receiver<Runnable> implements Channeled<AsynchronousSocketChannel> {

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    /**
     * The {@link CompletionHandler} used to process bytes when they are received by this {@link Client}.
     */
    static class Listener implements CompletionHandler<Integer, Client> {

        /**
         * A {@code static} instance of this class to be reused.
         */
        private static final Listener INSTANCE = new Listener();

        @Override
        public void completed(Integer result, Client client) {
            // A result of -1 normally means that the end-of-stream has been
            // reached. In that case, close the client's connection.
            if (result == -1) {
                client.close();
                return;
            }

            client.size += result;

            var buffer = client.buffer.flip();
            var queue = client.queue;

            IntPair<Consumer<ByteBuffer>> peek;

            if ((peek = queue.pollLast()) == null) {
                client.channel.read(buffer.flip().limit(buffer.capacity()), client, this);
                return;
            }

            client.prepend = true;

            boolean decrypt = client.decryption != null;

            var stack = client.stack;

            int key;

            while (client.size >= (key = peek.getKey())) {
                if (decrypt) {
                    try {
                        int position = buffer.position();
                        buffer.limit(buffer.position() + key);
                        client.decryption.update(buffer, buffer.duplicate());
                        buffer.flip().position(position);
                    } catch (Exception e) {
                        throw new IllegalStateException("Exception occurred when decrypting:", e);
                    }
                }

                client.size -= key;

                peek.getValue().accept(buffer);

                while (!stack.isEmpty()) {
                    queue.offer(stack.poll());
                }

                if ((peek = queue.pollLast()) == null) {
                    break;
                }
            }

            client.prepend = false;

            if (peek != null) {
                queue.addLast(peek);
            }

            if (client.size > 0) {
                buffer.compact();
            } else {
                buffer.flip();
            }

            client.channel.read(buffer.limit(buffer.capacity()), client, this);
        }

        @Override
        public void failed(Throwable t, Client client) {
            client.close();
        }
    }

    /**
     * The {@link CompletionHandler} used when this {@link Client} sends one or more {@link Packet}s
     * to a {@link Server}.
     */
    private static final CompletionHandler<Integer, Client> PACKET_HANDLER = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Client client) {
            if (!client.channel.isOpen()) {
                client.outgoingPackets.clear();
                client.packetsToFlush.clear();
                return;
            }

            var payload = client.packetsToFlush.pollLast();

            if (payload == null) {
                client.writing.set(false);
                return;
            }

            client.channel.write(payload, client, this);
        }

        @Override
        public void failed(Throwable t, Client client) {
            t.printStackTrace();
        }
    };

    /**
     * A thread-safe method of keeping track whether this {@link Client} is currently writing data
     * to the network.
     */
    private final AtomicBoolean writing;

    /**
     * The {@link ByteBuffer} that will hold data sent by the {@link Client} or {@link Server}.
     */
    private final ByteBuffer buffer;

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<Packet> outgoingPackets;

    /**
     * A {@link Deque} to manage {@link Packet}s that should be flushed as soon as possible.
     */
    private final Deque<ByteBuffer> packetsToFlush;

    /**
     * The {@link Deque} that keeps track of nested calls to {@link Client#read(int, Consumer)}
     * and assures that they will complete in the expected order.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> stack;

    /**
     * The {@link Deque} used when requesting a certain amount of bytes from the {@link Client} or
     * {@link Server}.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> queue;

    /**
     * Whether or not new elements added {@code queue} should be added to the front rather than the back.
     */
    private boolean prepend;

    /**
     * The amount of readable {@code byte}s that currently exist within this {@link Client}'s {@code buffer}.
     */
    private int size;

    /**
     * The {@link Cipher} used for {@link Packet} encryption.
     */
    private Cipher encryption;

    /**
     * The {@link Cipher} used for {@link Packet} decryption.
     */
    private Cipher decryption;

    /**
     * The backing {@link ThreadPoolExecutor} used for I/O.
     */
    private ThreadPoolExecutor executor;

    /**
     * The backing {@link Channel} of a {@link Client}.
     */
    private AsynchronousSocketChannel channel;

    /**
     * Instantiates a new {@link Client} by attempting to open the backing
     * {@link AsynchronousSocketChannel} with a default buffer size of {@code 4096} bytes.
     */
    public Client() {
        this(4096);
    }

    /**
     * Instantiates a new {@link Client} by attempting to open the backing {@link AsynchronousSocketChannel}
     * with a provided buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     */
    public Client(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Instantiates a new {@link Client} with an existing {@link AsynchronousSocketChannel} with a provided
     * buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     * @param channel    The channel to back this {@link Client} with.
     */
    public Client(int bufferSize, AsynchronousSocketChannel channel) {
        super(bufferSize);

        writing = new AtomicBoolean();
        outgoingPackets = new ArrayDeque<>();
        packetsToFlush = new ConcurrentLinkedDeque<>();
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();
        buffer = ByteBuffer.allocateDirect(bufferSize);

        if (channel != null) {
            this.channel = channel;
        }
    }

    public Client(Client client) {
        super(client);

        this.prepend = client.prepend;
        this.buffer = client.buffer;
        this.channel = client.channel;
        this.encryption = client.encryption;
        this.decryption = client.decryption;
        this.executor = client.executor;
        this.outgoingPackets = client.outgoingPackets;
        this.stack = client.stack;
        this.queue = client.queue;
        this.writing = client.writing;
        this.packetsToFlush = client.packetsToFlush;
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port}
     * and a default timeout of {@code 30} seconds.
     *
     * @param address The IP address to connect to.
     * @param port    The port to connect to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException  If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyConnectedException If a {@link Client} is already connected to any address/port.
     */
    public final void connect(String address, int port) {
        connect(address, port, 30L, TimeUnit.SECONDS, () -> {
            logger.error("Couldn't connect within 30 seconds!");
        });
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port}
     * and a specified timeout.  If the timeout is reached, then the {@link Runnable} is run and
     * the backing {@link AsynchronousSocketChannel} is closed.
     *
     * @param address   The IP address to connect to.
     * @param port      The port to connect to {@code 0 <= port <= 65535}.
     * @param timeout   The timeout value.
     * @param unit      The timeout unit.
     * @param onTimeout The {@link Runnable} that runs if this connection attempt times out.
     */
    public final void connect(String address, int port, long timeout, TimeUnit unit, Runnable onTimeout) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            logger.error("The port must be between 0 and 65535!");
            return;
        }

        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            return thread;
        });

        executor.prestartAllCoreThreads();

        try {
            this.channel = AsynchronousSocketChannel.open(AsynchronousChannelGroup.withThreadPool(executor));
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
            this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            logger.error("Unable to open the channel!");
            return;
        }

        try {
            channel.connect(new InetSocketAddress(address, port)).get(timeout, unit);
        } catch (AlreadyConnectedException e) {
            logger.error("This client is already connected to a server!");
        } catch (ExecutionException e) {
            logger.error("An ExecutionException has occurred:", e);
        } catch (Exception e) {
            onTimeout.run();
            close();
            return;
        }

        connectListeners.forEach(Runnable::run);

        try {
            channel.read(buffer, this, Listener.INSTANCE);
        } catch (ShutdownChannelGroupException e) {
            // This exception is caught whenever a client closes their
            // connection to the server. In that case, do nothing.
        }
    }

    /**
     * Closes this {@link Client}'s backing {@link AsynchronousSocketChannel} after flushing any queued packets.
     * <br><br>
     * Any registered pre-disconnect listeners are fired before remaining packets are flushed, and registered
     * post-disconnect listeners are fired after the backing channel has closed successfully.
     * <br><br>
     * Listeners are fired in the same order that they were registered in.
     */
    @Override
    public void close() {
        preDisconnectListeners.forEach(Runnable::run);

        flush();

        while (writing.get()) {
            Thread.onSpinWait();
        }

        Channeled.super.close();

        if (executor != null) {
            executor.shutdownNow();
        }

        while (channel.isOpen()) {
            Thread.onSpinWait();
        }

        postDisconnectListeners.forEach(Runnable::run);
    }

    /**
     * Registers a listener that fires right before a {@link Client} disconnects from a {@link Server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     * <br><br>
     * If this {@link Client}'s connection to a {@link Server} is lost unexpectedly, then its backing
     * {@link AsynchronousSocketChannel} may already be closed.
     *
     * @param listener A {@link Runnable}.
     */
    public void preDisconnect(Runnable listener) {
        preDisconnectListeners.add(listener);
    }

    /**
     * Registers a listener that fires right after a {@link Client} disconnects from a {@link Server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     *
     * @param listener A {@link Runnable}.
     */
    public void postDisconnect(Runnable listener) {
        postDisconnectListeners.add(listener);
    }

    /**
     * Requests {@code n} bytes and accepts a {@link Consumer} holding the {@code n} bytes once received.
     *
     * @param n        The number of bytes requested.
     * @param consumer A {@link Consumer}.
     */
    public final void read(int n, Consumer<ByteBuffer> consumer) {
        if (size >= n) {
            size -= n;
            consumer.accept(buffer);
            return;
        }

        if (prepend) {
            stack.addFirst(new IntPair<>(n, consumer));
        } else {
            queue.offer(new IntPair<>(n, consumer));
        }
    }

    /**
     * Calls {@link #read(int, Consumer)}, however once finished, {@link #read(int, Consumer)}
     * is called once again with the same parameters; this loops indefinitely whereas
     * {@link #read(int, Consumer)} completes after a single iteration.
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
     * A helper method to block until the {@link CompletableFuture} contains a value.
     *
     * @param future The {@link CompletableFuture} to wait for.
     * @param <T>    The type of the {@link CompletableFuture} and the return type.
     * @return The instance of {@code T} contained in the {@link CompletableFuture}.
     */
    private <T> T read(CompletableFuture<T> future) {
        return future.join();
    }

    /**
     * Reads a {@code byte} from the network, but blocks the executing thread unlike
     * {@link #readByte(ByteConsumer)}.
     *
     * @return A {@code byte}.
     */
    public final byte readByte() {
        var future = new CompletableFuture<Byte>();
        readByte(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code byte} and accepts a {@link ByteConsumer} with the {@code byte}
     * when it is received.
     *
     * @param consumer A {@link ByteConsumer}.
     */
    public final void readByte(ByteConsumer consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get()));
    }

    /**
     * Calls {@link #readByte(ByteConsumer)}, however once finished, {@link #readByte(ByteConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readByte(ByteConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link ByteConsumer}.
     */
    public final void readByteAlways(ByteConsumer consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get()));
    }

    /**
     * Reads a {@code byte} array from the network, but blocks the executing thread unlike
     * {@link #readBytes(int, Consumer)}.
     *
     * @param n The number of bytes requested.
     * @return A {@code byte} array.
     */
    public final byte[] readBytes(int n) {
        var future = new CompletableFuture<byte[]>();
        readBytes(n, future::complete);
        return read(future);
    }

    /**
     * Requests a byte array of size {@code n} and accepts a {@link Consumer} with the byte array when
     * all of the bytes are received.
     *
     * @param n        The number of bytes requested.
     * @param consumer A {@link Consumer}.
     */
    public final void readBytes(int n, Consumer<byte[]> consumer) {
        read(n, buffer -> {
           byte[] b = new byte[n];
           buffer.get(b);
           consumer.accept(b);
        });
    }

    /**
     * Calls {@link #readBytes(int, Consumer)}, however once finished, {@link #readBytes(int, Consumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readBytes(int, Consumer)} completes after a single iteration.
     *
     * @param n        The number of bytes requested.
     * @param consumer A {@link Consumer}.
     */
    public final void readBytesAlways(int n, Consumer<byte[]> consumer) {
        readAlways(n, buffer -> {
            byte[] b = new byte[n];
            buffer.get(b);
            consumer.accept(b);
        });
    }

    /**
     * Reads a {@code char} from the network, but blocks the executing thread unlike
     * {@link #readChar(CharConsumer)}.
     *
     * @return A {@code char}.
     */
    public final char readChar() {
        var future = new CompletableFuture<Character>();
        readChar(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code char} and accepts a {@link CharConsumer} with the {@code char}
     * when it is received.
     *
     * @param consumer A {@link CharConsumer}.
     */
    public final void readChar(CharConsumer consumer) {
        read(Character.BYTES, buffer -> consumer.accept(buffer.getChar()));
    }

    /**
     * Calls {@link #readChar(CharConsumer)}, however once finished, {@link #readChar(CharConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readChar(CharConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link CharConsumer}.
     */
    public final void readCharAlways(CharConsumer consumer) {
        readAlways(Character.BYTES, buffer -> consumer.accept(buffer.getChar()));
    }

    /**
     * Reads a {@code double} from the network, but blocks the executing thread unlike
     * {@link #readDouble(DoubleConsumer)}.
     *
     * @return A {@code double}.
     */
    public final double readDouble() {
        var future = new CompletableFuture<Double>();
        readDouble(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code double} and accepts a {@link Consumer} with the {@code double} when
     * it is received.
     *
     * @param consumer A {@link DoubleConsumer}.
     */
    public final void readDouble(DoubleConsumer consumer) {
        read(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()));
    }

    /**
     * Calls {@link #readDouble(DoubleConsumer)}, however once finished,
     * {@link #readDouble(DoubleConsumer)} is called once again with the same parameter; this loops
     * indefinitely whereas {@link #readDouble(DoubleConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link DoubleConsumer}.
     */
    public final void readDoubleAlways(DoubleConsumer consumer) {
        readAlways(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()));
    }

    /**
     * Reads a {@code float} from the network, but blocks the executing thread unlike
     * {@link #readFloat(FloatConsumer)}.
     *
     * @return A {@code float}.
     */
    public final float readFloat() {
        var future = new CompletableFuture<Float>();
        readFloat(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code float} and accepts a {@link FloatConsumer} with the {@code float} when
     * it is received.
     *
     * @param consumer A {@link FloatConsumer}.
     */
    public final void readFloat(FloatConsumer consumer) {
        read(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()));
    }

    /**
     * Calls {@link #readFloat(FloatConsumer)}, however once finished, {@link #readFloat(FloatConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readFloat(FloatConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link FloatConsumer}.
     */
    public final void readFloatAlways(FloatConsumer consumer) {
        readAlways(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()));
    }

    /**
     * Reads an {@code int} from the network, but blocks the executing thread unlike
     * {@link #readInt(IntConsumer)}.
     *
     * @return An {@code int}.
     */
    public final int readInt() {
        var future = new CompletableFuture<Integer>();
        readInt(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code int} and accepts a {@link Consumer} with the {@code int} when
     * it is received.
     *
     * @param consumer An {@link IntConsumer}.
     */
    public final void readInt(IntConsumer consumer) {
        read(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()));
    }

    /**
     * Calls {@link #readInt(IntConsumer)}, however once finished, {@link #readInt(IntConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readInt(IntConsumer)} completes after a single iteration.
     *
     * @param consumer An {@link IntConsumer}.
     */
    public final void readIntAlways(IntConsumer consumer) {
        readAlways(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()));
    }

    /**
     * Reads a {@code long} from the network, but blocks the executing thread
     * unlike {@link #readLong(LongConsumer)}.
     *
     * @return A {@code long}.
     */
    public final long readLong() {
        var future = new CompletableFuture<Long>();
        readLong(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code long} and accepts a {@link Consumer} with the {@code long} when
     * it is received.
     *
     * @param consumer A {@link LongConsumer}.
     */
    public final void readLong(LongConsumer consumer) {
        read(Long.BYTES, buffer -> consumer.accept(buffer.getLong()));
    }

    /**
     * Calls {@link #readLong(LongConsumer)}, however once finished, {@link #readLong(LongConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readLong(LongConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link LongConsumer}.
     */
    public final void readLongAlways(LongConsumer consumer) {
        readAlways(Long.BYTES, buffer -> consumer.accept(buffer.getLong()));
    }

    /**
     * Reads a {@code short} from the network, but blocks the executing thread unlike
     * {@link #readShort(ShortConsumer)}.
     *
     * @return A {@code short}.
     */
    public final short readShort() {
        var future = new CompletableFuture<Short>();
        readShort(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code short} and accepts a {@link Consumer} with the {@code short} when
     * it is received.
     *
     * @param consumer A {@link ShortConsumer}.
     */
    public final void readShort(ShortConsumer consumer) {
        read(Short.BYTES, buffer -> consumer.accept(buffer.getShort()));
    }

    /**
     * Calls {@link #readShort(ShortConsumer)}, however once finished, {@link #readShort(ShortConsumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readShort(ShortConsumer)} completes after a single iteration.
     *
     * @param consumer A {@link ShortConsumer}.
     */
    public final void readShortAlways(ShortConsumer consumer) {
        readAlways(Short.BYTES, buffer -> consumer.accept(buffer.getShort()));
    }

    /**
     * Reads a {@link String} from the network, but blocks the executing thread unlike
     * {@link #readString(Consumer)}.
     *
     * @return A {@code String}.
     */
    public final String readString() {
        var future = new CompletableFuture<String>();
        readString(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@link String} and accepts a {@link Consumer} with the {@link String}
     * when it is received. A {@code short} is used to store the length of the {@link String},
     * which imposes a maximum {@link String} length of {@code 65,535}.
     *
     * @param consumer A {@link Consumer}.
     */
    public final void readString(Consumer<String> consumer) {
        readShort(s -> {
            read(s, buffer -> {
                var b = new byte[s & 0xFFFF];
                buffer.get(b);
                consumer.accept(new String(b));
            });
        });
    }

    /**
     * Calls {@link #readString(Consumer)}, however once finished, {@link #readString(Consumer)}
     * is called once again with the same parameter; this loops indefinitely whereas
     * {@link #readString(Consumer)} completes after a single iteration.
     *
     * @param consumer A {@link Consumer}.
     */
    public final void readStringAlways(Consumer<String> consumer) {
        readShortAlways(s -> {
            read(s, buffer -> {
                var b = new byte[s & 0xFFFF];
                buffer.get(b);
                consumer.accept(new String(b));
            });
        });
    }

    /**
     * Flushes any queued {@link Packet}s held within the internal {@link Queue}.
     * <br><br>
     * Any {@link Packet}s queued after the call to this method will not be flushed until
     * this method is called again.
     */
    public final void flush() {
        int totalBytes = 0;
        int amountToPoll = outgoingPackets.size();

        Packet packet;

        var queue = new ArrayDeque<Consumer<ByteBuffer>>();

        while (amountToPoll-- > 0 && (packet = outgoingPackets.poll()) != null) {
            int currentBytes = totalBytes;

            boolean tooBig = (totalBytes += packet.getSize()) >= bufferSize;
            boolean empty = outgoingPackets.isEmpty();

            if (!tooBig || empty) {
                queue.addAll(packet.getQueue());
            }

            // If we've buffered all of the packets that we can, send them off.
            if (tooBig || empty) {
                var raw = ByteBuffer.allocateDirect(empty ? totalBytes : currentBytes);

                Consumer<ByteBuffer> consumer;

                while ((consumer = queue.pollFirst()) != null) {
                    consumer.accept(raw);
                }

                queue.addAll(packet.getQueue());

                raw.flip();

                if (encryption != null) {
                    try {
                        encryption.update(raw, raw.duplicate());
                        raw.flip();
                    } catch (Exception e) {
                        logger.error("Exception occurred when encrypting:", e);
                        return;
                    }
                }

                if (!writing.getAndSet(true)) {
                    channel.write(raw, this, PACKET_HANDLER);
                } else {
                    packetsToFlush.offerFirst(raw);
                }

                totalBytes = packet.getSize();
            }
        }
    }

    /**
     * Gets the {@link Queue} that manages outgoing {@link Packet}s before writing them to the
     * {@link Channel}.
     *
     * @return A {@link Queue}.
     */
    public Queue<Packet> getOutgoingPackets() {
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

    public void setEncryption(Cipher encryption) {
        Objects.requireNonNull(encryption);

        if (!encryption.getAlgorithm().endsWith("NoPadding")) {
            logger.error("The cipher cannot have any padding!");
            return;
        }

        this.encryption = encryption;
    }

    public void setDecryption(Cipher decryption) {
        Objects.requireNonNull(decryption);

        if (!decryption.getAlgorithm().endsWith("NoPadding")) {
            logger.error("The cipher cannot have any padding!");
            return;
        }

        this.decryption = decryption;
    }

}
