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

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.naming.OperationNotSupportedException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code SSLAsynchronousSocketChannel} class represents an asynchronous socket
 * channel that supports encrypting and decrypting of data according to the SSL/TLS
 * protocol. Moreover, it can be configured, if the connection uses the SSL/TLS protocol 
 * or transfers data as plaintext. Hence the same interface can be used for SSL and
 * non-SSL connections.
 * <p>
 * The channel is a duplex channel, i.e. it is possible to write to and read
 * from the channel at the same time.
 * <p>
 * In Java 7 the {@code AsynchronousSocketChannel} class was implemented. It simplifies
 * usage of the {@code SocketChannel} class in a classical selector pattern by introducing
 * callback handlers for connect, write and read events. However, the
 * {@code AsynchronousSocketChannel} class supports only plaintext data transfer. Hence, if
 * an application needs to send or receive data via the SSL/TLS protocol, the application
 * itself needs to care for SSL/TLS handshaking and ciphering and deciphering of the
 * data. This limits the applicability of the {@code AsynchronousSocketChannel} class.
 * <p>
 * The {@code SSLAsynchronousSocketChannel} class extends by composition the
 * {@code AsynchronousSocketChannel} class by adding a layer for SSL/TLS. This layer
 * is responsible for handshaking and for encrypting and decrypting plaintext data. These
 * procedures are transparent to the user. It supports all versions of the SSL/TLS protocol,
 * including TLS 1.3.
 * <p>The public methods and the semantics of class
 * {@code SSLAsynchronousSocketChannel} follow that of class
 * {@code AsynchronousSocketChannel}. There are two factory methods for opening the channel:
 * <blockquote><pre>
 *    SSLAsynchronousSocketChannel channel = SSLAsynchronousSocketChannel.open(group);
 * </pre></blockquote><p>
 * In this case, SSL/TLS protocol is chosen automatically, if the port used for
 * connecting the channel to its peer is 443. Otherwise, the SSL/TLS protocol is not in
 * use. By setting the boolean parameter {@code useSSL} accordingly, the following overloaded
 * version of the method
 * <blockquote><pre>
 *   SSLAsynchronousSocketChannel channel = SSLAsynchronousSocketChannel.open(group, useSSL);
 * </pre></blockquote><p>
 * lets the user specify explicitly, if the SSL/TLS protocol should be used or not. After the channel is opend, a
 * connection to the peer will be established by invoking
 * <blockquote><pre>
 *   channel.connect(address, attachement, connectHandler);
 * </pre></blockquote><p>
 * The command
 * <blockquote><pre>
 *   channel.write(src, timeout, timeUnit, attachement, writeHandler);
 * </pre></blockquote><p>
 * writes data asynchronously to the channel. If the connection uses the SSL/TLS protocol,
 * the data are enchrypted before they are written into the physical socket. Asynchronous
 * reading of data sent by the peer is triggered by
 * <blockquote><pre>
 *   channel.read(dst, timeout, timeUnit, attachement, readHandler);
 * </pre></blockquote><p>
 * If SSL/TLS is in use, the data will be decrypted before they are written into the
 * destination {@code dst}.
 * <p>
 * <b>Note on the shutdown process:</b> TLS 1.2 and earlier versions of TLS use a duplex-close policy, i.e.
 * if one of the peers closes its write direction, the other peer will send a close_notify alert
 * and will close its write direction, too. In contrast, TLS 1.3 uses a half-close policy, i.e.
 * if one of the peers closes its write direction, the other peer is allowed let its write direction 
 * open and to continue sending data. The class {@code SSLAsynchronousSocketChannel} uses the 
 * duplex-close policy. This concerns two cases:
 * <ol>
 * <li>If a close_notify alert is received from a peer, an instance of {@code SSLAsynchronousSocketChannel}
 * will reply a close_notify alert and will close the connection.</li>
 * <li>The {@code shutdown()} methods will send a close_notify alert to the peer and will duplex-close the channel
 * after the alert was received by the peer.</li>
 * </ol>
 *
 * @since Java 8
 * @author Ralph Ellinger
 */

