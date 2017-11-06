package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class MyFirstPacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		int length = payload.get();

		byte[] message = new byte[length];

		payload.get(message);

		System.out.println(new String(message));
	}

}
