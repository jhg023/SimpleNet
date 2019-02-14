package simplenet.utility.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.crypto.Cipher;

/**
 * An interface that defines the methods required to read data over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
@FunctionalInterface
public interface DataReader {
    
    /**
     * A helper method to block until the {@link CompletableFuture} contains a value.
     *
     * @param future The {@link CompletableFuture} to wait for.
     * @param <T>    The type of the {@link CompletableFuture} and the return type.
     * @return The instance of {@code T} contained in the {@link CompletableFuture}.
     */
    default <T> T read(CompletableFuture<T> future) {
        return future.join();
    }
    
    /**
     * Requests {@code n} bytes and accepts a {@link Consumer} with them (in a {@link ByteBuffer}) once received.
     * <br><br>
     * If the amount of bytes requested already reside in the buffer, then this method may block to accept the
     * {@link Consumer} with the bytes. Otherwise, it simply queues up a request for the bytes, which does not block.
     * <br><br>
     * If encryption is active with a {@link Cipher} that uses padding, then this method should <strong>not</strong> be
     * called directly, as each grouping of bytes ({@code byte}, {@code short}, {@code int}, etc.) is encrypted
     * separately and will most-likely not reflect the amount of bytes requested.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The byte order of the data being received.
     */
    void read(int n, Consumer<ByteBuffer> consumer, ByteOrder order);
    
    /**
     * Calls {@link #read(int, Consumer, ByteOrder)}, however once finished, {@link #read(int, Consumer, ByteOrder)} is
     * called once again with the same parameters; this loops indefinitely, whereas
     * {@link #read(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The byte order of the data being received.
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
