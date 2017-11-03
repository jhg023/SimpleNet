package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class BytePacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		System.out.println("BYTE PACKET: " + payload.get());
	}

}
