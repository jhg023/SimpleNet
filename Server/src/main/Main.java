package main;

import simplenet.server.Server;

public final class Main {

	public static void main(String[] args) {
        // Instantiate a new Server.
        var server = new Server();

        // Bind the server to an address and port.
        server.bind("localhost", 43594);

        // Register one connection and disconnection listener.
        server.onConnect(channel -> System.out.println(channel + " has connected!"));
        server.onDisconnect(channel -> System.out.println(channel + " has disconnected!"));

        /*
         * When 1 byte arrives from any client, switch on it.
         * If the byte equals 1, then "request" 4 bytes and
         * print them as an int whenever they arrive.
         *
         * Because `readAlways` is used, the server will always
         * attempt to read one byte.
         */
        server.readAlways(1, header -> {
            switch (header.get()) {
                case 1:
                    server.read(4, payload -> System.out.println(payload.getInt()));
            }
        });

        while (true) {
			Thread.onSpinWait();
		}
	}

}
