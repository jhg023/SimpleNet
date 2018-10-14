package simplenet.packet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.function.Consumer;
import simplenet.Client;
import simplenet.Server;

/**
 * A {@link Packet} that will be sent from a {@link Client} to the {@link Server} or vice versa.
 */
public final class Packet {

    /**
     * A {@code boolean} that designates whether data should be added
     * to the front of the {@link Deque} rather than the end.
     */
    private boolean prepend;

    /**
     * An {@code int} representing the amount of bytes that this {@link Packet}
     * will contain in its payload.
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
     * Writes a single {@code byte} to this {@link Packet}'s payload.
     *
     * @param b An {@code int} for ease-of-use, but internally down-casted to a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putByte(int b) {
        size += Byte.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.put((byte) b));
        } else {
            queue.offerLast(buffer -> buffer.put((byte) b));
        }
        return this;
    }

    /**
     * Writes a variable amount of {@code byte}s to this {@link Packet}'s payload.
     *
     * @param src An {@code int} array for ease-of-use, but each element is internally down-casted
     *            to a {@code byte}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putBytes(byte... src) {
        size += src.length * Byte.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.put(src));
        } else {
            queue.offerLast(buffer -> buffer.put(src));
        }
        return this;
    }

    /**
     * Writes a single {@code char} to this {@link Packet}'s payload.
     *
     * @param c A {@code char}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putChar(char c) {
        size += Character.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putChar(c));
        } else {
            queue.offerLast(buffer -> buffer.putChar(c));
        }
        return this;
    }

    /**
     * Writes a single {@code double} to this {@link Packet}'s payload.
     *
     * @param d A {@code double}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putDouble(double d) {
        size += Double.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putDouble(d));
        } else {
            queue.offerLast(buffer -> buffer.putDouble(d));
        }
        return this;
    }

    /**
     * Writes a single {@code float} to this {@link Packet}'s payload.
     *
     * @param f A {@code float}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putFloat(float f) {
        size += Float.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putFloat(f));
        } else {
            queue.offerLast(buffer -> buffer.putFloat(f));
        }
        return this;
    }

    /**
     * Writes a single {@code int} to this {@link Packet}'s payload.
     *
     * @param i A {@code int}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putInt(int i) {
        size += Integer.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putInt(i));
        } else {
            queue.offerLast(buffer -> buffer.putInt(i));
        }
        return this;
    }

    /**
     * Writes a single {@code long} to this {@link Packet}'s payload.
     *
     * @param l A {@code long}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putLong(long l) {
        size += Long.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putLong(l));
        } else {
            queue.offerLast(buffer -> buffer.putLong(l));
        }
        return this;
    }

    /**
     * Writes a single {@code short} to this {@link Packet}'s payload.
     *
     * @param s A {@code short}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putShort(int s) {
        size += Short.BYTES;
        if (prepend) {
            queue.offerFirst(buffer -> buffer.putShort((short) s));
        } else {
            queue.offerLast(buffer -> buffer.putShort((short) s));
        }
        return this;
    }

    /**
     * Writes a single {@link String} to this {@link Packet}'s payload.
     * <p>
     * The {@link String} can have a maximum length of {@code 65,535}.
     *
     * @param s A {@link String}.
     * @return The {@link Packet} to allow for chained writes.
     */
    public Packet putString(String s) {
        putShort(s.length());
        putBytes(s.getBytes());
        return this;
    }

    /**
     * Prepends data to the front of the {@link Packet}.
     * <p>
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
     * <p>
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
     * <p>
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
