package simplenet.packet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import simplenet.Client;
import simplenet.Server;

/**
 * A {@link Packet} that will be sent from a
 * {@link Client} to the {@link Server} or
 * vice versa.
 */
public class Packet {

    /**
     * An {@code int} representing the amount of bytes that this {@link Packet}
     * will contain in its payload.
     */
    protected int size;

    /**
     * A {@link Deque} that lazily writes data to the backing {@link ByteBuffer}.
     */
    protected final Deque<Consumer<ByteBuffer>> queue;

    /**
     * A {@code private} constructor.
     */
    private Packet() {
        queue = new ArrayDeque<>();
    }

    /**
     * Instantiates a raw {@link Packet} builder.
     */
    public static Packet builder() {
        return new Packet();
    }

    /**
     * Writes a single {@code byte} to this {@link Packet}'s payload.
     *
     * @param b An {@code int} for ease-of-use,
     *          but internally down-casted to a
     *          {@code byte}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putByte(int b) {
        size += Byte.BYTES;
        queue.offer(payload -> payload.put((byte) b));
        return this;
    }

    /**
     * Writes a variable amount of {@code byte}s to this
     * {@link Packet}'s payload.
     *
     * @param src An {@code int} array for ease-of-use,
     *            but each element is internally down-casted
     *            to a {@code byte}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putBytes(byte... src) {
        size += src.length * Byte.BYTES;

        queue.offer(payload -> {
            for (byte b : src) {
                payload.put(b);
            }
        });

        return this;
    }

    /**
     * Writes a single {@code char} to this {@link Packet}'s payload.
     *
     * @param c A {@code char}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putChar(char c) {
        size += Character.BYTES;
        queue.offer(payload -> payload.putChar(c));
        return this;
    }

    /**
     * Writes a single {@code double} to this {@link Packet}'s payload.
     *
     * @param d A {@code double}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putDouble(double d) {
        size += Double.BYTES;
        queue.offer(payload -> payload.putDouble(d));
        return this;
    }

    /**
     * Writes a single {@code float} to this {@link Packet}'s payload.
     *
     * @param f A {@code float}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putFloat(float f) {
        size += Float.BYTES;
        queue.offer(payload -> payload.putFloat(f));
        return this;
    }

    /**
     * Writes a single {@code int} to this {@link Packet}'s payload.
     *
     * @param i A {@code int}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putInt(int i) {
        size += Integer.BYTES;
        queue.offer(payload -> payload.putInt(i));
        return this;
    }

    /**
     * Writes a single {@code long} to this {@link Packet}'s payload.
     *
     * @param l A {@code long}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putLong(long l) {
        size += Long.BYTES;
        queue.offer(payload -> payload.putLong(l));
        return this;
    }

    /**
     * Writes a single {@code short} to this {@link Packet}'s payload.
     *
     * @param s A {@code short}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public final Packet putShort(short s) {
        size += Short.BYTES;
        queue.offer(payload -> payload.putShort(s));
        return this;
    }

    /**
     * Builds this {@link Packet}'s data into a {@link ByteBuffer}
     * for use in {@link #write(Client...)} and {@link #writeAndFlush(Client...)}.
     *
     * @return
     *      A {@link ByteBuffer}.
     */
    protected ByteBuffer build() {
        var payload = ByteBuffer.allocateDirect(size);
        queue.forEach(consumer -> consumer.accept(payload));
        return payload.flip();
    }

    /**
     * Queues this {@link Packet} to one (or more) {@link Client}(s).
     * <p>
     * All queued packets will be written to a {@link Client} when
     * {@link Client#flush()} is called.
     *
     * @param clients A variable amount of {@link Client}s.
     */
    public final void write(Client... clients) {
        if (clients.length == 0) {
            throw new IllegalArgumentException("You must send this packet to at least one channel!");
        }

        var payload = build();

        for (var client : clients) {
            client.getOutgoingPackets().offer(payload);
        }
    }

    /**
     * Queues this {@link Packet} to one or more {@link Client}s
     * and calls {@link Client#flush()}, flushing all
     * previously-queued packets as well.
     *
     * @param clients A variable amount of {@link Client}s.
     */
    public final void writeAndFlush(Client... clients) {
        if (clients.length == 0) {
            throw new IllegalArgumentException("You must send this packet to at least one channel!");
        }

        var payload = build();

        for (var client : clients) {
            client.getOutgoingPackets().offer(payload);
            client.flush();
        }
    }

}
