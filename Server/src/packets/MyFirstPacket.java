package packets;

import main.Main;
import simplenet.packet.incoming.*;

import java.nio.*;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousSocketChannel;

public final class MyFirstPacket implements IncomingPacket {

	@Override
	public void read(AsynchronousSocketChannel channel, ByteBuffer payload) {
		int length = payload.get();

		byte[] message = new byte[length];

		payload.get(message);

		System.out.println(new String(message));
	}

}
