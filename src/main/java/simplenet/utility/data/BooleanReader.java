package simplenet.utility.data;

import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import simplenet.utility.exposed.BooleanConsumer;

/**
 * An interface that defines the methods required to read {@code boolean}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface BooleanReader extends DataReader {
    
    /**
     * Reads a {@code boolean} with {@link ByteOrder#BIG_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readBoolean(BooleanConsumer)}.
     *
     * @return A {@code boolean}.
     * @see #readBoolean(ByteOrder)
     */
    default boolean readBoolean() {
        return readBoolean(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Reads a {@code boolean} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readBoolean(BooleanConsumer)}.
     *
     * @return A {@code boolean}.
     */
    default boolean readBoolean(ByteOrder order) {
        var future = new CompletableFuture<Boolean>();
        readBoolean(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readBoolean(BooleanConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     * @see #readBoolean(BooleanConsumer, ByteOrder)
     */
    default void readBoolean(BooleanConsumer consumer) {
        readBoolean(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code boolean}, with the specified {@link ByteOrder}, and accepts a {@link BooleanConsumer}
     * with the {@code boolean} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     * @param order    The byte order of the data being received.
     */
    default void readBoolean(BooleanConsumer consumer, ByteOrder order) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get() != 0), order);
    }
    
    /**
     * Calls {@link #readBooleanAlways(BooleanConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     * @see #readBooleanAlways(BooleanConsumer, ByteOrder)
     */
    default void readBooleanAlways(BooleanConsumer consumer) {
        readBooleanAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readBoolean(BooleanConsumer, ByteOrder)}; however, once finished,
     * {@link #readBoolean(BooleanConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readBoolean(BooleanConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     * @param order    The byte order of the data being received.
     */
    default void readBooleanAlways(BooleanConsumer consumer, ByteOrder order) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get() != 0), order);
    }
    
    /**
     * Calls {@link #readBooleans(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     * @see #readBooleans(int, Consumer, ByteOrder)
     */
    default void readBooleans(int n, Consumer<boolean[]> consumer) {
        readBooleans(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code boolean[]} of length {@code n} in the specified {@link ByteOrder} and accepts a
     * {@link Consumer} when all of the {@code boolean}s are received.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readBooleans(int n, Consumer<boolean[]> consumer, ByteOrder order) {
        read(n, buffer -> {
            var b = new boolean[n];
            
            for (int i = 0; i < n; i++) {
                b[i] = buffer.get() != 0;
            }
            
            consumer.accept(b);
        }, order);
    }
    
    /**
     * Calls {@link #readBooleansAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */
    default void readBooleansAlways(int n, Consumer<boolean[]> consumer) {
        readBooleansAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readBooleans(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readBooleans(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readBooleans(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readBooleansAlways(int n, Consumer<boolean[]> consumer, ByteOrder order) {
        readAlways(n, buffer -> {
            var b = new boolean[n];
    
            for (int i = 0; i < n; i++) {
                b[i] = buffer.get() != 0;
            }
    
            consumer.accept(b);
        }, order);
    }
    
}
