package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class ShortPacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		System.out.println("SHORT PACKET: " + payload.getShort());
	}

}
