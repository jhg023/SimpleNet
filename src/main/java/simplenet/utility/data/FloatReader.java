package simplenet.utility.data;

import bitbuffer.BitBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import simplenet.utility.exposed.FloatConsumer;

/**
 * An interface that defines the methods required to read {@code float}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface FloatReader extends DataReader {
    
    /**
     * Reads a {@code float} with {@link ByteOrder#LITTLE_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readFloat(FloatConsumer)}.
     *
     * @return A {@code float}.
     * @see #readFloat(ByteOrder)
     */
    default float readFloat() {
        return readFloat(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Reads a {@code float} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readFloat(FloatConsumer)}.
     *
     * @return A {@code float}.
     */
    default float readFloat(ByteOrder order) {
        var future = new CompletableFuture<Float>();
        readFloat(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readFloat(FloatConsumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @see #readFloat(FloatConsumer, ByteOrder)
     */
    default void readFloat(FloatConsumer consumer) {
        readFloat(consumer, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Requests a single {@code float}, with the specified {@link ByteOrder}, and accepts a {@link FloatConsumer} with
     * the {@code float} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @param order    The byte order of the data being received.
     */
    default void readFloat(FloatConsumer consumer, ByteOrder order) {
        read(Float.SIZE, buffer -> consumer.accept(buffer.getFloat(order)));
    }
    
    /**
     * Calls {@link #readFloatAlways(FloatConsumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @see #readFloatAlways(FloatConsumer, ByteOrder)
     */
    default void readFloatAlways(FloatConsumer consumer) {
        readFloatAlways(consumer, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Calls {@link #readFloat(FloatConsumer, ByteOrder)}; however, once finished,
     * {@link #readFloat(FloatConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readFloat(FloatConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @param order    The byte order of the data being received.
     */
    default void readFloatAlways(FloatConsumer consumer, ByteOrder order) {
        readAlways(Float.SIZE, buffer -> consumer.accept(buffer.getFloat(order)));
    }
    
    /**
     * Calls {@link #readFloats(int, Consumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @see #readFloats(int, Consumer, ByteOrder)
     */
    default void readFloats(int n, Consumer<float[]> consumer) {
        readFloats(n, consumer, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Requests a {@code float[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code float}s are received.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readFloats(int n, Consumer<float[]> consumer, ByteOrder order) {
        read(Float.SIZE * n, buffer -> processFloats(buffer, n, consumer, order));
    }
    
    /**
     * Calls {@link #readFloatsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     */
    default void readFloatsAlways(int n, Consumer<float[]> consumer) {
        readFloatsAlways(n, consumer, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Calls {@link #readFloats(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readFloats(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readFloats(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readFloatsAlways(int n, Consumer<float[]> consumer, ByteOrder order) {
        readAlways(Float.SIZE * n, buffer -> processFloats(buffer, n, consumer, order));
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link BitBuffer} that contains the bits needed to map to {@code float}s.
     * @param n          The amount of {@code float}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @param order      The byte order of the {@code float}s being received.
     */
    private void processFloats(BitBuffer buffer, int n, Consumer<float[]> consumer, ByteOrder order) {
        var f = new float[n];
        
        for (int i = 0; i < n; i++) {
            f[i] = buffer.getFloat(order);
        }
        
        consumer.accept(f);
    }
    
}
