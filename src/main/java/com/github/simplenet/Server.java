/*
 * MIT License
 *
 * Copyright (c) 2020 Jacob Glickman
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
package com.github.simplenet;

import com.github.simplenet.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Server extends AbstractReceiver<Consumer<Client>> implements Channeled<AsynchronousServerSocketChannel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     * A thread-safe {@link Set} that keeps track of {@link Client}s connected to this {@link Server}.
     */
    private final Set<Client> connectedClients;

    /**
     * The backing {@link AsynchronousChannelGroup} of this {@link Server}.
     */
    private AsynchronousChannelGroup group;
    
    /**
     * The backing {@link Channel} of this {@link Server}.
     */
    private AsynchronousServerSocketChannel channel;

    /**
     * Instantiates a new {@link Server}.
     */
    public Server() {
        this.connectedClients = ConcurrentHashMap.newKeySet();
    }

    /**
     * Attempts to bind the {@link Server} to the specified {@code address} and {@code port}.
     * <br><br>
     * The number of threads used by the backing {@link ThreadPoolExecutor} is equal to the larger of {@code 2} and
     * {@code Runtime.getRuntime().availableProcessors() - 2}.
     *
     * @param address The IP address to bind to, whose value can also be {@code "localhost"}.
     * @param port    The port to bind to, which must be in the range: {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException If {@code port} is less than 0 or greater than 65535.
     * @throws IllegalStateException    If this server is already running on any address/port.
     * @throws IllegalStateException    If the server is unable to bind to the specified address or port.
     * @see #bind(String, int, int)
     */
    public void bind(String address, int port) {
        bind(address, port, Math.max(2, Runtime.getRuntime().availableProcessors() - 2));
    }

    /**
     * Attempts to bind the {@link Server} to the specified {@code address} and {@code port}.
     * <br><br>
     * The number of threads used by the backing {@link ThreadPoolExecutor} is specified by {@code numThreads}.
     *
     * @param address    The IP address to bind to, whose value can also be {@code "localhost"}.
     * @param port       The port to bind to, which must be in the range: {@code 0 <= port <= 65535}.
     * @param numThreads The number of threads to use in the backing {@link ThreadPoolExecutor}.
     * @throws IllegalArgumentException If {@code port} is less than 0 or greater than 65535.
     * @throws IllegalStateException    If this server is already running on any address/port.
     * @throws IllegalStateException    If the server is unable to bind to the specified address or port.
     */
    public void bind(String address, int port, int numThreads) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.setName(thread.getName().replace("Thread", "SimpleNet"));
            return thread;
        }, (runnable, threadPoolExecutor) -> {});

        // Start one core thread in advance to prevent the JVM from shutting down.
        executor.prestartCoreThread();

        try {
            this.channel = AsynchronousServerSocketChannel.open(group = AsynchronousChannelGroup.withThreadPool(executor));
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the AsynchronousServerSocketChannel!", e);
        }

        try {
            channel.bind(new InetSocketAddress(address, port));
            channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Void attachment) {
                    Client client = new Client(channel);
                    connectedClients.add(client);
                    client.postDisconnect(() -> connectedClients.remove(client));
                    connectListeners.forEach(consumer -> consumer.accept(client));
                    Server.this.channel.accept(null, this); // Should this be first?
                }

                @Override
                public void failed(Throwable t, Void attachment) {
                    LOGGER.debug("An exception occurred when accepting a Client!", t);
                }
            });

            LOGGER.info("Successfully bound to {}:{}!", address, port);
        } catch (AlreadyBoundException e) {
            throw new IllegalStateException("This server is already bound!", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bind the specified address and port!", e);
        }
    }
    
    /**
     * Closes this {@link Server} by first invoking {@link Client#close()} on every connected {@link Client}, and
     * then closes the backing {@link AsynchronousChannelGroup}.
     */
    @Override
    public void close() {
        connectedClients.removeIf(client -> {
            client.close();
            return true;
        });
    
        Channeled.super.close();
        
        try {
            group.shutdownNow();
        } catch (IOException e) {
            LOGGER.debug("An IOException occurred when shutting down the AsynchronousChannelGroup!", e);
        }
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
     * A helper method that eliminates code duplication in the {@link #queueToAllExcept(Packet, Client[])} and
     * {@link #queueAndFlushToAllExcept(Packet, Client[])} methods.
     *
     * @param consumer The action to perform for each {@link Client}.
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    private void queueHelper(Consumer<Client> consumer, Client... clients) {
        Set<Client> toExclude = Collections.newSetFromMap(new IdentityHashMap<>(clients.length));
        Collections.addAll(toExclude, clients);
        connectedClients.stream().filter(client -> !toExclude.contains(client)).forEach(consumer);
    }
    
    /**
     * A helper method that eliminates code duplication in the {@link #queueToAllExcept(Packet, Collection)} and
     * {@link #queueAndFlushToAllExcept(Packet, Collection)} methods.
     *
     * @param consumer The action to perform for each {@link Client}.
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    private void queueHelper(Consumer<Client> consumer, Collection<? extends Client> clients) {
        Set<Client> toExclude = Collections.newSetFromMap(new IdentityHashMap<>(clients.size()));
        toExclude.addAll(clients);
        connectedClients.stream().filter(client -> !toExclude.contains(client)).forEach(consumer);
    }
    
    /**
     * Queues a {@link Packet} to all connected {@link Client}s except the one(s) specified.
     * <br><br>
     * No {@link Client} will receive this {@link Packet} until {@link Client#flush()} is called for that respective
     * {@link Client}.
     *
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void queueToAllExcept(Packet packet, Client... clients) {
        queueHelper(packet::queue, clients);
    }
    
    /**
     * Queues a {@link Packet} to all connected {@link Client}s except the one(s) specified.
     * <br><br>
     * No {@link Client} will receive this {@link Packet} until {@link Client#flush()} is called for that respective
     * {@link Client}.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void queueToAllExcept(Packet packet, Collection<? extends Client> clients) {
        queueHelper(packet::queue, clients);
    }
    
    /**
     * Flushes all queued {@link Packet}s for all {@link Client}s except the one(s) specified.
     *
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void flushToAllExcept(Client... clients) {
        queueHelper(Client::flush, clients);
    }
    
    /**
     * Flushes all queued {@link Packet}s for all {@link Client}s except the one(s) specified.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from having their queued packets flushed.
     */
    public final void flushToAllExcept(Collection<? extends Client> clients) {
        queueHelper(Client::flush, clients);
    }
    
    /**
     * Queues a {@link Packet} to a one or more {@link Client}s and calls {@link Client#flush()}, flushing all
     * previously-queued packets as well.
     *
     * @param clients A variable amount of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void queueAndFlushToAllExcept(Packet packet, Client... clients) {
        queueHelper(packet::queueAndFlush, clients);
    }
    
    /**
     * Queues a {@link Packet} to a one or more {@link Client}s and calls {@link Client#flush()}, flushing all
     * previously-queued packets as well.
     *
     * @param clients A {@link Collection} of {@link Client}s to exclude from receiving the {@link Packet}.
     */
    public final void queueAndFlushToAllExcept(Packet packet, Collection<? extends Client> clients) {
        queueHelper(packet::queueAndFlush, clients);
    }
}