/*
 * ** Documentation of the exception handling **
 *
 * There are 2 types of interaction where an exception could be thrown:
 *
 *   1. call of a public method
 *
 *   2. processing of a background task. There, the following methods can be executed:
 *      a) connectHandler.completed / connectHandler.failed
 *      b) writeHandler.completed   / writeHandler.failed
 *      c) readHandler.completed    / readHandler.failed
 *      d) doTask
 *      e) the task coming from handshake()
 *
 * Exceptions are treated accordingly:
 *
 *   Case 1:   The exception is thrown (so the user can react)
 *
 *   Case 2:  - There are no critical actions in the failed methods; no exceptions should be thrown there. The failed methods call doNotOK. *
 *            - The completed methods all have a surrounding try-catch-block. In the catch-block doNotOK is called.
 *            - The methods writeHandler.completed, readHandler.completed also have a finally block where defered write/read commands
 *              are executed. The invoked methods processDeferedWriteTask and processDeferedReadTask perform their actions within a
 *              try-catch-block and the catch-block calls doNotOK.
 *            - If in doTask the delegated tasks are executed in their own tasks, the action takes place within a try-catch-block
 *              and the catch-block calls doNotOK. After the last one of the delegated tasks has terminated, a task is stated
 *              that invokes and examines the automat. This also happens in a try-catch-block where the catch-block calls doNotOK.
 *            - In the task submitted in handshake(), the action takes place in a try-catch-block and the catch-block calls doNotOK.
 *
 *   Summary: All possible actions are surrounded by a try-catch-block and the catch-block calls doNotOK.
 *
 * The central method doNotOK does:
 *
 *   1. If an exception was thrown at an action that has associated a user handler (i.e. the context's action is CONNECT,
 *      WRITE or READ), the failed method of the corresponding user handler is invoked.
 *
 *   2. If there is no associated user handler (i.e. the context's action is HANDSHAKE), then the channel is closed.
 *
*/
public abstract class SSLAsynchronousSocketChannel implements AsynchronousChannel {

    private static final Logger logger = LoggerFactory.getLogger(SSLAsynchronousSocketChannel.class);

    /**  Constants  **/

    protected static final byte MAX_READ = 20;
    protected static final long INTERNAL_TIMEOUT = 30;
    protected static final TimeUnit INTERNAL_TIME_UNIT = TimeUnit.SECONDS;
    protected static enum ConnectionType { SSL, NON_SSL, UNKNOWN }

    // Configure, if readHandler.completed can run as inline code or has to be processed as a task
    protected static final boolean ENABLE_INLINE_READ = false;

    /** Basic objects  **/

    protected java.nio.channels.AsynchronousSocketChannel channel;
    protected ByteBuffer netWriteBuffer;
    protected ByteBuffer netReadBuffer;
    protected BufferState netReadBufferState;
    protected SSLEngine engine;
    protected SSLEngineAutomat automat;
    protected CompletionHandler<Integer, Context> writeHandler;
    protected CompletionHandler<Integer, Context> readHandler;
    protected Queue<Context> readWaiting;
    protected Queue<Context> writeWaiting;
    protected Context handshakeWaiting;
    protected Context shutdownWaiting;


    // SSLEngine requires a right sized app buffer for initial handshaking.
    // This is a bug in SSLEngine since this app buffer isn't used in initial
    // handshake. In order to avoid vasting space, a static dummy buffer is allocated
    // once on class level to fulfill this SSLEngine's requirement.
    // 16384 = 2^14 is the value returned by engine.getSession().getApplicationBufferSize().
    // (we can't invoke these methods here because they aren't static).
    protected static final ByteBuffer DUMMY = ByteBuffer.allocate(16384);

    // Serves two purposes:
    //   1. If dst buffer is too small, unwrapped data is stored in appDataStorage
    //   2. App data received during a rehandshake are stored till handshake is finished
    // Note: No Interface type, because we use List as well as Queue properties
    protected LinkedList<ByteBuffer> appDataStorage;

    // Indicates if an appBuffer (contained in appDataStorage) has been flipped
    // Note: No Interface type, because we use List as well as Queue properties     
    protected LinkedList<Boolean> appDataState;

    protected byte readCount;

    /** State Variables  **/

