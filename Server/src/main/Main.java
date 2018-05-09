package main;

import simplenet.server.Server;

public final class Main {

	public static void main(String[] args) {
	    // Instantiate a new Server.
		Server server = new Server();

        // Register one connection and disconnection listener.
        server.onConnect(channel -> System.out.println(channel + " has connected!"));
        server.onDisconnect(channel -> System.out.println(channel + " has disconnected!"));

        // Bind the server to an address and port.
        server.bind("localhost", 43_594);

        /*
         * When 1 byte arrives from any client, switch on it.
         * If the byte equals 1, then "request" 4 bytes and
         * print them as an int whenever they arrive.
         *
         * Because `readAlways` is used, the server will always
         * attempt to read one byte.
         */
        server.read(8, payload -> System.out.println(payload.getLong()));

        server.readAlways(1, header -> {
            int length = header.get();

            byte[] b = new byte[length];

            server.read(length, payload -> {
                payload.get(b);
                System.out.println(new String(b));
            });
        });

		while (true) {
			Thread.onSpinWait();
		}
	}

}
