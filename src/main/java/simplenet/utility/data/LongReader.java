package simplenet.utility.data;

import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * An interface that defines the methods required to read {@code long}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface LongReader extends DataReader {
    
    /**
     * Calls {@link #readLong(LongConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @see #readLong(LongConsumer, ByteOrder)
     */
    default void readLong(LongConsumer consumer) {
        readLong(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code long}, with the specified {@link ByteOrder}, and accepts a {@link LongConsumer} with
     * the {@code long} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @param order    The byte order of the data being received.
     */
    default void readLong(LongConsumer consumer, ByteOrder order) {
        read(Long.BYTES, buffer -> consumer.accept(buffer.getLong()), order);
    }
    
    /**
     * Calls {@link #readLongAlways(LongConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @see #readLongAlways(LongConsumer, ByteOrder)
     */
    default void readLongAlways(LongConsumer consumer) {
        readLongAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readLong(LongConsumer, ByteOrder)}; however, once finished,
     * {@link #readLong(LongConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readLong(LongConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @param order    The byte order of the data being received.
     */
    default void readLongAlways(LongConsumer consumer, ByteOrder order) {
        readAlways(Long.BYTES, buffer -> consumer.accept(buffer.getLong()), order);
    }
    
    /**
     * Calls {@link #readLongs(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @see #readLongs(int, Consumer, ByteOrder)
     */
    default void readLongs(int n, Consumer<long[]> consumer) {
        readLongs(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code long[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code long}s are received.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readLongs(int n, Consumer<long[]> consumer, ByteOrder order) {
        read(n, buffer -> {
            var l = new long[n];
            buffer.asLongBuffer().get(l);
            consumer.accept(l);
        }, order);
    }
    
    /**
     * Calls {@link #readLongsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     */
    default void readLongsAlways(int n, Consumer<long[]> consumer) {
        readLongsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readLongs(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readLongs(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readLongs(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readLongsAlways(int n, Consumer<long[]> consumer, ByteOrder order) {
        readAlways(n, buffer -> {
            var l = new long[n];
            buffer.asLongBuffer().get(l);
            consumer.accept(l);
        }, order);
    }
    
}
