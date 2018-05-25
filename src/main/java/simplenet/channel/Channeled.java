package simplenet.channel;

import java.nio.channels.AsynchronousChannel;
import java.nio.channels.Channel;

/**
 * An {@code interface} that denotes an entity as
 * having a backing {@link Channel}.
 *
 * @author Jacob G.
 * @since November 6, 2017
 */
@FunctionalInterface
public interface Channeled {

    /**
     * Gets the backing {@link Channel} of this entity.
     *
     * @return An {@link AsynchronousChannel}.
     */
    AsynchronousChannel getChannel();

}
