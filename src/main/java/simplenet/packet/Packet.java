package simplenet.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.function.Consumer;
import simplenet.Client;
import simplenet.Server;

/**
 * A {@link Packet} that will be sent from a {@link Client} to the {@link Server} or vice versa.
 * <br><br>
 * This class is <strong>NOT</strong> safe for concurrent use among multiple threads.
 */
public final class Packet {

    /**
     * A {@code boolean} that designates whether data should be added to the front of the {@link Deque} rather than
     * the end.
     */
    private boolean prepend;

    /**
     * An {@code int} representing the amount of bytes that this {@link Packet} will contain in its payload.
     */
    private int size;

    /**
     * A {@link Deque} that lazily writes data to the backing {@link ByteBuffer}.
     */
    private final Deque<Consumer<ByteBuffer>> queue;

    /**
     * A {@code private} constructor.
     */
    private Packet() {
        this.queue = new ArrayDeque<>();
    }

    /**
     * Instantiates a raw {@link Packet} builder.
     *
     * @return An instance of {@link Packet}.
     */
    public static Packet builder() {
        return new Packet();
    }
    
    /**
     * Writes a single {@code boolean} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param b A {@code boolean}, that is internally written as a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putBoolean(boolean, ByteOrder)
     */
    public Packet putBoolean(boolean b) {
        return putBoolean(b, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code boolean} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param b A {@code boolean}, that is internally written as a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putBoolean(boolean b, ByteOrder order) {
        size += Byte.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).put((byte) (b ? 1 : 0)));
        } else {
            queue.offerLast(buffer -> buffer.order(order).put((byte) (b ? 1 : 0)));
        }
        return this;
    }
    
    /**
     * Writes a single {@code byte} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param b An {@code int} for ease-of-use, but internally down-casted to a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putByte(int, ByteOrder)
     */
    public Packet putByte(int b) {
        return putByte(b, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code byte} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param b An {@code int} for ease-of-use, but internally down-casted to a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putByte(int b, ByteOrder order) {
        size += Byte.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).put((byte) b));
        } else {
            queue.offerLast(buffer -> buffer.order(order).put((byte) b));
        }
        return this;
    }

    /**
     * Writes a variable amount of {@code byte}s with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s
     * payload.
     *
     * @param src A variable amount of {@code byte}s.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putBytes(ByteOrder, byte...)
     */
    public Packet putBytes(byte... src) {
        return putBytes(ByteOrder.BIG_ENDIAN, src);
    }
    
    /**
     * Writes a variable amount of {@code byte}s with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param src A variable amount of {@code byte}s.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putBytes(ByteOrder order, byte... src) {
        size += src.length * Byte.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).put(src));
        } else {
            queue.offerLast(buffer -> buffer.order(order).put(src));
        }
        return this;
    }

    /**
     * Writes a single {@code char} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param c A {@code char}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putChar(char, ByteOrder)
     */
    public Packet putChar(char c) {
        return putChar(c, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code char} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param c A {@code char}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putChar(char c, ByteOrder order) {
        size += Character.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putChar(c));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putChar(c));
        }
        return this;
    }
    
    /**
     * Writes a single {@code double} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param d A {@code double}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putDouble(double, ByteOrder)
     */
    public Packet putDouble(double d) {
        return putDouble(d, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code double} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param d A {@code double}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putDouble(double d, ByteOrder order) {
        size += Double.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putDouble(d));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putDouble(d));
        }
        return this;
    }
    
    /**
     * Writes a single {@code float} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param f A {@code float}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putFloat(float, ByteOrder)
     */
    public Packet putFloat(float f) {
        return putFloat(f, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code float} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param f A {@code float}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putFloat(float f, ByteOrder order) {
        size += Float.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putFloat(f));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putFloat(f));
        }
        return this;
    }
    
    /**
     * Writes a single {@code int} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param i An {@code int}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putInt(int, ByteOrder)
     */
    public Packet putInt(int i) {
        return putInt(i, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code int} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param i An {@code int}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putInt(int i, ByteOrder order) {
        size += Integer.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putInt(i));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putInt(i));
        }
        return this;
    }
    
    /**
     * Writes a single {@code long} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param l A {@code long}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putLong(long, ByteOrder)
     */
    public Packet putLong(long l) {
        return putLong(l, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code long} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param l A {@code long}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putLong(long l, ByteOrder order) {
        size += Long.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putLong(l));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putLong(l));
        }
        return this;
    }
    
    /**
     * Writes a single {@code short} with {@link ByteOrder#BIG_ENDIAN} order to this {@link Packet}'s payload.
     *
     * @param s An {@code int} for ease-of-use, but internally down-casted to a {@code short}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putShort(int, ByteOrder)
     */
    public Packet putShort(int s) {
        return putShort(s, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@code short} with the specified {@link ByteOrder} to this {@link Packet}'s payload.
     *
     * @param s An {@code int} for ease-of-use, but internally down-casted to a {@code short}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putShort(int s, ByteOrder order) {
        size += Short.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.order(order).putShort((short) s));
        } else {
            queue.offerLast(buffer -> buffer.order(order).putShort((short) s));
        }
        return this;
    }

    /**
     * Writes a single {@link StandardCharsets#UTF_8}-encoded {@link String} with {@link ByteOrder#BIG_ENDIAN} order to
     * this {@link Packet}'s payload.
     * <br><br>
     * The {@link String} can have a maximum length of {@code 65,535}.
     *
     * @param s A {@link String}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putString(String, Charset, ByteOrder)
     */
    public Packet putString(String s) {
        return putString(s, StandardCharsets.UTF_8, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@link String} encoded with the specified {@link Charset} and {@link ByteOrder#BIG_ENDIAN}
     * order to this {@link Packet}'s payload.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} (or less) with a different encoding.
     *
     * @param s A {@link String}.
     * @return The {@link Packet} to allow for chained writes.
     * @see #putString(String, Charset, ByteOrder)
     */
    public Packet putString(String s, Charset charset) {
        return putString(s, charset, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Writes a single {@link String} encoded with the specified {@link Charset} and {@link ByteOrder} to this
     * {@link Packet}'s payload.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} (or less) with a different encoding.
     *
     * @param s A {@link String}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putString(String s, Charset charset, ByteOrder order) {
        var bytes = s.getBytes(charset);
        putShort(bytes.length, order);
        putBytes(order, bytes);
        return this;
    }

    /**
     * Prepends data to the front of the {@link Packet}.
     * <br><br>
     * This is primarily used for headers, such as when one or more of the headers depend on the size
     * of the data contained within the {@link Packet} itself.
     *
     * @param runnable The {@link Runnable} containing calls to add more data to this {@link Packet}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet prepend(Runnable runnable) {
        prepend = true;
        runnable.run();
        prepend = false;
        return this;
    }

    /**
     * Queues this {@link Packet} to one (or more) {@link Client}(s).
     * <br><br>
     * All queued packets will be written to a {@link Client} when {@link Client#flush()} is called.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param clients A variable amount of {@link Client}s.
     */
    @SafeVarargs
    public final <T extends Client> void write(T... clients) {
        if (clients.length == 0) {
            throw new IllegalArgumentException("You must send this packet to at least one client!");
        }

        for (Client client : clients) {
            if (size > client.getBufferSize()) {
                System.err.println("Packet is too large (Size: " + size + ") for client buffer size (Limit: " + client.getBufferSize() + ")");
                continue;
            }

            client.getOutgoingPackets().offer(this);
        }
    }

    /**
     * Queues this {@link Packet} to one (or more) {@link Client}(s).
     * <br><br>
     * All queued packets will be written to a {@link Client} when {@link Client#flush()} is called.
     *
     * @param clients A {@link Collection} of {@link Client}s.
     */
    public final void write(Collection<? extends Client> clients) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("You must send this packet to at least one client!");
        }

        clients.forEach(client -> {
            if (size > client.getBufferSize()) {
                System.err.println("Packet is too large (Size: " + size + ") for client buffer size (Limit: " + client.getBufferSize() + ")");
                return;
            }

            client.getOutgoingPackets().offer(Packet.this);
        });
    }

    /**
     * Queues this {@link Packet} to one or more {@link Client}s and calls {@link Client#flush()},
     * flushing all previously-queued packets as well.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param clients A variable amount of {@link Client}s.
     */
    @SafeVarargs
    public final <T extends Client> void writeAndFlush(T... clients) {
        if (clients.length == 0) {
            throw new IllegalArgumentException("You must send this packet to at least one client!");
        }

        for (Client client : clients) {
            if (size > client.getBufferSize()) {
                System.err.println("Packet is too large (Size: " + size + ") for client buffer size (Limit: " + client.getBufferSize() + ")");
                continue;
            }

            client.getOutgoingPackets().offer(this);
            client.flush();
        }
    }

    /**
     * Queues this {@link Packet} to one or more {@link Client}s and calls {@link Client#flush()},
     * flushing all previously-queued packets as well.
     *
     * @param clients A {@link Collection} of {@link Client}s.
     */
    public final void writeAndFlush(Collection<? extends Client> clients) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("You must send this packet to at least one client!");
        }

        clients.forEach(client -> {
            if (size > client.getBufferSize()) {
                throw new IllegalStateException("Packet is too large (Size: " + size + ") for client buffer size (Limit: " + client.getBufferSize() + ")");
            }

            client.getOutgoingPackets().offer(Packet.this);
            client.flush();
        });
    }

    /**
     * Gets the number of bytes in this {@link Packet}'s payload.
     *
     * @return The current size of this {@link Packet} measured in bytes.
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the backing {@link Deque} of this {@link Packet}.
     *
     * @return A {@link Deque}.
     */
    public Deque<Consumer<ByteBuffer>> getQueue() {
        return queue;
    }

}
