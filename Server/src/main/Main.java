package main;

import packets.*;
import simplenet.server.*;

public class Main {

	public static void main(String[] args) {
		Server server = new Server(channel -> {});

		server.bind("localhost", 43594);

		server.register(0, new MyFirstPacket());

		while (true) {
			Thread.onSpinWait();
		}
	}

}
