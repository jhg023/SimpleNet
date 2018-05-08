package main;

import simplenet.server.Server;

public final class Main {

	public static void main(String[] args) {
		Server server = new Server(channel -> {});

		server.bind("localhost", 43_594);

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
