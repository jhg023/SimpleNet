package simplenet                                                           ;

import simplenet.channel.Channeled                                          ;
import simplenet.client.Client                                              ;
import simplenet.server.Server                                              ;
import simplenet.utility.IntPair                                            ;

import java.io.IOException                                                  ;
import java.nio.ByteBuffer                                                  ;
import java.nio.channels.Channel                                            ;
import java.util.*                                                          ;
import java.util.function.Consumer                                          ;

public abstract class Receiver<T> implements Channeled                      {

    /**
     * The size of this {@link Receiver}'s buffer.
     */
    protected final int bufferSize                                          ;

    /**
     * The {@link Deque} that keeps track of nested calls
     * to {@link Client#read(int, Consumer)} and assures that they
     * will complete in the expected order.
     */
    protected final Deque<IntPair<Consumer<ByteBuffer>>> stack              ;

    /**
     * The {@link Deque} used when requesting a certain
     * amount of bytes from the {@link Client} or {@link Server}.
     */
    protected final Deque<IntPair<Consumer<ByteBuffer>>> queue              ;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    private final Collection<T> connectListeners                            ;

    /**
     * Listeners that are fired when a {@link Client} disconnects
     * to a {@link Server}.
     */
    private final Collection<T> disconnectListeners                         ;

    /**
     * Instantiates a new {@link Receiver} with a buffer capacity
     * of {@code bufferSize}.
     *
     * @param bufferSize
     *      The capacity of the buffer used for reading.
     */
    protected Receiver(int bufferSize)                                      {
        this.bufferSize = bufferSize                                        ;

        queue = new ArrayDeque<>()                                          ;
        stack = new ArrayDeque<>()                                          ;
        connectListeners = new ArrayList<>()                                ;
        disconnectListeners = new ArrayList<>()                             ;}

    /**
     * Closes the backing {@link Channel} of this {@link Receiver},
     * which results in the firing of any disconnect-listeners that exist.
     */
    public void close()                                                     {
        try                                                                 {
            getChannel().close()                                            ;}
        catch (IOException e)                                             {
            throw new IllegalStateException("Unable to close the channel!") ;}}

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
     * @param listener
     *      A {@link T}.
     */
    public void onConnect(T listener)                                       {
        connectListeners.add(listener)                                      ;}

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
     * @param listener
     *      A {@link T}.
     */
    public void onDisconnect(T listener)                                    {
        disconnectListeners.add(listener)                                   ;}

    /**
     * Gets the {@link Deque} that holds information
     * regarding requested bytes by this {@link Client}.
     *
     * @return
     *      A {@link Deque}.
     */
    public Deque<IntPair<Consumer<ByteBuffer>>> getQueue()                  {
        return queue                                                        ;}

    /**
     * Gets the {@link Deque} that keeps track of nested
     * calls to {@link Client#read(int, Consumer)}.
     *
     * @return
     *      A {@link Deque}.
     */
    public Deque<IntPair<Consumer<ByteBuffer>>> getStack()                  {
        return stack                                                        ;}

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} connects to a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<T> getConnectionListeners()                           {
        return connectListeners                                             ;}

    /**
     * Gets a {@link Collection} of listeners that are fired when a
     * {@link Client} disconnects from a {@link Server}.
     *
     * @return
     *      A {@link Collection}.
     */
    public Collection<T> getDisconnectListeners()                           {
        return disconnectListeners                                          ;}}