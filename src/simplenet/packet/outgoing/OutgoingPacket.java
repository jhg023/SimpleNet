package simplenet.packet.outgoing;

import simplenet.client.*;
import simplenet.packet.*;
import simplenet.server.*;

import java.nio.*;
import java.util.*;
import java.util.function.*;

/**
 * A {@link Packet} that will be sent from a
 * {@link Client} to the {@link Server} or
 * vice versa.
 */
public final class OutgoingPacket implements Packet {

	/**
	 * An {@code int} representing the amount
	 * of bytes that this {@link OutgoingPacket}
	 * will send.
	 */
	private int size;

	/**
	 * A unique identifier of an {@link OutgoingPacket}.
	 */
	private final int opcode;

	/**
	 * A {@link Queue} that lazily writes data to the
	 * backing {@link ByteBuffer}.
	 */
	private final Queue<Consumer<ByteBuffer>> queue = new ArrayDeque<>();

	/**
	 * Instantiates a raw {@link Packet}
	 * with a specific opcode.
	 *
	 * @param opcode
	 *      This {@link Packet}'s identifier;
	 *      Must range from {@code 0} to {@code 255}.
	 */
	public OutgoingPacket(int opcode) {
		this.opcode = opcode;
	}

	/**
	 * Writes a single {@code byte} to this
	 * {@link Packet}'s payload.
	 *
	 * @param b
	 *      An {@code int} for ease-of-use,
	 *      but internally down-casted to a
	 *      {@code byte}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putByte(int b) {
		size++;

		queue.offer(payload -> payload.put((byte) b));
		return this;
	}

	/**
	 * Writes a variable amount of
	 * {@code byte}s to this {@link Packet}'s
	 * payload.
	 *
	 * @param src
	 *      An {@code int} array for ease-of-use,
	 *      but each element is internally down-casted
	 *      to a {@code byte}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putBytes(int... src) {
		size += src.length;

		queue.offer(payload -> {
			for (int b : src) {
				payload.put((byte) b);
			}
		});

		return this;
	}

	/**
	 * Writes a single {@code char} to this
	 * {@link Packet}'s payload.
	 *
	 * @param c
	 *      A {@code char}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putChar(char c) {
		size += 2;

		queue.offer(payload -> payload.putChar(c));
		return this;
	}

	/**
	 * Writes a single {@code double} to this
	 * {@link Packet}'s payload.
	 *
	 * @param d
	 *      A {@code double}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putDouble(double d) {
		size += 8;

		queue.offer(payload -> payload.putDouble(d));
		return this;
	}

	/**
	 * Writes a single {@code float} to this
	 * {@link Packet}'s payload.
	 *
	 * @param f
	 *      A {@code float}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putFloat(float f) {
		size += 4;

		queue.offer(payload -> payload.putFloat(f));
		return this;
	}

	/**
	 * Writes a single {@code int} to this
	 * {@link Packet}'s payload.
	 *
	 * @param i
	 *      A {@code int}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putInt(int i) {
		size += 4;

		queue.offer(payload -> payload.putInt(i));
		return this;
	}

	/**
	 * Writes a single {@code long} to this
	 * {@link Packet}'s payload.
	 *
	 * @param l
	 *      A {@code long}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putLong(long l) {
		size += 8;

		queue.offer(payload -> payload.putLong(l));
		return this;
	}

	/**
	 * Writes a single {@code short} to this
	 * {@link Packet}'s payload.
	 *
	 * @param s
	 *      A {@code short}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putShort(int s) {
		size += 2;

		queue.offer(payload -> payload.putShort((short) s));
		return this;
	}

	/**
	 * Transmits this {@link OutgoingPacket} to
	 * one (or more) {@link Client}(s).
	 *
	 * @param clients
	 *      A variable amount of {@link Client}s.
	 */
	public void send(Client... clients) {
		/*
		 * Allocate a new buffer with the size of
		 * the data being added, as well as an extra
		 * two bytes to account for the opcode and the length.
		 *
		 * TODO: Give each Client their own direct ByteBuffer.
		 */
		ByteBuffer payload = ByteBuffer.allocateDirect(size + 2);

		/*
		 * Write the opcode to the buffer.
		 */
		payload.put((byte) opcode);

		/*
		 * Write the length to the buffer.
		 */
		payload.put((byte) size);

		/*
		 * Add the rest of the data to the buffer.
		 */
		queue.forEach(consumer -> consumer.accept(payload));

		/*
		 * Flip the buffer so the client can immediately
		 * read it on arrival.
		 */
		payload.flip();

		/*
		 * Write the buffer to the channels.
		 */
		for (Client client : clients) {
			client.getChannel().write(payload);
		}
	}

}
