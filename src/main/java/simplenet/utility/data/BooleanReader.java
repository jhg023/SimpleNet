package simplenet.utility.data;

import bitbuffer.BitBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import simplenet.utility.exposed.BooleanConsumer;

/**
 * An interface that defines the methods required to read {@code boolean}s over a network with SimpleNet.
 * <br><br>
 * Uncompressed {@code boolean}s are sent over the network as {@code byte}s with a value of {@code 1} for {@code true}
 * and a value of {@code 0} for {@code false}, whereas compressed booleans are stored in the backing {@link BitBuffer}
 * as a single bit ({@code 1} for {@code true} and {@code 0} for {@code false}).
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface BooleanReader extends DataReader {
    
    /**
     * Reads a {@code boolean} from the network, but blocks the executing thread unlike
     * {@link #readBoolean(BooleanConsumer)}.
     *
     * @return A {@code boolean}.
     * @see #readBoolean(BooleanConsumer)
     */
    default boolean readBoolean() {
        var future = new CompletableFuture<Boolean>();
        readBoolean(future::complete);
        return read(future);
    }
    
    /**
     * Reads a compressed {@code boolean} from the network, but blocks the executing thread unlike
     * {@link #readCompressedBoolean(BooleanConsumer)}.
     *
     * @return A {@code boolean}.
     * @see #readCompressedBoolean(BooleanConsumer)
     */
    default boolean readCompressedBoolean() {
        var future = new CompletableFuture<Boolean>();
        readCompressedBoolean(future::complete);
        return read(future);
    }
    
    /**
     * Requests a single {@code boolean}, and accepts a {@link BooleanConsumer} with the {@code boolean} when it is
     * received.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readBoolean(BooleanConsumer consumer) {
        read(Byte.SIZE, buffer -> consumer.accept(buffer.getBoolean(false)));
    }
    
    /**
     * Requests a single compressed {@code boolean}, and accepts a {@link BooleanConsumer} with the {@code boolean}
     * when it is received.
     * <br><br>
     * A compressed {@code boolean} is stored in the buffer as a single bit rather than as a {@code byte}.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readCompressedBoolean(BooleanConsumer consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.getBoolean(true)));
    }
    
    /**
     * Calls {@link #readBoolean(BooleanConsumer)}; however, once finished, {@link #readBoolean(BooleanConsumer)} is
     * called once again with the same consumer; this method loops indefinitely, whereas
     * {@link #readBoolean(BooleanConsumer)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readBooleanAlways(BooleanConsumer consumer) {
        readAlways(Byte.SIZE, buffer -> consumer.accept(buffer.getBoolean(false)));
    }
    
    /**
     * Calls {@link #readCompressedBoolean(BooleanConsumer)}; however, once finished,
     * {@link #readCompressedBoolean(BooleanConsumer)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readCompressedBoolean(BooleanConsumer)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readCompressedBooleanAlways(BooleanConsumer consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.getBoolean(true)));
    }
    
    /**
     * Requests a {@code boolean[]} of length {@code n} and accepts a {@link Consumer} when all of the
     * {@code boolean}s are received.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */
    default void readBooleans(int n, Consumer<boolean[]> consumer) {
        read(Byte.SIZE * n, buffer -> processBooleans(buffer, n, consumer, false));
    }
    
    /**
     * Requests a {@code boolean[]} of length {@code n} and accepts a {@link Consumer} when all of the compressed
     * {@code boolean}s are received.
     *
     * @param n        The amount of compressed {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */
    default void readCompressedBooleans(int n, Consumer<boolean[]> consumer) {
        read(Byte.BYTES * n, buffer -> processBooleans(buffer, n, consumer, true));
    }
    
    /**
     * Calls {@link #readBooleans(int, Consumer)}; however, once finished, {@link #readBooleans(int, Consumer)} is
     * called once again with the same parameter; this loops indefinitely, whereas
     * {@link #readBooleans(int, Consumer)} completes after a single iteration.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} compressed {@code boolean}s are
     *                 received.
     */
    default void readBooleansAlways(int n, Consumer<boolean[]> consumer) {
        readAlways(Byte.SIZE * n, buffer -> processBooleans(buffer, n, consumer, false));
    }
    
    /**
     * Calls {@link #readCompressedBooleans(int, Consumer)}; however, once finished,
     * {@link #readCompressedBooleans(int, Consumer)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readCompressedBooleans(int, Consumer)} completes after a single iteration.
     *
     * @param n        The amount of compressed {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} compressed {@code boolean}s are
     *                 received.
     */
    default void readCompressedBooleansAlways(int n, Consumer<boolean[]> consumer) {
        readAlways(Byte.BYTES * n, buffer -> processBooleans(buffer, n, consumer, true));
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link BitBuffer} that contains the bits needed to map to {@code boolean}s.
     * @param n          The amount of possibly-compressed {@code boolean}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     * @param compressed Whether or not the requested {@code boolean}s are compressed.
     */
    private void processBooleans(BitBuffer buffer, int n, Consumer<boolean[]> consumer, boolean compressed) {
        var b = new boolean[n];
    
        for (int i = 0; i < n; i++) {
            b[i] = buffer.getBoolean(compressed);
        }
    
        consumer.accept(b);
    }
    
}
