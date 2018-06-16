package simplenet.packet;

import java.io.ByteArrayOutputStream;
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
public final class Packet {

    /**
     * An {@code int} representing the amount of bytes that this {@link Packet}
     * will contain in its payload.
     */
    private int size;

    /**
     * A {@link Deque} that lazily writes data to the backing
     * {@link ByteArrayOutputStream}.
     */
    private final Deque<Consumer<ByteArrayOutputStream>> queue;

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
    public Packet putByte(int b) {
        size += Byte.BYTES;
        queue.offer(stream -> stream.write(b));
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
    public Packet putBytes(byte... src) {
        size += src.length * Byte.BYTES;

        queue.offer(stream -> {
            for (byte b : src) {
                stream.write(b);
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
    public Packet putChar(char c) {
        size += Character.BYTES;
        queue.offer(stream -> {
            stream.write((c >>> 8) & 0xFF);
            stream.write( c        & 0xFF);
        });
        return this;
    }

    /**
     * Writes a single {@code double} to this {@link Packet}'s payload.
     *
     * @param d A {@code double}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putDouble(double d) {
        putLong(Double.doubleToLongBits(d));
        return this;
    }

    /**
     * Writes a single {@code float} to this {@link Packet}'s payload.
     *
     * @param f A {@code float}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putFloat(float f) {
        putInt(Float.floatToIntBits(f));
        return this;
    }

    /**
     * Writes a single {@code int} to this {@link Packet}'s payload.
     *
     * @param i A {@code int}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putInt(int i) {
        size += Integer.BYTES;
        queue.offer(stream -> {
            stream.write((i >>> 24) & 0xFF);
            stream.write((i >>> 16) & 0xFF);
            stream.write((i >>>  8) & 0xFF);
            stream.write( i         & 0xFF);
        });
        return this;
    }

    /**
     * Writes a single {@code long} to this {@link Packet}'s payload.
     *
     * @param l A {@code long}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putLong(long l) {
        size += Long.BYTES;

        byte[] b = new byte[Long.BYTES];

        b[0] = (byte) (l >>> 56);
        b[1] = (byte) (l >>> 48);
        b[2] = (byte) (l >>> 40);
        b[3] = (byte) (l >>> 32);
        b[4] = (byte) (l >>> 24);
        b[5] = (byte) (l >>> 16);
        b[6] = (byte) (l >>>  8);
        b[7] = (byte)  l;

        queue.offer(stream -> stream.write(b, 0, b.length));
        return this;
    }

    /**
     * Writes a single {@code short} to this {@link Packet}'s payload.
     *
     * @param s A {@code short}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putShort(int s) {
        size += Short.BYTES;
        queue.offer(stream -> {
            stream.write((s >>> 8) & 0xFF);
            stream.write( s        & 0xFF);
        });
        return this;
    }

    /**
     * Writes a single {@link String} to this {@link Packet}'s payload.
     *
     * @param s A {@link String}.
     * @return The {@link Packet} to allow for
     * chained writes.
     */
    public Packet putString(String s) {
        putShort(s.length());
        putBytes(s.getBytes());
        return this;
    }

    /**
     * Prepends data to the front of the {@link Packet}.
     * <p>
     * This is primarily used for headers, such as when one
     * or more of the headers depend on the size of the data
     * contained within the {@link Packet} itself.
     *
     * @param consumer
     *      The {@link ByteArrayOutputStream} containing
     *      this packet's data.
     * @return
     *      The {@link Packet} to allow for chained writes.
     */
    public Packet prepend(Consumer<ByteArrayOutputStream> consumer) {
        queue.offerFirst(consumer);
        return this;
    }

    /**
     * Builds this {@link Packet}'s data into a {@link ByteBuffer}
     * for use in {@link #write(Client...)} and {@link #writeAndFlush(Client...)}.
     *
     * @return
     *      A {@link ByteBuffer}.
     */
    private ByteBuffer build() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        queue.forEach(consumer -> consumer.accept(stream));
        return (ByteBuffer) ByteBuffer.allocateDirect(stream.size())
                         .put(stream.toByteArray())
                         .flip();
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

        ByteBuffer payload = build();

        for (Client client : clients) {
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

        ByteBuffer payload = build();

        for (Client client : clients) {
            client.getOutgoingPackets().offer(payload);
            client.flush();
        }
    }

    /**
     * Gets the number of bytes in this {@link Packet}'s payload.
     *
     * @return
     *      The current size of this {@link Packet} measured in bytes.
     */
    public int getSize() {
        return size;
    }

}