    protected ConnectionType connectionType;
    protected volatile boolean isOpen;
    protected volatile boolean isConnected;
    protected boolean isShuttingDown;
    protected volatile boolean connectPending;
    protected boolean writePending;
    protected boolean readPending;
    protected boolean internalWritePending;
    protected boolean internalReadPending;
    protected boolean readTimedOut;
    protected boolean writeTimedOut;

    // use different locks, because the actions can be processed concurrently
    protected final Object writeLock;
    protected final Object readLock;
    protected final Object shutdownLock;


    /**  Parameters in Method calls  **/

    protected ExecutorService sslTaskWorker;
    protected ExecutorService groupExecutor;
    protected SSLAsynchronousChannelGroup group;
    protected Runnable task;
    protected Thread groupThread;


    /**  Variables for setting SNI host name  **/

    protected boolean setSNIHostName;
    protected String savedHostName;


    /**  Variables for initializing SSLContext **/

    protected boolean initSSLContext;
    protected KeyManager[] savedKeyMgr;
    protected TrustManager[] savedTrustMgr;
    protected SecureRandom savedSecureRand;


    /* ------------------------------------------------------------------------- */
    /* ---------------- CONSTRUCTOR -------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    // Constructor is package local. To create a SSLAsynchronousSocketChannel object
    // outside the package, use one the factory open methods.
    protected SSLAsynchronousSocketChannel() {

        isOpen = true;
        setSNIHostName = false;
        initSSLContext = false;
        connectionType = ConnectionType.UNKNOWN;

        netReadBufferState = new BufferState();
        writeLock = new Object();
        readLock = new Object();
        shutdownLock = new Object();

    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- OPEN --------------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /**
     * Factory method that returns an instance of class {@code SSLAsynchronousSocketChannel}.
     * The channel is opened implictly.
     * @param group
     *        The group to which the newly constructed channel should be bound. If group is null, a group
     *        is automatically instantiated. However, this approach is not recommended if a large
     *        number of connections is used.
     * @param useSSL
     *        If {@code true}, the SSL/TLS protocol is used. If {@code false},
     *        no SSL/TLS is used; in particular, all text is sent and received as plaintext.
     * @return An instance of class {@code SSLAsynchronousSocketChannel}
     * @throws ShutdownChannelGroupException
     *         If the channel group is shutdown
     * @throws IOException
     *         If an I/O error occurs
    */
    public static SSLAsynchronousSocketChannel open(SSLAsynchronousChannelGroup group, boolean useSSL)
                  throws IOException, ShutdownChannelGroupException  {

        logger.trace("start");

        SSLAsynchronousSocketChannel async = SSLAsynchronousSocketChannel.open(group);
        async.sslTaskWorker = null;
        async.connectionType = (useSSL == true ?  ConnectionType.SSL : ConnectionType.NON_SSL);
        return async;
    }


    /**
     * Factory method that returns an instance of class {@code SSLAsynchronousSocketChannel}.
     * The channel is opened implictly.
     * <p>
     * If the channel is connected to port 443 then
     * the SSL/TLS protocol is used automatically. For other ports, no SSL/TLS is used.
     * @param group
     *        The group to which the newly constructed channel should be bound. If group is null, a group
     *        is automatically instantiated. However, this approach is not recommended if a large
     *        number of connections is used.
     * @return An instance of class {@code SSLAsynchronousSocketChannel}
     * @throws ShutdownChannelGroupException
     *         If the channel group is shutdown
     * @throws IOException
     *         If an I/O error occurs
    */
    public static SSLAsynchronousSocketChannel open(SSLAsynchronousChannelGroup group) throws IOException, ShutdownChannelGroupException {
        
        logger.trace("start");
        
        if (group == null) {
            group = new SSLAsynchronousChannelGroup();
        }             
        
        SSLAsynchronousSocketChannel async = new SSLAsynchronousSocketChannelImpl(); 
        async.channel = java.nio.channels.AsynchronousSocketChannel.open(group.getGroup());  
        async.group = group;
        async.groupExecutor = group.getExecutor();
        group.add(async);
        
        return async;
    }


    /**
     * Tells whether or not this channel is open.
     *
     * @return
     * {@code true} if and only if this channel is open.
    */
    public boolean isOpen() { return isOpen; }


    /* ------------------------------------------------------------------------- */
    /* ---------------- CONNECT ------------------------------------------------ */
    /* ------------------------------------------------------------------------- */

