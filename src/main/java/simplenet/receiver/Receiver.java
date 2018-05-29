package simplenet.receiver;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Collection;
import simplenet.Client;
import simplenet.Server;
import simplenet.channel.Channeled;

public abstract class Receiver<T> implements Channeled {

    /**
     * The size of this {@link Receiver}'s buffer.
     */
    protected final int bufferSize;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    private final Collection<T> connectListeners;

    /**
     * Listeners that are fired when a {@link Client} disconnects
     * to a {@link Server}.
     */
    private final Collection<T> disconnectListeners;

    /**
     * Instantiates a new {@link Receiver} with a buffer capacity
     * of {@code bufferSize}.
     *
     * @param bufferSize The capacity of the buffer used for reading.
     */
    protected Receiver(int bufferSize) {
        this.bufferSize = bufferSize;

        connectListeners = new ArrayList<>();
        disconnectListeners = new ArrayList<>();
    }

    protected Receiver(Receiver<T> receiver) {
        this.bufferSize = receiver.bufferSize;
        this.connectListeners = receiver.connectListeners;
        this.disconnectListeners = receiver.disconnectListeners;
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
     * @param listener A {@link T}.
     */
    public void onConnect(T listener) {
        connectListeners.add(listener);
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
     * @param listener A {@link T}.
     */
    public void onDisconnect(T listener) {
        disconnectListeners.add(listener);
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} connects to a {@link Server}.
     *
     * @return A {@link Collection}.
     */
    protected Collection<T> getConnectionListeners() {
        return connectListeners;
    }

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} disconnects from a {@link Server}.
     *
     * @return A {@link Collection}.
     */
    protected Collection<T> getDisconnectListeners() {
        return disconnectListeners;
    }

}
