package simplenet.utility.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read data over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
@FunctionalInterface
public interface DataReader {
    
    /**
     * Calls {@link #read(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @see #read(int, Consumer, ByteOrder)
     */
    default void read(int n, Consumer<ByteBuffer> consumer) {
        read(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests {@code n} bytes and accepts a {@link Consumer} holding the {@code n} bytes (with the specified
     * {@link ByteOrder}) in a {@link ByteBuffer} once received.
     * <br><br>
     * If the amount of {@code byte}s requested already reside in the buffer, then this method may block to accept
     * the {@link Consumer} with the {@code byte}s. Otherwise, it simply queues up a request for the {@code byte}s,
     * which does not block.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The order of the bytes inside the {@link ByteBuffer}.
     */
    void read(int n, Consumer<ByteBuffer> consumer, ByteOrder order);
    
    /**
     * Calls {@link #readAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @see #readAlways(int, Consumer, ByteOrder)
     */
    default void readAlways(int n, Consumer<ByteBuffer> consumer) {
        readAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #read(int, Consumer, ByteOrder)}, however once finished, {@link #read(int, Consumer, ByteOrder)} is
     * called once again with the same parameters; this loops indefinitely, whereas
     * {@link #read(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The order of the bytes inside the {@link ByteBuffer}.
     */
    default void readAlways(int n, Consumer<ByteBuffer> consumer, ByteOrder order) {
        read(n, new Consumer<>() {
            @Override
            public void accept(ByteBuffer buffer) {
                consumer.accept(buffer);
                read(n, this, order);
            }
        }, order);
    }
    
}
