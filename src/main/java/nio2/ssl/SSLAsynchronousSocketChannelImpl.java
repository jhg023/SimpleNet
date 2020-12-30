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

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.WritePendingException;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import javax.naming.OperationNotSupportedException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import static nio2.ssl.SSLAsynchronousSocketChannel.DUMMY;
import static nio2.ssl.SSLAsynchronousSocketChannel.INTERNAL_TIMEOUT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSLAsynchronousSocketChannelImpl extends SSLAsynchronousSocketChannelLayer {

    private static final Logger logger = LoggerFactory.getLogger(SSLAsynchronousSocketChannelImpl.class);

        
    /* ------------------------------------------------------------------------- */
    /* ---------------- CONNECT ------------------------------------------------ */
    /* ------------------------------------------------------------------------- */

    /** Connects this channel to the remote address. */ 
    @Override 
    public <A> void connect(SocketAddress remote, A attachement, CompletionHandler<Void, ? super A> handler)
                throws UnresolvedAddressException,
                       UnsupportedAddressTypeException,
                       AlreadyConnectedException,
                       ConnectionPendingException,
                       SecurityException {

        logger.trace("start {}", this);

        // If connection type is unknown, determine it with help of the port number
        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.UNKNOWN) {
            if (remote instanceof InetSocketAddress &&
                    ((InetSocketAddress)remote).getPort() == 443) {
                connectionType = SSLAsynchronousSocketChannel.ConnectionType.SSL;
            } else {
                connectionType = SSLAsynchronousSocketChannel.ConnectionType.NON_SSL;
            }
        }

        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.NON_SSL) {
            channel.connect(remote, attachement, handler);
            return;
        }

        /**  SSL/TLS connection  **/

        if (connectPending) {
            throw new ConnectionPendingException();
        }
        connectPending = true;
        Context ctx = new Context(Action.CONNECT, handler, attachement, DUMMY);
        channel.connect(remote, ctx, new SSLConnectHandler());
    }

    // Internal connect handler for SSL connections. If the connection has been
    // established or if establishing the connecion fails, the user's
    // connect handler method completed resp. failed is invoked.
    private class SSLConnectHandler implements CompletionHandler<Void, Context> {

        @Override
        public void completed(Void v, Context ctx) {

            logger.trace("start");

            try {

                // save referenve to current thread
                groupThread = Thread.currentThread();


                /**  Initializations needed for SSL/TLS  **/

                appDataStorage = new LinkedList<>();
                appDataState = new LinkedList<>();

                SSLContext context = SSLContext.getDefault();

                if (initSSLContext) {
                    initSSLContext = false;
                    context.init(savedKeyMgr, savedTrustMgr, savedSecureRand);
                }
                engine = context.createSSLEngine();
                engine.setUseClientMode(true);
                if (setSNIHostName) {
                    setSNIHostName(savedHostName);
                    setSNIHostName = false;
                }
                
                automat = new SSLEngineAutomat(engine);
                writeHandler = new SSLWriteHandler();
                readHandler = new SSLReadHandler();

                if (netWriteBuffer == null) {
                    netWriteBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize() + 3000);
                }

                if (netReadBuffer == null) {
                    netReadBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize() + 3000);
                    netReadBufferState.setEmpty(true);
                }

                // Start initial handshake
                examine(automat.invoke(Action.CONNECT, Action.CONNECT, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);

            } catch(Exception t) {
                logger.trace("Exception during processing of ConnectHandler.completed. ex: {} msg: {}", t.getClass().getName(), t.getMessage());
                doNotOK(t, ctx);
            }
        }

        @Override
        public void failed(Throwable t, Context ctx) {
            logger.trace("ErrType: {} ErrMsg: {}", t.getClass().getName(), t.getMessage());

            // save group thread
            groupThread = Thread.currentThread();

            doNotOK(t,ctx);
        }

    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- WRITE -------------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /** Writes a sequence of bytes to this channel from the given buffer.
     * @throws javax.net.ssl.SSLException */ 
    @Override 
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit,
                            A attachement, CompletionHandler<Integer, ? super A> handler)
                    throws IllegalArgumentException,
                           NotYetConnectedException,
                           WritePendingException,
                           ShutdownChannelGroupException,
                           SSLException {

        logger.trace("start");

        // reflects behavoir of class AsynchronousSocketChannel
        if (writeTimedOut) {
            throw new IllegalStateException("Writing not allowed due to timeout or cancellation");
        }

        if ( src == null )
            throw new IllegalArgumentException("Empty source buffer");

        if ( !src.hasRemaining() )
            handler.completed(0, attachement);

        /**  Non-SSL/TSL case  **/

        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.NON_SSL) {
            channel.write(src, timeout, unit, attachement, handler);
            return;
        }

        /**  SSL/TLS connection  **/

        if (!isConnected)
            throw new NotYetConnectedException();

        // concurrent write operations not supported on AsynchronousSocketChannel
        synchronized(writeLock) {
            if (writePending) {
                throw new WritePendingException();
            }
            writePending = true;
        }

        final Context ctx = new Context(Action.WRITE, handler, attachement, src);
        ctx.setTimeout(timeout);
        ctx.setTimeUnit(unit);

        // The following code is executed as a task in the groupExecutor, if it is not already
        // running in the groupThread. This is because the code calls channel.write. Executing in a task of the
        // single-threaded groupExecutor avoids concurrency conflicts.
        Runnable r = () -> {

            // defer, if a write process is running in background
            if (internalWritePending){
                // put write command on the waiting queue
                if (writeWaiting == null) {
                    writeWaiting = new LinkedList<>();
                }
                writeWaiting.add(ctx);
                return;
            }

            try {
                // Encode app data
                ctx.setCompletedHandler(Handler.NONE);
                examine(automat.invoke(ctx.getAction(), Action.WRAP, src, netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
            } catch(SSLException e) {
                internalWritePending = false;
                doNotOK(e, ctx);
            }
        };

        if (Thread.currentThread() == groupThread) {
            // run directly
            logger.trace("running in groupThread");
            r.run();
        } else {
            // execute task (note: groupExecutor is single-threaded)
            logger.trace("execute as new task");
            groupExecutor.execute(r);
        }
    }


    // Internal write handler for SSL connections. If all data from the user's
    // source buffer are written to the channel (in encrypted form), the user's
    // write handler method completed resp. failed is invoked.
    private class SSLWriteHandler implements CompletionHandler<Integer, Context> {

        @Override
        public void completed(Integer bytesWritten, Context ctx) {

            // We are faced with the following problem:
            // When AsyncSocketChannel.write is invoked, the user's completion handler's
            // completion method should return the number of bytes that have been
            // written to the channel. However, internally in the AsyncSocketChannel,
            // the app data are encrypted before writing to the channel, having the
            // effect that the encrypted message is longer than the pure app data. So,
            // the parameter bytesWritten actually doesn't tell how many bytes of app
            // data have been written to the channel. Therefore there is no point in
            // returning this value to the user's completion handler's completion method.
            // On the other hand, the current completion handler just doesn't know how
            // many app data have been written to the channel. So, after a write to the
            // channel is completed, which number of written bytes should be returned to
            // the user's completion handler's completion method ?
            //   As solution of the problem, we repeat writing data into the channel till
            // all of the user's app data have been send to the peer and then just the
            // total number of app data is returned to the user's completion handler's
            // completion method.

            logger.trace("bytesWritten: {} action: {} netWriteBuffer: {} this: {}", bytesWritten, ctx.getAction(), netWriteBuffer, this);

            try {

                internalWritePending = false;
                ctx.setCompletedHandler(Handler.WRITE);

                // Repeat writing, if there are data left in the netBuffer
                if (netWriteBuffer.hasRemaining()) {
                    writeChannel(ctx);
                    return;
                }

                // All data in netWriteBuffer have been written to channel.
                // Thus we can clear netWriteBuffer.
                netWriteBuffer.clear();

                // we need to make sure that the completed method of the user's completion handler is called
                // if the app data are completely written. Consider the following scenario:
                //   1. user calls write(), channel.write processes in background
                //   2. the read handler completes and the automat says need_wrap because of
                //      a rehandshake or key update. The write command is dropped because internal write is
                //      pending.
                //   3. the write handler completes. It invokes the automat who again says need_wrap (because
                //      no wrap had been done).
                // In this case we need to
                //   a) execute the completed method of the user's completion handler
                //   b) wrap and write the wrapped data into the channel
                //  We also need to make sure that we don't call the completed method of the user's handler again
                //  after the write in b) completed. For this reason ctx.action is changed for b) from WRITE to
                //  HANDSHAKE.
                if (ctx.getAction().equals(Action.WRITE)) {
                    logger.trace("writing of user data completed");
                    examine(Response.OK, ctx);
                    writePending = false;
                    ctx.setAction(Action.HANDSHAKE);
                    ctx.setTimeout(INTERNAL_TIMEOUT);
                    ctx.setTimeUnit(INTERNAL_TIME_UNIT);
                }

                // Note: If an exception is thrown, we are (in the catch-block) either calling the failed method of the user's handler,
                // or we close the channel in case a rehandshake / key update is going on. This also covers the case that we just executed
                // the user handler's completion method and then continue handshaking (because above the action changed). In any case,
                // we don't call the failed method after the completed method of a user handler has been invoked!
                examine(automat.invoke(ctx.getAction(), Action.REINVOKE, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);

            } catch(Exception e) {
                doNotOK(e, ctx);

            } finally {
                processDeferedWriteTask();
            }

        }

        @Override
        public void failed(Throwable t, Context ctx) {
            logger.trace("ErrType: {} ErrMsg: {}", t.getClass().getName(), t.getMessage());

            if (t instanceof InterruptedByTimeoutException) {
                writeTimedOut = true;
            }

            internalWritePending = false;
            ctx.setCompletedHandler(Handler.NONE);
            doNotOK(t, ctx);
            processDeferedWriteTask();
        }
    }


    /* ------------------------------------------------------------------------- */
    /* ---------------- READ --------------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /** Reads a sequence of bytes from this channel into the given buffer. */
    @Override
    public <A> void read(final ByteBuffer dst,
                         final long timeout,
                         final TimeUnit unit,
                         final A attachement,
                         final CompletionHandler<Integer, ? super A> handler)
                    throws IllegalArgumentException,
                           NotYetConnectedException,
                           ReadPendingException,
                           ShutdownChannelGroupException {

        logger.trace("start {}", this);

        if ( dst == null )
            throw new IllegalArgumentException("destination buffer is null");

        // reflects behavoir of class AsynchronousSocketChannel
        if (readTimedOut) {
            throw new IllegalStateException("Reading not allowed due to timeout or cancellation");
        }

        /**  Non-SSL/TSL case  **/

        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.NON_SSL) {
            channel.read(dst, timeout, unit, attachement, handler);
            return;
        }


        /**  SSL/TLS connection  **/

        if (!isConnected)
            throw new NotYetConnectedException();

        // concurent read operations not supported by AsynchronousSocketChannel
        synchronized(readLock) {
            if (readPending) {
                throw new ReadPendingException();
            }
            readPending = true;
        }

        // The readCore is executed as a task in the groupExecutor, if it is not already
        // running in the groupThread. This is for two reasons:
        // 1. If there are app data in appDataStorage, they have to be delivered by the handler.completed
        //    method and, by convention, this method must be exectued in groupThread.
        // 2. If there are no data in appDataStorage, the code operates on netReadBuffer or calls channel.read.
        //    Executing it in a task of the single-threaded groupExecutor avoids concurrency conflicts.
        if (Thread.currentThread() != groupThread) {
            logger.trace("read() called in external thread");
            groupExecutor.execute( () ->  readCore(dst, timeout, unit, attachement, handler, false) );
        } else { // we are running in group thread
            readCore(dst, timeout, unit, attachement, handler, !ENABLE_INLINE_READ);
        }
    }

    // Internal read handler for SSL connections. Data that are read from the physical
    // channel are decrypted. Afterwards, the user's external read handler method
    // completed is invoked. If the read operation fails, the user's external read
    // handler method failed is invoked.
    private class SSLReadHandler implements CompletionHandler<Integer, Context> {

        @Override
        public void completed(Integer bytesRead, Context ctx) {

            logger.trace("bytesRead: {} action: {} {}", bytesRead, ctx.getAction(), this);

            internalReadPending = false;
            ctx.setCompletedHandler(Handler.READ);
            try {

                netReadBuffer.flip();

                Action action = ctx.getAction();
                switch(bytesRead) {

                    case -1:   // EOF

                        close();

                        switch(action) {

                            case READ:  // return bytes read to user's completed() method

                                readPending = false;
                                executeExternalCompleted(-1, ctx);
                                break;

                            default:  // call user's failed() method

                                Exception e = new EOFException("Received EOF from Peer");
                                doNotOK(e, ctx);
                                break;
                        }
                        break;

                    case 0:  // usually 0 bytes read precedes an EOF

                        if (action != Action.READ) { // try again
                            if ( readCount < MAX_READ ) {
                                readChannel(ctx);
                                readCount++;
                            } else { // Maximal number of retries reached
                                logger.error("Error during read: maximal number of retries reached");
                                Exception e = new SSLException("Too many consecutive reads with no data");
                                doNotOK(e, ctx);
                            }
                        } else { // action == READ -> invoke completed()
                            doOK(ctx);
                        }
                        break;

                    default:  // bytesRead > 0

                        netReadBufferState.setEmpty(false);
                        examine(automat.invoke(ctx.getAction(), Action.UNWRAP, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);

                }  // end switch


            } catch(Exception e) {
                doNotOK(e, ctx);
            } finally {
                processDeferedReadTask();
            }
        }

        @Override
        public void failed(Throwable t, Context ctx) {

            logger.trace("ErrType: {} ErrMsg: {}", t.getClass().getName(), t.getMessage());

            if (t instanceof InterruptedByTimeoutException) {
                readTimedOut = true;
            }

            internalReadPending = false;
            ctx.setCompletedHandler(Handler.NONE);
            doNotOK(t, ctx);
            processDeferedReadTask();
        }
    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- HANDSHAKE ---------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /* Starts a rehandshake or key update. */ 
    @Override 
    public void handshake() throws OperationNotSupportedException,
                                   NotYetConnectedException,
                                   SSLException {

        logger.trace("start");

        // groupExecutor is needed
        if (groupExecutor == null) {
            throw new OperationNotSupportedException("Method handshake() can only be used if parameter *groupExecutor* in method open is not null");
        }

        // Only supported for SSL/TLS connections
        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.NON_SSL) {
            throw new OperationNotSupportedException("Method handshake() can only be used for SSL/TLS connections.");
        }

        if (!isConnected) {
            throw new NotYetConnectedException();
        }

        // It may happen, that application data is received during the handshake/key update. Consider for instance the following
        // scenario:
        //   1. user writes an HTTP Get request
        //   2. user calls handshake()
        //   3. after the handshake call, the channel will write a ClientHello or key update request and then it will invoke a read
        //      command in case, the server replies with a ServerHello message or a key update response.
        //   4. server sends response to the HTTP Get request
        // Hence, the application data is received during handshake and has to be stored. This is done by the byte buffer
        // akquired below:
        ByteBuffer appBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        Context ctx = new Context(Action.HANDSHAKE, null, null, appBuffer);
        ctx.setTimeout(INTERNAL_TIMEOUT);
        ctx.setTimeUnit(INTERNAL_TIME_UNIT);

        // In order to avoid concurrency conflicts, we initiate the handshake as a task on groupExecutor
        groupExecutor.execute(
            () -> {

                logger.trace("Handshake task started executing");

                if (internalWritePending) {
                    handshakeWaiting = ctx;
                } else {
                    try {
                        examine(automat.invoke(Action.HANDSHAKE, Action.HANDSHAKE, appBuffer, netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                    } catch(SSLException e) {
                        doNotOK(e, ctx);
                    }
                }
            }
        );
    }    
    

    /* ------------------------------------------------------------------------- */
    /* ---------------- SHUTDOWN ----------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /* Performs an orderly shutdown of the asynchronous channel. */
    @Override 
    public void shutdown() {
        shutdown(INTERNAL_TIMEOUT, INTERNAL_TIME_UNIT, null, null);
    }    
    
    
    /** Performs an orderly shutdown of the asynchronous channel and invokes a completion handler after shutdown has finished.  */
    @Override
    public <A> void shutdown(long timeout, TimeUnit unit, A attachement, CompletionHandler<Void, ? super A> handler) { 

        logger.trace("start");

        if (!isOpen) {
            return;
        }

        synchronized(shutdownLock) {
            if(isShuttingDown) {
                return;
            } else {
                isShuttingDown = true;
            }
        }

        // 1) There is no shutdown method in class AsynchronousSocketChannel
        // we therefore simply close the channel for non ssl connections
        // 2) If there had been a timeout while writing in the channel, the
        // class AsynchronousSocketChannel doesn't allow writing to the 
        // channel any longer. In particular, we can't send a close_notify 
        // alert to the peer. Hence, all we can do is to close the channel.
        if (connectionType == SSLAsynchronousSocketChannel.ConnectionType.NON_SSL || writeTimedOut) {
            close();
            if (handler != null) {
                handler.completed(null, attachement);
            }
            return;
        }

        /*** SSL connection ***/

        Context ctx = new Context(Action.SHUTDOWN, handler, attachement, DUMMY);
        ctx.setTimeout(timeout);
        ctx.setTimeUnit(unit);

        // create groupExecutor task for shutdown
        groupExecutor.execute(
            () -> {

                logger.trace("Shutdown task started executing");

                if (internalWritePending) {
                    shutdownWaiting = ctx;
                } else {
                    try {
                        // we use DUMMY as app buffer because when shuting down we are no longer
                        // interested in receiving data
                        examine(automat.invoke(Action.SHUTDOWN, Action.SHUTDOWN, DUMMY, netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                    } catch(SSLException e) {
                        doNotOK(e, ctx);
                    }
                }
            }
        );
    }

}