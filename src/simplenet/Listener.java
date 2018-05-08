package simplenet;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * An {@code abstract} class, essentially defining a {@link CompletionHandler}.
 *
 * @param <R> The result of the {@link CompletionHandler}.
 * @param <A> The attachment of the {@link CompletionHandler}.
 */
public abstract class Listener<R, A extends Receiver> implements CompletionHandler<R, A> {

    /**
     * The method that will be called when the
     * {@link CompletionHandler} succeeds.
     *
     * @param result     The result of the {@link CompletionHandler}.
     * @param attachment The attachment of the {@link CompletionHandler}.
     */
    protected abstract void onCompletion(R result, A attachment);

    /**
     * Gets the {@link AsynchronousSocketChannel} from either the
     * result of the {@link CompletionHandler} or its attachment.
     *
     * @param result     The result of the {@link CompletionHandler}.
     * @param attachment The attachment of the {@link CompletionHandler}.
     * @return The {@link AsynchronousSocketChannel} to read/write to/from.
     */
    protected abstract AsynchronousSocketChannel getChannel(R result, A attachment);

    @Override
    public void completed(R result, A receiver) {
        onCompletion(result, receiver);

        var channel = getChannel(result, receiver);

        channel.read(receiver.getBuffer(), receiver, new CompletionHandler<>() {
            private int size;

            @Override
            public void completed(Integer result, A receiver) {
                size += result;

                var buffer = receiver.getBuffer().flip();
                var queue = receiver.getQueue();
                var peek = queue.poll();

                if (peek == null) {
                    channel.read(buffer.flip().limit(buffer.capacity()), receiver, this);
                    return;
                }

                while (size >= peek.getKey()) {
                    peek.getValue().accept(receiver.getBuffer());

                    size -= peek.getKey();

                    if ((peek = queue.pollLast()) == null) {
                        break;
                    }
                }

                if (peek != null) {
                    queue.addFirst(peek);
                }

                if (size > 0) {
                    receiver.getBuffer().compact();
                } else {
                    receiver.getBuffer().flip();
                }

                channel.read(receiver.getBuffer(), receiver, this);
            }

            @Override
            public void failed(Throwable t, A receiver) {
                // TODO: Fire disconnect listener.

                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void failed(Throwable t, A receiver) {
        t.printStackTrace();
    }

}
