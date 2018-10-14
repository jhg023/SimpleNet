package simplenet.channel;

import java.io.IOException;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.Channel;
import simplenet.receiver.Receiver;

/**
 * An {@code interface} that denotes an entity as having a backing {@link Channel}.
 *
 * @author Jacob G.
 * @since November 6, 2017
 */
@FunctionalInterface
public interface Channeled<T extends AsynchronousChannel> {

    /**
     * Gets the backing {@link Channel} of this entity.
     *
     * @return An {@link T}.
     */
    T getChannel();

    /**
     * Closes the backing {@link Channel} of this {@link Receiver},
     * which results in the firing of any disconnect-listeners that exist.
     */
    default void close() {
        try {
            getChannel().close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to close the channel!", e);
        }
    }

}
