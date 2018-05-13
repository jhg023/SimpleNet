package simplenet;

import simplenet.client.Client;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

public class Listener implements CompletionHandler<Integer, Client> {

    private static final Listener INSTANCE = new Listener();

    @Override
    public void completed(Integer result, Client client) {
        var buffer = client.getBuffer().flip();
        var queue = client.getQueue();
        var peek = queue.pollLast();
        var stack = client.getStack();

        if (peek == null) {
            client.getChannel().read(buffer.flip().limit(buffer.capacity()), client, this);
            return;
        }

        client.setPrepend(true);

        while (client.getBuffer().remaining() >= peek.getKey()) {
            peek.getValue().accept(client.getBuffer());

            while (!stack.isEmpty()) {
                queue.offer(stack.poll());
            }

            if ((peek = queue.pollLast()) == null) {
                break;
            }
        }

        client.setPrepend(false);

        if (peek != null) {
            queue.addFirst(peek);
        }

        if (client.getBuffer().hasRemaining()) {
            client.getBuffer().compact();
        } else {
            client.getBuffer().flip();
        }

        client.getChannel().read(client.getBuffer(), client, this);
    }

    @Override
    public void failed(Throwable t, Client client) {
        client.getDisconnectListeners().forEach(Runnable::run);
        client.close();
    }

    public static Listener getInstance() {
        return INSTANCE;
    }

}
