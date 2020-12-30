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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


// Class used as attachement for the internal completion handlers.
class Context {

    private Action action;

    // this attribute indicates if the read handler or write handler has completed
    // we need to save this information in the context because otherweise it could be
    // lost when do_task is processed in a separate task.
    private Handler completedHandler;
    private boolean invokeAutomat;
    private long timeout;
    private TimeUnit timeUnit;
    private final CompletionHandler externalHandler;
    private final Object externalAttachement;
    private final ByteBuffer externalBuffer;

    // When read() is invoked, the bytes read into the app channel, as
    // delivered to the user's completion handler is computed as
    // externalBuffer.position() - startExternalBuffer, where
    // startExternalBuffer is initialized below
    private int startExternalBuffer;
    private ByteBuffer appBuffer;
    private AtomicInteger taskCounter;

    // Constructor
    Context(Action action,
            CompletionHandler handler,
            Object attachement,
            ByteBuffer buffer) {

        this.action = action;
        this.completedHandler = Handler.NONE;
        this.externalHandler = handler;
        this.externalAttachement = attachement;
        this.externalBuffer = buffer;
        this.appBuffer = buffer;
        this.invokeAutomat = false;

        if (buffer != null) {
            this.startExternalBuffer = buffer.position();
        }
    }

    // Getter
    Action getAction() { return action; }
    Handler getCompletedHandler() { return completedHandler; }
    boolean getInvokeAutomat() { return invokeAutomat; }
    long getTimeout() { return timeout; }
    TimeUnit getTimeUnit() { return timeUnit; }
    CompletionHandler getExternalHandler() { return externalHandler; }
    Object getExternalAttachement() { return externalAttachement; }
    ByteBuffer getExternalBuffer() { return externalBuffer; }
    int getStartExternalBuffer() { return startExternalBuffer; }
    ByteBuffer getAppBuffer() { return appBuffer; }
    AtomicInteger getTaskCounter() { return taskCounter; }

    // Setter
    void setAction(Action action) { this.action = action; }
    void setCompletedHandler(Handler completedHandler) { this.completedHandler = completedHandler; }
    void setTimeout(long timeout) { this.timeout = timeout; }
    void setTimeUnit(TimeUnit unit) { this.timeUnit = unit; }
    void setInvokeAutomat(boolean invoke) { this.invokeAutomat = invoke; }
    void setAppBuffer(ByteBuffer appBuffer) { this.appBuffer = appBuffer; }
    void setTaskCounter(AtomicInteger counter) { this.taskCounter = counter; }
}

