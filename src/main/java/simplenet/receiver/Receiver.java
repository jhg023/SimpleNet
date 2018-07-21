package simplenet.receiver;

import java.util.ArrayList;
import java.util.Collection;
import simplenet.Client;
import simplenet.Server;

public abstract class Receiver<T> {

    /**
     * The size of this {@link Receiver}'s buffer.
     */
    protected final int bufferSize;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    protected final Collection<T> connectListeners;

    /**
     * Listeners that are fired when a {@link Client} disconnects
     * to a {@link Server}.
     */
    protected final Collection<T> disconnectListeners;

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

}
