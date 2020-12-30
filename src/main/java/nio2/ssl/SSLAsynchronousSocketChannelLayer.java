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


import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class SSLAsynchronousSocketChannelLayer extends SSLAsynchronousSocketChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(SSLAsynchronousSocketChannelLayer.class);    
    
    /* ------------------------------------------------------------------------- */
    /* ----------------  EXAMINE  ---------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    
    /* Examines responses from SSLEngineAutomat */ 
    protected void examine(Response response, Context ctx) throws SSLException {

        logger.trace("response: {} action: {}", response.toString(), ctx.getAction());

        try {

            // Possible responses: DROP, DO_READ, DO_WRITE, DO_TASK, APP_DATA, ACQUIRE_APP_STORAGE, OK
            switch(response) {

                case DO_WRITE:
                    ctx.setInvokeAutomat(true);
                    netWriteBuffer.flip();
                    writeChannel(ctx);
                    break;

                case DO_READ:
                    ctx.setInvokeAutomat(true);
                    readCount = 0;
                    netReadBuffer.compact();
                    netReadBufferState.setEmpty(true);
                    readChannel(ctx);
                    return; // return in this case!

                case DO_TASK:
                    ctx.setInvokeAutomat(true);
                    doTask(ctx);
                    return; // return in this case!

                case APP_DATA:

                    switch(ctx.getAction()) {

                        case READ: // return data to user
                            int len = ctx.getExternalBuffer().position() - ctx.getStartExternalBuffer();
                            readPending = false;
                            executeExternalCompleted(len, ctx);
                            return;  // return in this case!

                        default:
                            // store buffer in app data, if it is not already there

                            boolean found = false;
                            ByteBuffer appBuffer = ctx.getAppBuffer();
                            for (ByteBuffer b: appDataStorage) {
                                if (b == appBuffer) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                appDataStorage.add(appBuffer);
                                appDataState.add(Boolean.FALSE);
                            }

                    } // end switch
                    break;


                // appBuffer too small - allocate additional app buffer and invoke
                // the automat again
                case ACQUIRE_APP_STORAGE:

                    ctx.setInvokeAutomat(true);
                    ByteBuffer appBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                    appDataStorage.add(appBuffer);
                    appDataState.add(Boolean.FALSE);
                    ctx.setAppBuffer(appBuffer);
                    examine(automat.invoke(ctx.getAction(), Action.UNWRAP, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                    return;

                // OK is returned by SSLEngineAutomat, if
                // 1. action == shutdown & hsStatus == closed
                // 2. (action == connect or action == handshake) & hsStatus == finished
                // 3. hsStatus == not_handshaking
                case OK:
                    ctx.setInvokeAutomat(false);
                    doOK(ctx);
                    // We need to return here. For, consider the scenario:
                    //   1. connect finishes
                    //   2. within the outer action == connect, the completion method of the user's connect handler is invoked
                    //   3. after the completion method is processed, we are here, at this place. Without a return we
                    //      would do the checks below and it could happen that we wronly invoke the automat again (wrongly,
                    //      because the connect is finished)
                    return;

                case DROP:
                    // do nothing
                    break;


                case CLOSED:  // we received a close_notify alert from server

                    ctx.setAction(Action.CLOSE_NOTIFY);
                    try {
                        examine(automat.invoke(Action.CLOSE_NOTIFY, Action.REINVOKE, DUMMY, netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                    } catch(SSLException e) {
                        close();
                        executeExternalCompleted(-1, ctx);
                    }
                    return;  // return in this case!


                default: // should not happen

                    logger.error("Method examin() called with illegal response code: {}", response);
                    throw new IllegalStateException("Method examin() called with illegal response code: " + response);

            }  // end-switch(response)

            // Data had been read
            if ( ctx.getCompletedHandler().equals(Handler.READ) && !internalReadPending ) {

                if ( netReadBufferState.isEmpty() || !netReadBuffer.hasRemaining() ) { // no more data

                    // we need to ensure that no user read will be lost during a rehandshake or a key update request. Possible scenario:
                    //   1. user calls read()
                    //   2. a key update request is received
                    //   3. after unwrapping, the handshake status is need_wrap
                    //   4. SSLEngineAutomat returns DO_WRITE
                    //   5. the write handler completes and checks the handshake status -> the status is NOT_HANDSHAKING
                    // -> so it can happen, that that the user's read call is in effect ignored
                    // -> therefore, we need to read again
                    if (ctx.getAction().equals(Action.READ)) {
                        ctx.setInvokeAutomat(true);
                        readCount = 0;
                        netReadBuffer.compact();
                        netReadBufferState.setEmpty(true);
                        readChannel(ctx);
                    }

                } else { // more net data -> process next data
                    examine(automat.invoke(ctx.getAction(), Action.UNWRAP, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                }

            }

        } catch(Exception e) {
            logger.trace("Exception: {} msg: {}", e.getClass().getName(), e.getMessage());
            throw new SSLException(e.getMessage(), e);
        }
    }


    protected void doOK(Context ctx) throws Exception {

        logger.trace("start");

        switch(ctx.getAction()) {

            case CONNECT:
                connectPending = false;
                isConnected = true;
                executeExternalCompleted(null, ctx);
                return;

            case READ:
                readPending = false;
                int len = ctx.getExternalBuffer().position() - ctx.getStartExternalBuffer();
                executeExternalCompleted(len, ctx);
                return;

            case WRITE:
                writePending = false;
                len = ctx.getExternalBuffer().position() - ctx.getStartExternalBuffer();
                executeExternalCompleted(len, ctx);
                return;

            case HANDSHAKE:
                // ignore (??)
                return;

            case SHUTDOWN:

                switch( ctx.getCompletedHandler() ) {

                    case WRITE: // close_notify alert has been sent

                        // 1) TLS 1.2 and earlier relies on a duplex close policy, while TLS 1.3 relies on
                        // a half-close policy. That means in TLS 1.2, the server replies a close_notify
                        // alert after it has received a close_notify alert from client, while in TLS 1.3,
                        // the server isn't required to reply a close_notify alert (i.e. the server's write
                        // side can be kept open, even if the client's write side is closed).
                        // -> in TLS 1.3 we close the channel directly after having sent the close_notify alert
                        // 2) If a read has timed out, it isn't possible to read any longer data over the channel.
                        // Hence, we can't receive the server's close_notify alert in TLS 1.2 in this case, and
                        // close therefore the channel directly
                        if ( engine.getSession().getProtocol().equals("TLSv1.3") || readTimedOut ) {
                            close();
                            executeExternalCompleted(null, ctx);
                        } else {
                            // read server's close_notify alert
                            examine(Response.DO_READ, ctx);
                        }
                        return;

                    case READ: // close_notify has been received from peer
                        channel.close();
                        executeExternalCompleted(null, ctx);
                        return;

                    default:
                        logger.trace("Illegal completed handler: {}", ctx.getCompletedHandler());
                        throw new IllegalStateException("Illegal completed handler: " + ctx.getCompletedHandler());
                }

            case CLOSE_NOTIFY:
                close();
                executeExternalCompleted(-1, ctx);
                return;

            default:
                logger.trace("Unknown action: {}", ctx.getAction());
                throw new IllegalStateException("Unknown action: " + ctx.getAction());
        }
    }

    protected void doNotOK(Throwable t, Context ctx) {

        logger.trace("start");

        // Initialize internal buffers
        if (netWriteBuffer != null) {
            netWriteBuffer.clear();
        }
        if (netReadBuffer != null) {
            netReadBuffer.clear();
            netReadBufferState.setEmpty(true);
        }

        switch(ctx.getAction()) {

            case CONNECT:
                connectPending = false;
                executeExternalFailed(t, ctx);
                return;

            case READ:
                readPending = false;
                executeExternalFailed(t, ctx);
                return;

            case WRITE:
                writePending = false;
                executeExternalFailed(t, ctx);
                return;

            case HANDSHAKE:
                logger.trace("Exception: {}, msg: {}", t.getClass().getName(), t.getMessage());
                close();
                return;

            case SHUTDOWN:
                close();
                executeExternalCompleted(null, ctx);
                return;

            case CLOSE_NOTIFY:
                close();
                executeExternalCompleted(-1, ctx);
                return;

            default:
                logger.trace("Unknown action: {}", ctx.getAction());
        }
    }


    protected void doTask(Context ctx) throws SSLException {

        logger.trace("start");

        /**  Run task(s) in current thread  **/

        if ( sslTaskWorker == null ) {

            while ( (task = engine.getDelegatedTask()) != null ) {
                task.run();
            }
            examine(automat.invoke(ctx.getAction(), Action.REINVOKE, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
            return;
        }

        /**  Run task(s) in sslTaskWorker  **/

        ctx.setTaskCounter(new AtomicInteger(0));
        while ((task = engine.getDelegatedTask()) != null) {
            ctx.getTaskCounter().incrementAndGet();

            // Wrap delegate task in a new runnable.
            // Reason: Handshake can not continue before all delegates are
            // finished. The wrapper runs the delegate und if no more delegates
            // are running, it continues handshaking in a thread associated
            // with the AsynchronousChannelGroup.
            sslTaskWorker.execute(
                () -> {

                    try {

                        // Run delegate
                        task.run();

                        // Check if all delegates are finished
                        if (ctx.getTaskCounter().decrementAndGet() == 0) {
                            // Continue handshake in a thread associated with the channel group
                            groupExecutor.execute(
                                () -> {
                                    try {
                                        examine(automat.invoke(ctx.getAction(), Action.REINVOKE, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                                    } catch(SSLException e) {
                                        doNotOK(e, ctx);
                                    }
                                }
                            );
                        }
                    } catch(Exception e) {
                        doNotOK(e, ctx);
                    }
                }
            );
        } // end-while
    }

    /* ------------------------------------------------------------------------- */
    /* ----------------  READCORE ---------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /*
     * Implements core of the read() method.
     *
     * The method is invoked in two places:
     *  1. read()
     *  2. processDeferedReadTask()
     *
     * The parameter needTask controls, if part of the processing is done in a new task.
     * If needTask is false, the whole method is performed in the current task (which then
     * necessarily runs in the group thread). If needTask is true, then the parts that
     * invoke handler.completed resp. handler.failed are processed in a new task. This
     * happens iff read() is invoked in the group thread and ENABLE_INLINE_READ is false.
     * This ensures that handler.completed/failed is called in a task that runs after the
     * task where read() is called (in case inlining is not allowed).
     *
     * More specifically, needTask is set in the following cases for the following reasons:
     * 1. read() called in external thread: needTask = false, because read() already starts
     *    a new task in which readCore is executed.
     * 2. read() called in group thread: needTask = !ENABLE_INLINE_READ: see above
     * 3. processDeferedReadTask(): needTask = false, because the method processDeferedReadTask
     *    is already invoked in a new task (in a former the read was defered).
    */
    protected <A> void readCore(final ByteBuffer dst,
                                final long timeout,
                                final TimeUnit unit,
                                final A attachement,
                                final CompletionHandler<Integer, ? super A> handler,
                                final boolean needTask)
                       throws ShutdownChannelGroupException {

        logger.trace("start {}", this);

        // First return app data from appDataStorage
        if ( appDataStorage != null && !appDataStorage.isEmpty() ) {
            logger.trace("data in appDataStorage");

            Runnable r1 = () -> {

                int start = dst.position();

                while(!appDataStorage.isEmpty()) {

                    ByteBuffer appBuffer = appDataStorage.peek();
                    boolean appBufferFlipped = appDataState.peek();
                    if (!appBufferFlipped) {
                        appBuffer.flip();
                        appDataState.set(0, Boolean.TRUE);   // set flipped = true
                    }

                    int d = dst.remaining();
                    if ( appBuffer.remaining() <= d ) {
                        dst.put(appBuffer);
                        appDataStorage.poll();
                        appDataState.poll();
                    } else {
                        for(int i = 0; i < d; i++) {
                            dst.put(appBuffer.get());
                        }
                        break;
                    }
                }
                readPending = false;
                handler.completed(dst.position() - start, attachement);
            }; // end r1

            if (needTask) {
                logger.trace("execute r1 as new task");
                groupExecutor.execute(r1);
            } else {
                logger.trace("running r1 directly");
                r1.run();
            }
            return;
        }

        // Create Context
        Context ctx = new Context(Action.READ, handler, attachement, dst);
        ctx.setTimeout(timeout);
        ctx.setTimeUnit(unit);

        // defer, if a read process is running in background
        if (internalReadPending){
            // put read command on the waiting queue
            if (readWaiting == null) {
                readWaiting = new LinkedList<>();
            }
            readWaiting.add(ctx);
            return;
        }

        // Check if there are data in netReadBuffer
        if ( !netReadBufferState.isEmpty() && netReadBuffer.hasRemaining() ) {
            logger.trace("remaining data in netReadBuffer");

            // if we need to process in a separate task, we repeat the whole method, because until that tasks runs,
            // another task (e.g. a rehandshake) may have processed the data from netReadBuffer
            if (needTask) {
                logger.trace("perform method again in new task");
                groupExecutor.execute( () -> readCore(dst, timeout, unit, attachement, handler, false) );

            } else {
                try {
                    ctx.setCompletedHandler(Handler.READ);
                    examine(automat.invoke(Action.READ, Action.UNWRAP, dst, netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                } catch(SSLException e) { // if an exception is thrown (e.g. the channel was closed), we call the user's failed method
                    internalReadPending = false;
                    doNotOK(e, ctx);
                }
            }
            return;
        }

        // Read data from physical channel
        ctx.setInvokeAutomat(true);
        readCount = 0;
        netReadBuffer.compact();
        netReadBufferState.setEmpty(true);
        readChannel(ctx);

    }

/*
* Reasons for using @SuppressWarnings("unchecked") below
 *
 * The public methods read and write are generic with type A for the attachement and the completion handler. However,
 * because of possible rehandshakes (TLS 1.2 and earlier) or key updates (TLS 1.3), a single read or write call by the
 * user could internally result in a sequence of read and write calls. We therefore save the user's attachement and handler
 * in a context object, that is passed through this sequence of intternal read and write calls. So, ideally, this context
 * should also be generic of type A. But there are two reasons that prevent to make the context generic:
 *
 *  1. All internal read and write operations (i.e. AsynchronousSocketChannel.read resp. AsynchronousSocketChannel.write)
 *     take as attachement the context and as handler the object readHandler resp. writeHandler. However, if the
 *     attachement is generic, the handle has to be generic as well (as can be seen directly from the syntax of
 *     AsynchronousSocketChannel.read resp. AsynchronousSocketChannel.write). Hence, if the context were generic, we would need
 *     to instantiate a new internal read handler and internal write handler object for each call of the public read or write
 *     method by the user. This would be very inefficient and not GC friendly. By making the context non-generic, all we need are
 *     the two objects readHandler and writeHandler.
 *
 *  2. It can happen that a user calls the public read method, but internally the read operation is pending because of a rehandshake or
 *     a key update. In this case, we need to save the context in an attribute of our class SSLAsynchronousSocketChannel , until
 *     the read operation is no longer pending. But if the context were generic, the whole class SSLAsynchronousSocketChannel would
 *     need to be generic. This isn't possible. For, the user can call write() with an attachement of type A and read() with
 *     an attachement of type B. Hence our class would need to be of type A as well as of type B.
 *
 * Therefore, the context object is taken to be non-generic. Since we assign generic types from the public methods read and write in the
 * context to non-generic variables, the generic type is lost. This yields warnings which we suppress.
*/
    @SuppressWarnings("unchecked") // cf. "Reasons for using @SuppressWarnings" at the beginning
    protected void executeExternalCompleted(Object obj, Context ctx) {
        CompletionHandler externalHandler;
        if ( (externalHandler = ctx.getExternalHandler()) != null ) {
            externalHandler.completed(obj, ctx.getExternalAttachement());
        }
    }

    @SuppressWarnings("unchecked") // cf. "executeExternalCompleted" for explanation 
    protected void executeExternalFailed(Throwable t, Context ctx) {
        CompletionHandler externalHandler;
        if ( (externalHandler = ctx.getExternalHandler()) != null ) {
            externalHandler.failed(t, ctx.getExternalAttachement());
        }
    }


    protected void readChannel(Context ctx) {

        logger.trace("start internalReadPending: {}", internalReadPending);
        if (internalReadPending){
            // put read command on the waiting queue
            if (readWaiting == null) {
                readWaiting = new LinkedList<>();
            }
            readWaiting.add(ctx);

        } else {
            // note: this is the only place, where internalReadPending is set to be true
            channel.read(netReadBuffer, ctx.getTimeout(), ctx.getTimeUnit(), ctx, readHandler);
            internalReadPending = true;
        }
    }

    protected void writeChannel(Context ctx) {

        logger.trace("start internalWritePending: {} action: {}", internalWritePending, ctx.getAction());
        if (internalWritePending){
            // put read command on the waiting queue
            if (writeWaiting == null) {
                writeWaiting = new LinkedList<>();
            }
            writeWaiting.add(ctx);

        } else {
            // note: this is the only place, where internalWritePending is set to be true
            channel.write(netWriteBuffer, ctx.getTimeout(), ctx.getTimeUnit(), ctx, writeHandler);
            internalWritePending = true;
        }
    }

    protected void processDeferedWriteTask() {

        logger.trace("start");

        // Ensure that write isn't pending - otherwise, we keep the tasks waiting
        if (internalWritePending) {
          return;
        }

        if (shutdownWaiting != null) {
            Context ctx = shutdownWaiting;
            shutdownWaiting = null;
            try {
                examine(automat.invoke(ctx.getAction(), Action.SHUTDOWN, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
            } catch(SSLException e) {
                doNotOK(e, ctx);
            }
        }

        if (writeWaiting != null && !writeWaiting.isEmpty()) {
            try {
                Context ctx = writeWaiting.remove();
                try {
                    examine(automat.invoke(ctx.getAction(), Action.WRAP, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
                } catch(SSLException e) {
                    doNotOK(e, ctx);
                }
                return;
            } catch(NoSuchElementException e) {
                // ignore it
            }
        }

        if (handshakeWaiting != null) {
            Context ctx = handshakeWaiting;
            handshakeWaiting = null;
            try {
                examine(automat.invoke(ctx.getAction(), Action.HANDSHAKE, ctx.getAppBuffer(), netWriteBuffer, netReadBuffer, netReadBufferState, internalWritePending, internalReadPending), ctx);
            } catch(SSLException e) {
                doNotOK(e, ctx);
            }
        }
    }

    @SuppressWarnings("unchecked")  // // cf. "executeExternalCompleted" for explanation 
    protected void processDeferedReadTask() {

        logger.trace("start");

        // Ensure that read isn't pending - otherwise, we keep the tasks waiting
        if (internalReadPending) {
          return;
        }

        if (readWaiting != null && !readWaiting.isEmpty()) {
            try {
                Context ctx = readWaiting.remove();
                try {
                    readCore(ctx.getExternalBuffer(), ctx.getTimeout(), ctx.getTimeUnit(), ctx.getExternalAttachement(), ctx.getExternalHandler(), false);
                } catch(Exception e) {
                    doNotOK(e, ctx);
                }
            } catch(NoSuchElementException e) {
                // ignore it
            }
        }
    }


}
