package simplenet;

import simplenet.channel.Channeled;
import simplenet.client.Client;
import simplenet.server.Server;
import simplenet.utility.IntPair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class Receiver implements Channeled {

    /**
     * The size of this {@link Receiver}'s buffer.
     */
    private final int bufferSize;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    private final Collection<Consumer<Client>> connectListeners;

    /**
     * Listeners that are fired when a {@link Client} disconnects
     * to a {@link Server}.
     */
    private final Collection<Consumer<Client>> disconnectListeners;

    /**
     * Instantiates a new {@link Receiver} with a buffer capacity
     * of {@code bufferSize}.
     *
     * @param bufferSize
     *      The capacity of the buffer used for reading.
     */
    protected Receiver(int bufferSize) {
        this.bufferSize = bufferSize;

        connectListeners = new ArrayList<>();
        disconnectListeners = new ArrayList<>();
    }

    /**
     * Closes the backing {@link Channel} of this {@link Receiver},
     * which results in the firing of any disconnect-listeners that exist.
     */
    public void close() {
        try {
            getChannel().close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to close the channel!");
        }
    }

    /**
     * Registers a listener that fires when a {@link Client} connects
     * to a {@link Server}.
     * <p>
     * This listener is able to be used by both the {@link Client}
     * and {@link Server}, but can be independent of one-another.
     * <p>
     * When calling this method more than once, multiple listeners
     * are registered.
     *
     * @param consumer
     *      A {@link Consumer}.
     */
    public void onConnect(Consumer<Client> consumer) {
        connectListeners.add(consumer);
    }

    /**
     * Registers a listener that fires when a {@link Client}
     * disconnects from a {@link Server}.
     * <p>
     * This listener is able to be used by both the {@link Client}
     * and {@link Server}, but can be independent of one-another.
     * <p>
     * When calling this method more than once, multiple listeners
     * are registered.
     *
     * @param consumer
     *      A {@link Consumer}.
     */
    public void onDisconnect(Consumer<Client> consumer) {
        disconnectListeners.add(consumer);
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} connects to a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<Consumer<Client>> getConnectionListeners() {
        return connectListeners;
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} disconnects from a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<Consumer<Client>> getDisconnectListeners() {
        return disconnectListeners;
    }

    /**
     * The size of this {@link Receiver}'s buffer.
     *
     * @return
     *      An {@code int}.
     */
    public int getBufferSize() {
        return bufferSize;
    }

}
