package simplenet.utility.data;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@link String}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface StringReader extends ShortReader {
    
    /**
     * Reads a {@link String} with {@link StandardCharsets#UTF_8} as the encoding and {@link ByteOrder#LITTLE_ENDIAN} as
     * the {@code order}, but blocks the executing thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     * @see #readString(Charset)
     */
    default String readString() {
        return readString(StandardCharsets.UTF_8);
    }
    
    /**
     * Reads a {@link String} with the specified {@link Charset} and {@link ByteOrder#LITTLE_ENDIAN} as the
     * {@code order}, but blocks the executing thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     * @see #readString(Charset, ByteOrder)
     */
    default String readString(Charset charset) {
        return readString(charset, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Reads a {@link String} with the specified {@link Charset} and {@link ByteOrder}, but blocks the executing
     * thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     */
    default String readString(Charset charset, ByteOrder order) {
        var future = new CompletableFuture<String>();
        readString(future::complete, charset, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)} with {@link StandardCharsets#UTF_8} as the encoding and
     * {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readString(Consumer, Charset, ByteOrder)
     */
    default void readString(Consumer<String> consumer) {
        readString(consumer, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)} with the specified {@link Charset} as the encoding and
     * {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readString(Consumer, Charset, ByteOrder)
     */
    default void readString(Consumer<String> consumer, Charset charset) {
        readString(consumer, charset, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Requests a single {@link String}, with the specified {@link Charset} and {@link ByteOrder}, and accepts a
     * {@link Consumer} with the {@link String} when it is received.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} (or less) with a different encoding.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @param charset  The {@link Charset} encoding of the {@link String}.
     * @param order    The byte order of the data being received.
     */
    default void readString(Consumer<String> consumer, Charset charset, ByteOrder order) {
        readShort(length -> {
            read(Byte.SIZE * length, buffer -> {
                consumer.accept(new String(buffer.getBytes(length & 0xFFFF), charset));
            });
        }, order);
    }
    
    /**
     * Calls {@link #readStringAlways(Consumer, Charset, ByteOrder)} with {@link StandardCharsets#UTF_8} as the encoding
     * and {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readStringAlways(Consumer, Charset, ByteOrder)
     */
    default void readStringAlways(Consumer<String> consumer) {
        readStringAlways(consumer, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Calls {@link #readStringAlways(Consumer, Charset, ByteOrder)} with the specified {@link Charset} as the encoding
     * and {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readStringAlways(Consumer, Charset, ByteOrder)
     */
    default void readStringAlways(Consumer<String> consumer, Charset charset) {
        readStringAlways(consumer, charset, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)}; however, once finished,
     * {@link #readString(Consumer, Charset, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readString(Consumer, Charset, ByteOrder)} completes after a single iteration.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} with a {@link StandardCharsets#UTF_16} encoding.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @param charset  The {@link Charset} encoding of the {@link String}.
     * @param order    The byte order of the data being received.
     */
    default void readStringAlways(Consumer<String> consumer, Charset charset, ByteOrder order) {
        readShortAlways(length -> {
            read(Byte.SIZE * length, buffer -> {
                consumer.accept(new String(buffer.getBytes(length & 0xFFFF), charset));
            });
        });
    }
    
}