    /** Connects this channel to the remote address.
     * <p>
     * If SSL/TLS is in use, the
     * handshake is part of the connect operation. The handler is invoked if the
     * connection is successfully established or if the connection can not be
     * established. If the connection cannot be established then the channel is closed.
     *
     * @param <A>
     *        The type of the attachement
     * @param remote
     *        The remote address to which the channel is connected
     * @param attachement
     *        An attachement which is available when the completion handler
     *        is invoked by the system; can be {@code null}
     * @param handler
     *        Completion handler that is invoked by the system after the
     *        connection has been established or if establishing the connection failed. The
     *        handler can be {@code null}.
     * @throws UnresolvedAddressException
     *         If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException
     *         If the type of the given remote address is not supported
     * @throws AlreadyConnectedException
     *         If this channel is already connected
     * @throws ConnectionPendingException If a connection operation is already in progress
     *         on this channel
     * @throws SecurityException
     *         If a security manager has been installed and it does not permit access to
     *         the given remote endpoint
    */

    // Connect and initial hanshake.
    // Note: The initial handshake requires an established connection, since
    // handshake data have to be exchanged with the peer. That's the reason why
    // the initial hanshake was integrated into the connect() method and not,
    // for instance, into the open() method.
    public abstract <A> void connect(SocketAddress remote, A attachement, CompletionHandler<Void, ? super A> handler)
                throws UnresolvedAddressException,
                       UnsupportedAddressTypeException,
                       AlreadyConnectedException,
                       ConnectionPendingException,
                       SecurityException; 


    /* ------------------------------------------------------------------------- */
    /* ---------------- WRITE -------------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /** Writes a sequence of bytes to this channel from the given buffer.
     * <p>
     * This method initiates an asynchronous write operation to write a sequence of bytes
     * to this channel from the given buffer. The handler parameter is a completion handler
     * that is invoked when the write
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes written. If SSL/TLS is in use, data from buffer src will
     * be encrypted before it is written to the network. The number of bytes written is the
     * amount of unencrypted application data that has been taken from buffer src
     * and has then been written in encrypted form to the network.
     * <p>
     * If a timeout is specified and the timeout elapses before the operation completes then
     * it completes with the exception InterruptedByTimeoutException. Where a timeout occurs,
     * and the implementation cannot guarantee that bytes have not been written, or will not
     * be written to the channel from the given buffer, then further attempts to write to the
     * channel will cause an unspecific runtime exception to be thrown.
     * @param <A>
     *        The type of the attachment
     * @param src
     *        The buffer from which bytes are to be retrieved.
     * @param timeout
     *        The maximum time for the I/O operation to complete. If a timeout occurs, method
     *        handler.failed is invoked with an InterruptedByTimeoutException exception.
     * @param unit
     *        The time unit of the {@code timeout} argument
     * @param attachement
     *        The object to attach to the I/O operation; can be {@code null}
     * @param handler
     *        The handler for consuming the result. The handler can be {@code null}.
     *
     * @throws IllegalArgumentException
     *         If the parameter buffer {@code src} is null
     * @throws NotYetConnectedException
     *         If this channel is not yet connected
     * @throws WritePendingException
     *         If a write operation is already in progress on this channel
     * @throws ShutdownChannelGroupException
     *         If the channel group has terminated
     * @throws SSLException
     *         If an error or illegal state occurs while encrypting the bytes written.
     *         This exception can only be thrown, if SSL/TLS is in use.
    */
    public abstract <A> void write(ByteBuffer src, long timeout, TimeUnit unit,
                            A attachement, CompletionHandler<Integer, ? super A> handler)
                    throws IllegalArgumentException,
                           NotYetConnectedException,
                           WritePendingException,
                           ShutdownChannelGroupException,
                           SSLException; 


