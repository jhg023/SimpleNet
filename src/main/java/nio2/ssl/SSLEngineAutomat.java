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
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wraps and upwrapps data and performs handshaking and shutdown.
class SSLEngineAutomat {

    private static final Logger logger = LoggerFactory.getLogger(SSLEngineAutomat.class);

    /**  Objects of the Class  **/
    private final SSLEngine engine;

    /**  Constructor  **/
    public SSLEngineAutomat(SSLEngine engine) {
        this.engine = engine;
    }

    // This method is the central place that evaluates the state of the SSLEngine
    // and decides which action has to be taken.
    Response invoke(Action action,
                    Action command,
                    ByteBuffer appBuffer,
                    ByteBuffer netWriteBuffer,
                    ByteBuffer netReadBuffer,
                    BufferState netReadBufferState,
                    boolean internalWritePending,
                    boolean internalReadPending)
            throws SSLException {


        logger.trace("action: {} command: {}", action, command);

        try {

            // Determine handshake status
            SSLEngineResult result;
            SSLEngineResult.Status resultStatus;
            SSLEngineResult.HandshakeStatus hsStatus;
            if (command.equals(Action.REINVOKE)) {
                hsStatus = engine.getHandshakeStatus();
            } else {
                switch(command) {
                    case HANDSHAKE:
                    case CONNECT: // initial handshake
                        engine.beginHandshake();
                        hsStatus = engine.getHandshakeStatus();
                        break;
                    case WRITE:
                    case WRAP:
                        hsStatus = SSLEngineResult.HandshakeStatus.NEED_WRAP;
                        break;
                    case READ:
                    case UNWRAP:
                        hsStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
                        break;
                    case SHUTDOWN:
                        engine.closeOutbound();
                        hsStatus = engine.getHandshakeStatus();
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal action " + action);
                }
            } // end-if

            // marker for receiving of application data
            boolean appDataReceived = false;

            while (true) {
                logger.trace("hsStatus: {} engine.getHandshakeStatus: {} ", hsStatus, engine.getHandshakeStatus());

                // Possibilities:  NEED_WRAP, NEED_UNWRAP, NEED_TASK, FINISHED, NOT_HANDSHAKING
                switch (hsStatus) {

                    case NEED_WRAP:

                        // We need to be careful here: It may happen that a write process is running in the background.
                        // A scenario were this happens is:
                        //   1. a write process is running in background
                        //   2. the read handler completed;
                        //      it received a handshake message (say a key update request);
                        //      after unwrapping, a wrap is required
                        // While wrapping, netWriteBuffer is used concurrently in two threads. This has to be avoided
                        if (internalWritePending) {
                            logger.trace("internalWritePending: true");
                            // the currently processing command 'channel.write' will invoke the automat after
                            // completing -> we can drop the command
                            return Response.DROP;
                        }

                        logger.trace("Before wrap.  netWriteBuffer.pos: {} netWriteBuffer.limit: {}", netWriteBuffer.position(), netWriteBuffer.limit());

                        result = engine.wrap(appBuffer, netWriteBuffer);
                        hsStatus = result.getHandshakeStatus();
                        resultStatus = result.getStatus();

                        logger.trace("After wrap. status: {} hsStatus: {} netWriteBuffer.pos: {} ", resultStatus, hsStatus, netWriteBuffer.position());
                        logger.trace("After wrap. appBuffer.pos: {} appBuffer.limit: {} appBuffer.capacity: {}", appBuffer.position(), appBuffer.limit(), appBuffer.capacity());

                        // Possibilities:  OK, BUFFER_UNDERFLOW, BUFFER_OVERFLOW, CLOSED
                        switch (resultStatus) {

                            case OK:

                                // Continue in the following cases:
                                //   1. more app data -> continue wrapping
                                //   2. need more wrapping -> continue wrapping
                                //   3. client ChangeCipherSpec message for TLS 1.3 handshaking
                                // In all other cases the wrapped data are written to the channel
                                //
                                //    More details for case 3: In TLS 1.3 handshaking the following status sequence occurs:
                                //     need_wrap   (produce ClientHello)
                                //     need_unwrap (consume ServerHello)
                                //     need_wrap   (produce ChangeCipherSpec message)
                                //     need_unwrap (consume EncryptedExtensions)
                                //     need_unwrap (consume Certificate)
                                //     ..
                                //     need_unwrap
                                //     need_wrap   (produce Finished message)
                                //    For performance reasons we don't physically write the ChangeCipherSpec message (consisting of 5 Bytes)
                                //    into the channel, but write it together with the Finished message and the first application data

                                // case 1
                                //   We need to be careful: When handshaking, appBuffer is empty DUMMY, which therefore
                                //   has remaining data. That's why the action is checked here
                                if (  action.equals(Action.WRITE) && appBuffer.hasRemaining() ) {
                                    hsStatus = SSLEngineResult.HandshakeStatus.NEED_WRAP;
                                    continue;
                                }

                                // cases 2,3
                                if (  hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP  ||
                                      action.equals(Action.CONNECT) && !netReadBufferState.isEmpty() && netReadBuffer.hasRemaining()  ) {
                                    continue;
                                }

                                return Response.DO_WRITE;


                            case BUFFER_OVERFLOW:

                                // netWriteBuffer to small -> write data that are already wrapped into the channel
                                return Response.DO_WRITE;


                            case BUFFER_UNDERFLOW:  // Should not happen

                                throw new IllegalStateException("Inconsistent status " + resultStatus + " after NEED_WRAP");


                            // This happens in two cases: 
                            //   1. for shutdown by user after engine.closeOutbound and engine.wrap has been called
                            //   2. If unwrap reveals a close_notify alert, the hs statsu is need_wrap and after the wrap
                            //      the result status is CLOSED
                            case CLOSED:

                                return Response.DO_WRITE;


                            default: // should not happen

                                logger.error("hsStatus unknown: {}", hsStatus.toString());
                                throw new IllegalStateException("hsStatus unknown: " + hsStatus.toString());

                        } // end switch(resultStatus) for wrap


                    case NEED_UNWRAP:

                        // We need to be sure that no read is running in the background, since otherwise two threads could
                        // use netReadBuffer concurrently.
                        //   Example: In background user data are being read. The current task is a handshake() where the writeHandler has
                        //   just completed (i.e. ClientHello or key update request have been written) and now the automat says need_unwrap.
                        // Also note: Since the next completed read task will also call the automat's need_unwrap path, we can drop the current
                        // read task
                        if (internalReadPending) {
                          return Response.DROP;
                        }

                        logger.trace("Before unwrap. netReadBuffer.remaining: {} netReadBuffer.pos: {} netReadBuffer.limit: {} appBuffer.pos: {} internalReadPending: {}", netReadBuffer.remaining(), netReadBuffer.position(), netReadBuffer.limit(), appBuffer.position(), internalReadPending);

                        // Ensure data have been read before unwrapping
                        if ( netReadBufferState.isEmpty() || !netReadBuffer.hasRemaining() ) {
                            return Response.DO_READ;
                        }

                        result = engine.unwrap(netReadBuffer, appBuffer);
                        hsStatus = result.getHandshakeStatus();
                        resultStatus = result.getStatus();
                        int bytesProduced = result.bytesProduced();

                        if (bytesProduced > 0) {
                            appDataReceived = true;
                        }

                        logger.trace("After unwrap. status: {} hsStatus: {} bytesProduced: {}", resultStatus, hsStatus, bytesProduced);
                        logger.trace("After unwrap. netReadBuffer.remaining: {} netReadBuffer.pos: {} netReadBuffer.limit: {} appBuffer.pos: {}", netReadBuffer.remaining(), netReadBuffer.position(), netReadBuffer.limit(), appBuffer.position());


                        // Possibilities:  OK, BUFFER_UNDERFLOW, BUFFER_OVERFLOW, CLOSED
                        switch (resultStatus) {

                            case OK:

                                // if application data have been received, return application data to user
                                //   Note: the following can happen: server sends application data, followed by a close alert.
                                //   In such a case, first, we return the application data to the user; if the user reads again,
                                //   the remaining data are unwrapped, throwing to a closed connection exception.
                                if ( appDataReceived ) {
                                    return Response.APP_DATA;

                                // in all other cases we evaluate the handshake status
                                } else {
                                    continue;
                                }


                            case BUFFER_UNDERFLOW:

                                return Response.DO_READ;


                            case BUFFER_OVERFLOW:

                                return Response.ACQUIRE_APP_STORAGE;


                            case CLOSED:

                                // happens, if a close_notify alert has been initially sent by the peer
                                if (hsStatus.equals(SSLEngineResult.HandshakeStatus.NEED_WRAP)) {
                                    return Response.CLOSED;

                                // happens, if we shutdown an have just received the peers close_notify alert as a response
                                } else if (action.equals(Action.SHUTDOWN)) {
                                    netReadBuffer.clear();
                                    netReadBufferState.setEmpty(true);
                                    return Response.OK;

                                } else {
                                    logger.error("case need_unwrap: connection closed by server");
                                    throw new SSLException("Connection has been closed by the server");
                                }


                            default: // should not happen
                                logger.error("unwrap: illegal state for resultStatus");
                                throw new IllegalStateException("unwrap: illegal state for resultStatus");

                        }  // end switch(resultStatus) for unwrap


                    case NEED_TASK:

                        return Response.DO_TASK;


                    case FINISHED:

                        if ( action.equals(Action.CONNECT) || action.equals(Action.HANDSHAKE) ) {
                            return Response.OK;

                        // happens in TLS 1.3: here we can have the following scenario:
                        //  1. initial handshake finished
                        //  2. server sends new session ticket message
                        //  3. write user data (HTTP Get request)
                        //  4. after data are written call read()
                        //  5. after read is completed: new session ticket mesage has been read
                        //     status sequence: NEED_UNWRAP -> OK, FINISHED, bytesProduced: 0
                        } else if ( !netReadBufferState.isEmpty() && netReadBuffer.hasRemaining() ) {
                            hsStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
                            continue;

                        // same case as before, but now buffer netReadBuffer has no more data -> read again
                        } else if ( action.equals(Action.READ) && hsStatus == SSLEngineResult.HandshakeStatus.FINISHED &&
                                   (netReadBufferState.isEmpty() || !netReadBuffer.hasRemaining()) ) {
                            return Response.DO_READ;

                        } else { // should not happen
                            logger.error("Illegal handshake status: FINISHED while not connecting");
                            throw new IllegalStateException("Illegal handshake status: FINISHED while not connecting");
                        }


                    case NOT_HANDSHAKING: // should not happen

                        // happens, for instance, if we reinvoke after application data have been successfully wrapped
                        return Response.OK;


                    default: // should not happen

                        logger.error("Illegal handshake status: {}", hsStatus.toString());
                        throw new IllegalStateException("Illegal handshake status: " + hsStatus.toString());

                } // end-switch(handshakeStatus)

            } // end-while

        } catch(Exception e) {
            // map exception to SSLException
            logger.error("Exception was thrown. ex: {} msg: {}", e.getClass().getName(), e.getMessage());
            throw new SSLException(e.getMessage(), e);
        }

    }

}

