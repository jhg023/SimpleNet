package simplenet.utility.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import simplenet.utility.exposed.ShortConsumer;

/**
 * An interface that defines the methods required to read {@code short}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface ShortReader extends DataReader {
    
    /**
     * Reads a {@code short} with {@link ByteOrder#BIG_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readShort(ShortConsumer)}.
     *
     * @return A {@code short}.
     * @see #readShort(ByteOrder)
     */
    default short readShort() {
        return readShort(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Reads a {@code short} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readShort(ShortConsumer)}.
     *
     * @return A {@code short}.
     */
    default short readShort(ByteOrder order) {
        var future = new CompletableFuture<Short>();
        readShort(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readShort(ShortConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code short} is received.
     * @see #readShort(ShortConsumer, ByteOrder)
     */
    default void readShort(ShortConsumer consumer) {
        readShort(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code short}, with the specified {@link ByteOrder}, and accepts a {@link ShortConsumer} with
     * the {@code short} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code short} is received.
     * @param order    The byte order of the data being received.
     */
    default void readShort(ShortConsumer consumer, ByteOrder order) {
        read(Short.BYTES, buffer -> consumer.accept(buffer.getShort()), order);
    }
    
    /**
     * Calls {@link #readShortAlways(ShortConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code short} is received.
     * @see #readShortAlways(ShortConsumer, ByteOrder)
     */
    default void readShortAlways(ShortConsumer consumer) {
        readShortAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readShort(ShortConsumer, ByteOrder)}; however, once finished,
     * {@link #readShort(ShortConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readShort(ShortConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code short} is received.
     * @param order    The byte order of the data being received.
     */
    default void readShortAlways(ShortConsumer consumer, ByteOrder order) {
        readAlways(Short.BYTES, buffer -> consumer.accept(buffer.getShort()), order);
    }
    
    /**
     * Calls {@link #readShorts(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code short}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code short}s are received.
     * @see #readShorts(int, Consumer, ByteOrder)
     */
    default void readShorts(int n, Consumer<short[]> consumer) {
        readShorts(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code short[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code short}s are received.
     *
     * @param n        The amount of {@code short}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code short}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readShorts(int n, Consumer<short[]> consumer, ByteOrder order) {
        read(Short.BYTES * n, buffer -> processShorts(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readShortsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code short}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code short}s are received.
     */
    default void readShortsAlways(int n, Consumer<short[]> consumer) {
        readShortsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readShorts(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readShorts(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readShorts(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code short}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code short}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readShortsAlways(int n, Consumer<short[]> consumer, ByteOrder order) {
        readAlways(Short.BYTES * n, buffer -> processShorts(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code short}s.
     * @param n          The amount of {@code short}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code short}s are received.
     */
    private void processShorts(ByteBuffer buffer, int n, Consumer<short[]> consumer) {
        var s = new short[n];
        buffer.asShortBuffer().get(s);
        consumer.accept(s);
    }
    
}
