package main;

import simplenet.server.Server;

public final class Main {

	public static void main(String[] args) {
        // Instantiate a new Server.
        var server = new Server();

        // Bind the server to an address and port.
        server.bind("localhost", 43594);

        // Register one connection listener.
        server.onConnect(client -> {
            System.out.println(client + " has connected!");

            /*
             * When one byte arrives from the client, switch on it.
             * If the byte equals 1, then "request" an int and
             * print it when it arrives.
             *
             * Because `readByteAlways` is used, the server will always
             * attempt to read one byte.
             */
            client.readByteAlways(b -> {
                switch (b) {
                    case 1:
                        client.readInt(System.out::println);
                }
            });
        });

        // Register one disconnection listener.
        server.onDisconnect(client -> System.out.println(client + " has disconnected!"));

        while (true) {
			Thread.onSpinWait();
		}
	}

}
