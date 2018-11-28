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
     * Listeners that are fired right before a {@link Client} disconnects
     * from a {@link Server}.
     */
    protected final Collection<T> preDisconnectListeners;

    /**
     * Listeners that are fired right after a {@link Client} disconnects
     * from a {@link Server}.
     */
    protected final Collection<T> postDisconnectListeners;

    /**
     * Instantiates a new {@link Receiver} with a buffer capacity of {@code bufferSize}.
     *
     * @param bufferSize The capacity of the buffer used for reading in bytes.
     */
    protected Receiver(int bufferSize) {
        this.bufferSize = bufferSize;
        this.connectListeners = new ArrayList<>();
        this.preDisconnectListeners = new ArrayList<>();
        this.postDisconnectListeners = new ArrayList<>();
    }

    /**
     * A copy-constructor to create a {@link Receiver}.
     *
     * @param receiver Another {@link Receiver}.
     */
    protected Receiver(Receiver<T> receiver) {
        this.bufferSize = receiver.bufferSize;
        this.connectListeners = receiver.connectListeners;
        this.preDisconnectListeners = receiver.preDisconnectListeners;
        this.postDisconnectListeners = receiver.postDisconnectListeners;
    }

    /**
     * Registers a listener that fires when a {@link Client} connects to a {@link Server}.
     * <br><br>
     * When calling this method more than once, multiple listeners are registered.
     *
     * @param listener A {@link T}.
     */
    public void onConnect(T listener) {
        connectListeners.add(listener);
    }

    /**
     * Gets the buffer size of this {@link Receiver} in bytes.
     *
     * @return An {@code int}
     */
    public int getBufferSize() {
        return bufferSize;
    }

}