    /* ------------------------------------------------------------------------- */
    /* ---------------- READ --------------------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> This method initiates an asynchronous read operation to read a
     * sequence of bytes from this channel into the given buffer. The {@code
     * handler} parameter is a completion handler that is invoked when the read
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes read or {@code -1} if no bytes could be
     * read because the channel has reached end-of-stream. If SSL/TLS is in use, data
     * received from the network are decrypted. The number of bytes read counts the
     * amount of decrypted data.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then the operation completes with the exception
     * InterruptedByTimeoutException. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffer, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * @param <A>
     *        The type of the attachment
     * @param dst
     *        The buffer into which bytes read from the channel are to be transferred.
     * @param timeout
     *        The maximum time for the I/O operation to complete. If a timeout occurs, method
     *        handler.failed is invoked with an InterruptedByTimeoutException exception.
     * @param unit
     *        The time unit of the {@code timeout} argument
     * @param attachement
     *        The object to attach to the I/O operation; can be {@code null}
     * @param handler
     *        The handler for consuming the result. The handler can be {@code null}.
     *
     * @throws IllegalArgumentException
     *         If the parameter {@code dst} is null
     * @throws NotYetConnectedException
     *         If this channel is not yet connected
     * @throws ReadPendingException
     *         If a read operation is already in progress on this channel
     * @throws ShutdownChannelGroupException
     *         If the channel group has terminated
    */

    public abstract <A> void read(final Buffer dst,
                         final long timeout,
                         final TimeUnit unit,
                         final A attachement,
                         final CompletionHandler<Integer, ? super A> handler)
                    throws IllegalArgumentException,
                           NotYetConnectedException,
                           ReadPendingException,
                           ShutdownChannelGroupException;

    /* ------------------------------------------------------------------------- */
    /* ---------------- HANDSHAKE ---------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /**
     * Starts a rehandshake or key update.
     * <p>
     * In TLS 1.2 and earlier, the method starts a rehandshake. In TLS 1.3 (where rehandshaking
     * no longer supported for security reasons), the method starts a key update.
     *
     * @throws OperationNotSupportedException
     *         If SSL/TLS is not in use
     * @throws NotYetConnectedException
     *         If this channel is not yet connected
     * @throws SSLException
     *         If an SSL exception occurs when handshaking is started.
    */
    public abstract void handshake() throws OperationNotSupportedException,
                                   NotYetConnectedException,
                                   SSLException;


    /* ------------------------------------------------------------------------- */
    /* ---------------- SHUTDOWN ----------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /**
     * Performs an orderly shutdown of the asynchronous channel.
     * If the connection is non-SSL, the operation just closes the
     * underlying {@code AsynchronousSocketChannel} channel. If the
     * connection is a SSL/TLS connection, a close_notify alert is sent
     * to the server. In TLS 1.2 or earlier (duplex-close), the server's 
     * close_notify alert is also received and afterwards the channel is closed. 
     * In TLS 1.3 (half-duplex-close), the channel is closed after sending the 
     * close_notify alert. 
    */
    public abstract void shutdown();  


    /**
     * Performs an orderly shutdown of the asynchronous channel and invokes a completion handler after shutdown has finished. 
     * If the connection is non-SSL, the operation just closes the 
     * underlying {@code AsynchronousSocketChannel} channel. If the 
     * connection is a SSL/TLS connection, a close_notify alert is sent 
     * to the server. In TLS 1.2 or earlier (duplex-close), the server's 
     * close_notify alert is also received and afterwards the channel is closed. 
     * In TLS 1.3 (half-duplex-close), the channel is closed after sending the 
     * close_notify alert. After the channel is closed, the handler's completion 
     * method is invoked. Since the shutdown is always successful (i.e. even if 
     * an exception is thrown during processing, the channel will be closed), the 
     * method {@code handler.failed()} will never be called.
     *
    * @param <A>
     *        The type of the attachment
     * @param timeout
     *        The maximum time for the I/O operation to complete. If a timeout occurs, method
     *        handler.failed is invoked with an InterruptedByTimeoutException exception.
     * @param unit
     *        The time unit of the {@code timeout} argument
     * @param attachement
     *        The object to attach to the I/O operation; can be {@code null}
     * @param handler
     *        The handler for consuming the result. The handler can be {@code null}.
    */
    public abstract <A> void shutdown(long timeout, TimeUnit unit, A attachement, CompletionHandler<Void, ? super A> handler);


