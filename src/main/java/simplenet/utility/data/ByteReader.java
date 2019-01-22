package simplenet.utility.data;

import java.nio.ByteOrder;
import java.util.function.Consumer;
import simplenet.utility.exposed.ByteConsumer;

/**
 * An interface that defines the methods required to read {@code byte}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface ByteReader extends DataReader {
    
    /**
     * Calls {@link #readByte(ByteConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     * @see #readByte(ByteConsumer, ByteOrder)
     */
    default void readByte(ByteConsumer consumer) {
        readByte(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code byte}, with the specified {@link ByteOrder}, and accepts a {@link ByteConsumer} with the
     * {@code byte} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     * @param order    The byte order of the data being received.
     */
    default void readByte(ByteConsumer consumer, ByteOrder order) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get()), order);
    }
    
    /**
     * Calls {@link #readByteAlways(ByteConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     * @see #readByteAlways(ByteConsumer, ByteOrder)
     */
    default void readByteAlways(ByteConsumer consumer) {
        readByteAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readByte(ByteConsumer, ByteOrder)}; however, once finished, {@link #readByte(ByteConsumer, ByteOrder)}
     * is called once again with the same consumer; this method loops indefinitely, whereas
     * {@link #readByte(ByteConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     * @param order    The byte order of the data being received.
     */
    default void readByteAlways(ByteConsumer consumer, ByteOrder order) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get()), order);
    }
    
    /**
     * Calls {@link #readBytes(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     * @see #readBytes(int, Consumer, ByteOrder)
     */
    default void readBytes(int n, Consumer<byte[]> consumer) {
        readBytes(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code byte[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer} when
     * all of the {@code byte}s are received.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readBytes(int n, Consumer<byte[]> consumer, ByteOrder order) {
        read(n, buffer -> {
            var b = new byte[n];
            buffer.get(b);
            consumer.accept(b);
        }, order);
    }
    
    /**
     * Calls {@link #readBytesAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     * @see #readBytesAlways(int, Consumer, ByteOrder)
     */
    default void readBytesAlways(int n, Consumer<byte[]> consumer) {
        readBytesAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readBytes(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readBytes(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readBytes(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readBytesAlways(int n, Consumer<byte[]> consumer, ByteOrder order) {
        readAlways(n, buffer -> {
            var b = new byte[n];
            buffer.get(b);
            consumer.accept(b);
        }, order);
    }
    
}
