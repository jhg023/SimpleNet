/**
* Copyright (c) 2015-2019, Ralph Ellinger
* All rights reserved.
*
* Permission is hereby granted, free  of charge, to any person obtaining
* a  copy  of this  software  and  associated  documentation files  (the
* "Software"), to  deal in  the Software without  restriction, including
* without limitation  the rights to  use, copy, modify,  merge, publish,
* distribute,  sublicense, and/or sell  copies of  the Software,  and to
* permit persons to whom the Software  is furnished to do so, subject to
* the following conditions:
*
* The  above  copyright  notice  and  this permission  notice  shall  be
* included in all copies or substantial portions of the Software.
*
* THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
* EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
* MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
* LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
* OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
* WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
*
* Author: Ralph Ellinger
*
**/

package nio2.ssl;

import java.nio.channels.AsynchronousChannelGroup;
import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A grouping of ssl asynchronous channels for the purpose of resource sharing.
 *
 * <p> An ssl asynchronous channel group encapsulates the mechanics required to
 * handle the completion of I/O operations initiated by {@link nio2.ssl.SSLAsynchronousSocketChannel
 * channels} that are bound to the group. A group has an associated
 * single-threaded executor service to which tasks are submitted to handle I/O events and dispatch to
 * {@link java.nio.channels.CompletionHandler completion-handlers} that consume the result of
 * asynchronous operations performed on channels in the group.
 *
 * <p> An ssl asynchronous channel group is created by invoking one of its constructors.
 * SSLAsynchronousSocketChannel channels are bound to a group by
 * specifying the group when opening the channel. The associated executor service is <em>owned</em>
 * by the group; termination of the group results in the
 * shutdown of the associated executor service.
 *
 * <p> The completion handler for an I/O operation initiated on a
 * SSLAsynchronousSocketChannel channel bound to a group is guaranteed to be invoked by
 * its associated thread. This ensures that the completion handler is run by the thread with the
 * expected <em>identity</em>.
 *
 * <p> Where an I/O operation completes immediately, and the initiating thread
 * is the pooled thread in the group then the completion handler may
 * be invoked directly by the initiating thread.
 * <p>
 * <a id="shutdown"></a><b>Shutdown and Termination</b>
 *
 * <p> The {@link #shutdown() shutdown} method is used to initiate an <em>orderly
 * shutdown</em> of a group. An orderly shutdown marks the group as shutdown;
 * further attempts to construct a channel that binds to the group will throw
 * {@link java.nio.channels.ShutdownChannelGroupException}. Whether or not a group is shutdown can
 * be tested using the {@link #isShutdown() isShutdown} method. Once shutdown,
 * the group <em>terminates</em> when all asynchronous channels that are bound to
 * the group are closed, all actively executing completion handlers have run to
 * completion. No attempt is made to stop executing completion handlers. The
 * {@link #isTerminated() isTerminated} method is used to test if the group has
 * terminated, and the {@link #awaitTermination awaitTermination} method can be
 * used to block until the group has terminated.
 *
 * <p> The {@link #shutdownNow() shutdownNow} method can be used to initiate a
 * <em>forceful shutdown</em> of the group. In addition to the actions performed
 * by an orderly shutdown, the {@code shutdownNow} method closes all open channels
 * in the group.
 *
 * <p><b>Implementation Note</b>
 * <p>An SSLAsynchronousChannelGroup object is essentially a wrapper of an
 * {@link AsynchronousChannelGroup} group. The reasons, why class {@link SSLAsynchronousSocketChannel}
 * does not use {@link AsynchronousChannelGroup} directly, are:
 * <ol>
 *  <li>SSLAsynchronousSocketChannel needs a group with a single-threaded executor service. This is
 *      constructed in the SSLAsynchronousChannelGroup constructor.</li>
 *  <li>SSLAsynchronousSocketChannel class needs to access the executor service to that the group is
 *      bound. However, class AsynchronousChannelGroup does not offer a method to access the underlying
 *      executor service.</li>
 * </ol>
 *
 * @see nio2.ssl.SSLAsynchronousSocketChannel#open(SSLAsynchronousChannelGroup)
 * @since Java 8
 * @author Ralph Ellinger
 */
public class SSLAsynchronousChannelGroup {

    private static final Logger logger = LoggerFactory.getLogger(SSLAsynchronousChannelGroup.class);

    private final AsynchronousChannelGroup group;
    private final ExecutorService groupExecutor;
    private ConcurrentLinkedQueue<SSLAsynchronousSocketChannel> channels;
    private final Object lock; 
    private volatile boolean isShuttingDown; 

    /**
     * Constructs an SSLAsynchronousChannelGroup group.
     * @throws  IOException
     *          If an I/O error occurs
     */
    public SSLAsynchronousChannelGroup() throws IOException {
        this(Executors.newSingleThreadExecutor());
    }

    /**
     * Constructs an SSLAsynchronousChannelGroup group with a given executor service.
     * @param   executor
     *          Executor service to which the group is bound. The executor service needs to be single-threaded.
     *          Otherwise the results are unpredictable for SSL/TLS processing.
     * @throws  IOException
     *          If an I/O error occurs
     */
    public SSLAsynchronousChannelGroup(ExecutorService executor) throws IOException {

        groupExecutor = executor;
        group = AsynchronousChannelGroup.withThreadPool(groupExecutor);
        channels = new ConcurrentLinkedQueue<>();
        lock = new Object(); 
    }

    /**
     * Tells whether or not this asynchronous channel group is shutdown.
     *
     * @return  {@code true} if this asynchronous channel group is shutdown or
     *          has been marked for shutdown.
     */
    public boolean isShutdown() {
        return group.isShutdown();
    }

    /**
     * Tells whether or not this group has terminated.
     *
     * <p> Where this method returns {@code true}, then the associated thread
     * pool has also {@link ExecutorService#isTerminated terminated}.
     *
     * @return  {@code true} if this group has terminated
     */
    public boolean isTerminated() {
        return group.isTerminated();
    }

    /**
     * Initiates an orderly shutdown of the group.
     *
     * <p> Shuts down each asynchronous channel bound to the group and when all 
     * channels are closed, the underlying executor service is terminated. 
     * Further attempts to construct channels that bind to this group will throw
     * {@link java.nio.channels.ShutdownChannelGroupException}.
     * This method has no effect if the group is already shutdown.
     */
    public void shutdown() {
        
        synchronized(lock) { 
            if (isShuttingDown) { 
                return; 
            } else { 
                isShuttingDown = true; 
            }
        }
        
        // define completion handler for shutdown of the channels 
        class ShutdownHandler implements CompletionHandler<Void, Void> { 
                                   
            @Override 
            public void completed(Void v, Void w) {
                
                if (channels.isEmpty()) { 
                    group.shutdown();                    
                }                
            }
            
            // Note: SSLAsynchronousSocketChannel.shutdown never leads to an 
            // invocation of handler.failed. Therefore it can be empty. 
            @Override
            public void failed(Throwable t, Void w) {}
        }
        
        channels.forEach(c -> c.shutdown(10, TimeUnit.SECONDS, null, new ShutdownHandler()));               
    }

    /**
     * Closes all open channels in the group and terminates the executor service.
     *
     * <p> The method closes all channels bound to the group and then terminates 
     * the associated executor service. Further attempts to construct channels that bind 
     * to this group will throw
     * {@link java.nio.channels.ShutdownChannelGroupException}.
     * This method has no effect if the group is already shutdown.
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public void shutdownNow() throws IOException {
        
        channels.forEach(c -> c.close()); 
        groupExecutor.shutdownNow(); 
        group.shutdownNow();
    }

    /**
     * Awaits termination of the group.

     * <p> This method blocks until the group has terminated, or the timeout
     * occurs, or the current thread is interrupted, whichever happens first.
     *
     * @param   timeout
     *          The maximum time to wait, or zero or less to not wait
     * @param   unit
     *          The time unit of the timeout argument
     *
     * @return  {@code true} if the group has terminated; {@code false} if the
     *          timeout elapsed before termination
     *
     * @throws  InterruptedException
     *          If interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return group.awaitTermination(timeout, unit);
    }

    // Package local for internal use
    ExecutorService getExecutor() {
        return groupExecutor;
    }

    // Package local for internal use
    AsynchronousChannelGroup getGroup() {
        return group;
    }

    // Package local for internal use
    boolean add(SSLAsynchronousSocketChannel channel) {
        return channels.add(channel);
    }

    // Package local for internal use
    boolean remove(SSLAsynchronousSocketChannel channel) {
        return channels.remove(channel);
    }

}
