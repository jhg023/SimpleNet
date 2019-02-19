/*
 * MIT License
 *
 * Copyright (c) 2019 Jacob Glickman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package simplenet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import simplenet.channel.Channeled;
import simplenet.packet.Packet;
import simplenet.receiver.Receiver;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public final class Server extends Receiver<Consumer<Client>> implements Channeled<AsynchronousServerSocketChannel> {
    
    /**
     * A thread-safe {@link Set} that keeps track of {@link Client}s connected to this {@link Server}.
     */
    final Set<Client> connectedClients;
    
    /**
     * The backing {@link ThreadPoolExecutor} used for I/O.
     */
    private final ThreadPoolExecutor executor;

    /**
     * The backing {@link Channel} of this {@link Server}.
     */
    private final AsynchronousServerSocketChannel channel;
    
    /**
     * Instantiates a new {@link Server} (with a buffer size of {@code 4,096} bytes) by attempting to open the
     * backing {@link AsynchronousServerSocketChannel}.
     *
     * @throws IllegalStateException If multiple {@link Server} instances are created.
     * @see #Server(int)
     */
    public Server() throws IllegalStateException {
        this(4_096);
    }
    
    /**
     * Instantiates a new {@link Server} (with the specified buffer size) by attempting to open the backing
     * {@link AsynchronousServerSocketChannel}.
     * <br><br>
     * The number of threads used by the backing {@link ThreadPoolExecutor} is equal to the larger of {@code 1} and
     * {@code Runtime.getRuntime().availableProcessors() - 1}.
     *
     * @param bufferSize the size of the buffer, in {@code byte}s.
     * @throws IllegalStateException If multiple {@link Server} instances are created.
     * @see #Server(int, int)
     */
    public Server(int bufferSize) throws IllegalStateException {
        this(bufferSize, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }
    
    /**
     * Instantiates a new {@link Server} (with the specified buffer size and number of threads) by attempting to open
     * the backing {@link AsynchronousServerSocketChannel}.
     *
     * @param bufferSize the size of the buffer, in {@code byte}s.
     * @param numThreads the number of threads to use in the backing {@link ThreadPoolExecutor}.
     * @throws IllegalStateException If multiple {@link Server} instances are created.
     */
    public Server(int bufferSize, int numThreads) throws IllegalStateException {
        super(bufferSize);
    
        connectedClients = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
        
        try {
            executor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(false);
                thread.setName(thread.getName().replace("Thread", "SimpleNet"));
                return thread;
            });
        
            executor.prestartAllCoreThreads();
        
            channel = AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withThreadPool(executor));
            channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel:", e);
        }
    }

    /**
     * Attempts to bind the {@link Server} to a specific {@code address} and {@code port}.
     *
     * @param address The IP address to bind to.
     * @param port    The port to bind to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyBoundException    If a server is already running on any address/port.
     * @throws RuntimeException         If the server is unable to be bound to a specific address or port.
     */
    public void bind(String address, int port) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        try {
            channel.bind(new InetSocketAddress(address, port));
            channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Void attachment) {
                    var server = Server.this;
                    var client = new Client(bufferSize, channel, server);
                    connectedClients.add(client);
                    connectListeners.forEach(consumer -> consumer.accept(client));
                    server.channel.accept(null, this);
                    channel.read(client.buffer, client, Client.Listener.SERVER_INSTANCE);
                }

                @Override
                public void failed(Throwable t, Void attachment) {

                }
            });

            System.out.printf("Successfully bound to %s:%d!\n", address, port);
        } catch (AlreadyBoundException e) {
            throw new IllegalStateException("This server is already bound:", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bind the server:", e);
        }
    }
    
    /**
     * Closes this {@link Server} by closing the backing {@link AsynchronousServerSocketChannel}, shutting down the
     * backing {@link ThreadPoolExecutor}, and clearing the {@link Set} of connected {@link Client}s.
     */
    @Override
    public void close() {
        Channeled.super.close();
        executor.shutdownNow();
        connectedClients.clear();
    }

    /**
     * Gets the backing {@link Channel} of this {@link Server}.
     *
     * @return A {@link Channel}.
     */
    @Override
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }
    
    /**
     * Gets the number of {@link Client}s connected to this {@link Server}.
     *
     * @return the number of connected {@link Client}s as an {@code int}.
     */
    public int getNumConnectedClients() {
        return connectedClients.size();
    }
    
    /**
     * A helper method that eliminates code duplication in the {@link #writeToAllExcept(Packet, Client[])} and
     * {@link #writeAndFlushToAllExcept(Packet, Client[])} methods.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param consumer The action to perform for each {@link Client}.
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    @SafeVarargs
    private <T extends Client> void writeHelper(Consumer<Client> consumer, T... clients) {
        var toExclude = Collections.newSetFromMap(new IdentityHashMap<>(clients.length));
        toExclude.addAll(List.of(clients));
        connectedClients.stream().filter(Predicate.not(toExclude::contains)).forEach(consumer);
    }
    
    /**
     * A helper method that eliminates code duplication in the {@link #writeToAllExcept(Packet, Collection)} and
     * {@link #writeAndFlushToAllExcept(Packet, Collection)} methods.
     *
     * @param consumer The action to perform for each {@link Client}.
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    private void writeHelper(Consumer<Client> consumer, Collection<? extends Client> clients) {
        var toExclude = Collections.newSetFromMap(new IdentityHashMap<>(clients.size()));
        toExclude.addAll(clients);
        connectedClients.stream().filter(Predicate.not(toExclude::contains)).forEach(consumer);
    }
    
    /**
     * Queues a {@link Packet} to all connected {@link Client}s except the one(s) specified.
     * <br><br>
     * No {@link Client} will receive this {@link Packet} until {@link Client#flush()} is called for that respective
     * {@link Client}.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    @SafeVarargs
    public final <T extends Client> void writeToAllExcept(Packet packet, T... clients) {
        writeHelper(packet::write, clients);
    }
    
    /**
     * Queues a {@link Packet} to all connected {@link Client}s except the one(s) specified.
     * <br><br>
     * No {@link Client} will receive this {@link Packet} until {@link Client#flush()} is called for that respective
     * {@link Client}.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void writeToAllExcept(Packet packet, Collection<? extends Client> clients) {
        writeHelper(packet::write, clients);
    }
    
    /**
     * Flushes all queued {@link Packet}s for all {@link Client}s except the one(s) specified.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    @SafeVarargs
    public final <T extends Client> void flushToAllExcept(T... clients) {
        writeHelper(Client::flush, clients);
    }
    
    /**
     * Flushes all queued {@link Packet}s for all {@link Client}s except the one(s) specified.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from having their queued packets flushed.
     */
    public final void flushToAllExcept(Collection<? extends Client> clients) {
        writeHelper(Client::flush, clients);
    }
    
    /**
     * Queues a {@link Packet} to a one or more {@link Client}s and calls {@link Client#flush()}, flushing all
     * previously-queued packets as well.
     *
     * @param <T> A {@link Client} or any of its children.
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    @SafeVarargs
    public final <T extends Client> void writeAndFlushToAllExcept(Packet packet, T... clients) {
        writeHelper(packet::writeAndFlush, clients);
    }
    
    /**
     * Queues a {@link Packet} to a one or more {@link Client}s and calls {@link Client#flush()}, flushing all
     * previously-queued packets as well.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void writeAndFlushToAllExcept(Packet packet, Collection<? extends Client> clients) {
        writeHelper(packet::writeAndFlush, clients);
    }
    
}
