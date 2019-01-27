package simplenet.utility.data;

import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * An interface that defines the methods required to read {@code double}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface DoubleReader extends DataReader {
    
    /**
     * Reads a {@code double} with {@link ByteOrder#BIG_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readDouble(DoubleConsumer)}.
     *
     * @return A {@code double}.
     * @see #readDouble(ByteOrder)
     */
    default double readDouble() {
        return readDouble(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Reads a {@code double} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readDouble(DoubleConsumer)}.
     *
     * @return A {@code double}.
     */
    default double readDouble(ByteOrder order) {
        var future = new CompletableFuture<Double>();
        readDouble(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readDouble(DoubleConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code double} is received.
     * @see #readDouble(DoubleConsumer, ByteOrder)
     */
    default void readDouble(DoubleConsumer consumer) {
        readDouble(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code double}, with the specified {@link ByteOrder}, and accepts a {@link DoubleConsumer} with
     * the {@code double} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code double} is received.
     * @param order    The byte order of the data being received.
     */
    default void readDouble(DoubleConsumer consumer, ByteOrder order) {
        read(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()), order);
    }
    
    /**
     * Calls {@link #readDoubleAlways(DoubleConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code double} is received.
     * @see #readDoubleAlways(DoubleConsumer, ByteOrder)
     */
    default void readDoubleAlways(DoubleConsumer consumer) {
        readDoubleAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readDouble(DoubleConsumer, ByteOrder)}; however, once finished,
     * {@link #readDouble(DoubleConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readDouble(DoubleConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code double} is received.
     * @param order    The byte order of the data being received.
     */
    default void readDoubleAlways(DoubleConsumer consumer, ByteOrder order) {
        readAlways(Double.BYTES, buffer -> consumer.accept(buffer.getDouble()), order);
    }
    
    /**
     * Calls {@link #readDoubles(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code double}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code double}s are received.
     * @see #readDoubles(int, Consumer, ByteOrder)
     */
    default void readDoubles(int n, Consumer<double[]> consumer) {
        readDoubles(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code double[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code double}s are received.
     *
     * @param n        The amount of {@code double}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code double}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readDoubles(int n, Consumer<double[]> consumer, ByteOrder order) {
        read(n, buffer -> {
            var d = new double[n];
            buffer.asDoubleBuffer().get(d);
            consumer.accept(d);
        }, order);
    }
    
    /**
     * Calls {@link #readDoublesAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code double}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code double}s are received.
     */
    default void readDoublesAlways(int n, Consumer<double[]> consumer) {
        readDoublesAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readDoubles(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readDoubles(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readDoubles(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code double}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code double}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readDoublesAlways(int n, Consumer<double[]> consumer, ByteOrder order) {
        readAlways(n, buffer -> {
            var d = new double[n];
            buffer.asDoubleBuffer().get(d);
            consumer.accept(d);
        }, order);
    }
    
}