    /* ------------------------------------------------------------------------- */
    /* ---------------- CLOSE -------------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /**
     * Close the channel without sending a close_notify to the peer in case of a SSL/TLS
     * connection.
    */
    public void close() {

        logger.trace("start");

        try {
            channel.close();
        } catch(IOException e) {
            logger.trace("ErrType: {} ErrMsg: {}", e.getClass().getName(), e.getMessage());
            // there is nothing we can do here more
        } finally {
            group.remove(this);
            isOpen = false;
        }
    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- NET BUFFERS -------------------------------------------- */
    /* ------------------------------------------------------------------------- */
    /**
     * Set buffers for writing data to and reading from the network.
     * <p>
     * This method is only relevant for SSL/TLS connections. In this case, these
     * buffers contain the encrypted data.
     * <p>
     * For instance, the buffer used in the
     * write method obtains plaintext data. During the writing procedure these
     * data will be decrypted and the decrypted data are written into the buffer
     * writeBuffer. Form there they are sent to the peer.
     * <p>Similarly, data received from the peer over a SSL/TLS connection are
     * written into the buffer readBuffer. Afterwards, they will be decrypted and
     * written into the buffer the was provided in the read method.
     * <p>If net buffers are not set explicitly by calling the setNetBuffers method,
     * the net buffers will be akquired implicitly by default. The reason why it is
     * possible to explicitly set the net buffers is to support the use of
     * a buffer pool for the net buffers.
     * @param writeBuffer
     *        Buffer from which handshake data and encrypted application data are sent
     *        to the peer.
     *
     * @param readBuffer
     *        Buffer in which handshake data and encrypted application data received
     *        from the peer are written in.
     */
    public void setNetBuffers(ByteBuffer writeBuffer, ByteBuffer readBuffer) {

      netWriteBuffer = writeBuffer;
      netWriteBuffer.clear();
      netReadBuffer  = readBuffer;
      netReadBuffer.clear();
      netReadBufferState.setEmpty(true);
    }


    /** Frees the net buffers by setting the corresponding attributes to null.
     *
     */
    public void freeNetBuffers() {

      netWriteBuffer = null;
      netReadBuffer  = null;
    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- setSNIHostName------------------------------------------ */
    /* ------------------------------------------------------------------------- */

    // Sets the host name for Server Name Indication (SNI) extension of TLS

    /**
     * Set the host name for Server Name Indication (SNI) extension of SSL/TLS
     *
     * @param hostName
     *        Host name for the SNI extension.
     *
     * @throws java.lang.NullPointerException
     *         If parameter {@code hostName} is null.
    */
    public void setSNIHostName(String hostName) {

        if (hostName == null) {
            throw new NullPointerException("hostName is null");
        }

        if (engine == null) {
            // defer setting of SNI host name till initialization of the engine
            setSNIHostName = true;
            savedHostName = hostName;
            return;
        }

        SSLParameters params = engine.getSSLParameters();
        List<SNIServerName> list = new ArrayList<>();
        list.add(new SNIHostName(hostName));
        params.setServerNames(list);
        engine.setSSLParameters(params);
    }

    /* ------------------------------------------------------------------------- */
    /* ---------------- initSSLContext ----------------------------------------- */
    /* ------------------------------------------------------------------------- */

    /** Initializes the SSLContext of the connetion.
     * <p>
     * If the method is not called, the
     * default implementations are used. Either of the first two parameters may be null
     * in which case the installed security providers will be searched for the highest
     * priority implementation of the appropriate factory. Likewise, the secure random
     * parameter may be null in which case the default implementation will be used.
     * <p>
     * Only the first instance of a particular key and/or trust manager implementation type
     * in the array is used. (For example, only the first javax.net.ssl.X509KeyManager in the
     * array will be used.)
    * @param keyMgr
    *        the sources of authentication keys or null
    * @param trustMgr
    *        the sources of peer authentication trust decisions or null
    * @param secureRand
    *        source of randomness for this generator or null
    */
    public void initSSLContext(KeyManager[] keyMgr, TrustManager[] trustMgr, SecureRandom secureRand) {

        initSSLContext = true;
        savedKeyMgr = keyMgr;
        savedTrustMgr = trustMgr;
        savedSecureRand = secureRand;
    }

    /**
     * @throws  IllegalArgumentException                {@inheritDoc}
     * @throws  ClosedChannelException                  {@inheritDoc}
     * @throws  IOException                             {@inheritDoc}
     */
    public abstract <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

}


