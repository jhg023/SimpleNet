package main;

import simplenet.packet.Packet;
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
             * When 1 byte arrives from any client, switch on it.
             * If the byte equals 1, then "request" 4 bytes and
             * print them as an int whenever they arrive.
             *
             * Because `readAlways` is used, the server will always
             * attempt to read one byte.
             */
            client.read(1, header -> {
                System.out.println("A");

                client.read(1, nested ->  {
                    System.out.println("B");

                    client.read(1, payload ->  {
                        System.out.println("C");
                    });

                    client.read(1, payload ->  {
                        System.out.println("D");
                    });
                });

                client.read(1, nested ->  {
                    System.out.println("E");
                });
            });

            Packet.builder()
                  .putInt(6969)
                  .writeAndFlush(client);
        });

        // Register one disconnection listener.
        server.onDisconnect(client -> System.out.println(client + " has disconnected!"));

        while (true) {
			Thread.onSpinWait();
		}
	}

}
