package simplenet.utility.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * An interface that defines the methods required to read {@code int}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface IntReader extends DataReader {
    
    /**
     * Reads an {@code int} with {@link ByteOrder#BIG_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readInt(IntConsumer)}.
     *
     * @return An {@code int}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     * @see #readInt(ByteOrder)
     */
    default int readInt() throws IllegalStateException {
        return readInt(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Reads an {@code int} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readInt(IntConsumer)}.
     *
     * @return An {@code int}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     */
    default int readInt(ByteOrder order) throws IllegalStateException {
        blockingInsideCallback();
        var future = new CompletableFuture<Integer>();
        readInt(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readInt(IntConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @see #readInt(IntConsumer, ByteOrder)
     */
    default void readInt(IntConsumer consumer) {
        readInt(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code int}, with the specified {@link ByteOrder}, and accepts a {@link IntConsumer} with
     * the {@code int} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @param order    The byte order of the data being received.
     */
    default void readInt(IntConsumer consumer, ByteOrder order) {
        read(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()), order);
    }
    
    /**
     * Calls {@link #readIntAlways(IntConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @see #readIntAlways(IntConsumer, ByteOrder)
     */
    default void readIntAlways(IntConsumer consumer) {
        readIntAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readInt(IntConsumer, ByteOrder)}; however, once finished,
     * {@link #readInt(IntConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readInt(IntConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @param order    The byte order of the data being received.
     */
    default void readIntAlways(IntConsumer consumer, ByteOrder order) {
        readAlways(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()), order);
    }
    
    /**
     * Calls {@link #readInts(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @see #readInts(int, Consumer, ByteOrder)
     */
    default void readInts(int n, Consumer<int[]> consumer) {
        readInts(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code int[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code int}s are received.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readInts(int n, Consumer<int[]> consumer, ByteOrder order) {
        read(Integer.BYTES * n, buffer -> processInts(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readIntsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     */
    default void readIntsAlways(int n, Consumer<int[]> consumer) {
        readIntsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readInts(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readInts(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readInts(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readIntsAlways(int n, Consumer<int[]> consumer, ByteOrder order) {
        readAlways(Integer.BYTES * n, buffer -> processInts(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code int}s.
     * @param n          The amount of {@code int}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code int}s are received.
     */
    private void processInts(ByteBuffer buffer, int n, Consumer<int[]> consumer) {
        var i = new int[n];
        buffer.asIntBuffer().get(i);
        consumer.accept(i);
    }
    
}
