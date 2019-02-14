package simplenet.utility.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
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
     * Reads a {@code byte} from the network, but blocks the executing thread unlike {@link #readByte(ByteConsumer)}.
     *
     * @return A {@code byte}.
     */
    default byte readByte() {
        var future = new CompletableFuture<Byte>();
        readByte(future::complete);
        return read(future);
    }
    
    /**
     * Requests a single {@code byte} and accepts a {@link ByteConsumer} with the {@code byte} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     */
    default void readByte(ByteConsumer consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get()), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readByte(ByteConsumer)}; however, once finished, {@link #readByte(ByteConsumer)} is called once
     * again with the same consumer; this method loops indefinitely, whereas {@link #readByte(ByteConsumer)}
     * completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     */
    default void readByteAlways(ByteConsumer consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get()), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code byte[]} of length {@code n} and accepts a {@link Consumer} when all of the {@code byte}s are
     * received.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     */
    default void readBytes(int n, Consumer<byte[]> consumer) {
        read(Byte.BYTES * n, buffer -> processBytes(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readBytes(int, Consumer)}; however, once finished, {@link #readBytes(int, Consumer)} is called
     * once again with the same parameter; this loops indefinitely, whereas {@link #readBytes(int, Consumer)}
     * completes after a single iteration.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     */
    default void readBytesAlways(int n, Consumer<byte[]> consumer) {
        readAlways(Byte.BYTES * n, buffer -> processBytes(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed.
     * @param n          The amount of bytes requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} bytes are received.
     */
    private void processBytes(ByteBuffer buffer, int n, Consumer<byte[]> consumer) {
        var b = new byte[n];
        buffer.get(b);
        consumer.accept(b);
    }
    
}
