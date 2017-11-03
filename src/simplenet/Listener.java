package simplenet;

import simplenet.client.*;
import simplenet.packet.*;
import simplenet.packet.incoming.*;
import simplenet.server.*;
import simplenet.utility.*;

import java.nio.*;
import java.nio.channels.*;

/**
 * An {@code abstract} class, essentially defining a {@link CompletionHandler}
 * to handle {@link IncomingPacket}s on both a {@link Client} and the
 * {@link Server} with a unique attachment, {@code A}, for each.
 *
 * @param <R>
 *     The result of the {@link CompletionHandler}, depends on the backing
 *     Java implementation (i.e. no choice of object type unlike attachment).
 * @param <A>
 *     The attachment that <strong>must</strong> hold a {@link Packetable}.
 *     {@code A} could be a {@link Tuple}, so {@code Listener<A extends Packetable>}
 *     is not an option.
 */
public abstract class Listener<R, A> implements CompletionHandler<R, A> {

	/**
	 * An {@code int} representing the opcode
	 * of the current {@link IncomingPacket}
	 * being preprocessed.
	 */
	private int currentOpcode = -1;

	/**
	 * An {@code int} representing the length
	 * of the current {@link IncomingPacket}
	 * being preprocessed.
	 */
	private int currentLength = -1;

	/**
	 * An {@code int} representing the amount
	 * of payload data (excluding the header)
	 * within the backing {@link ByteBuffer}
	 * at any given time.
	 */
	private int currentSize;

	/**
	 * Allocate a new {@link ByteBuffer} for the opcode
	 * and length, each being {@code 1} byte.
	 */
	private ByteBuffer buffer = ByteBuffer.allocate(2);

	/**
	 * The method that will be called when the
	 * {@link CompletionHandler} succeeds.
	 *
	 * @param a
	 *      The attachment that must hold a {@link Packetable}.
	 */
	protected abstract void onCompletion(A a);

	/**
	 * Gets the {@link AsynchronousSocketChannel} from either the
	 * result of the {@link CompletionHandler} or its attachment.
	 *
	 * @param result
	 *      The result of the {@link CompletionHandler}.
	 * @param attachment
	 *      The attachment of the {@link CompletionHandler}.
	 * @return
	 *      The {@link AsynchronousSocketChannel} to read/write to/from.
	 */
	protected abstract AsynchronousSocketChannel getChannel(R result, A attachment);

	/**
	 * Gets the {@link Packetable} that will determine
	 * where the {@link IncomingPacket} will be processed.
	 *
	 * @param attachment
	 *      The attachment that must hold a {@link Packetable}.
	 * @return
	 *      The {@link Packetable} within the attachment.
	 */
	protected abstract Packetable getPacketable(A attachment);

	@Override
	public void completed(R result, A attachment) {
		onCompletion(attachment);

		AsynchronousSocketChannel channel = getChannel(result, attachment);

		/*
		 * Handle this connection by continuously attempting
		 * to asynchronously read from the channel.
		 */
		channel.read(buffer, attachment, new CompletionHandler<>() {
			private boolean needToFlip = true;

			@Override
			public void completed(Integer result, A attachment) {
				/*
				 * Because we're about to read from a buffer
				 * that was just written to, flip it.
				 */
				if (needToFlip) {
					currentSize += result;

					buffer.flip();
				} else {
					needToFlip = true;
				}

				attemptReadOpcode();
				attemptReadLength(result - 1);

				if (attemptProcessPacket(getPacketable(attachment))) {
					needToFlip = false;

					completed(buffer.remaining(), attachment);
					return;
				}

				if (currentSize > 0) {
					buffer.compact();
				} else {
				   /*
				    * Because the buffer does not
				    * have any data left over, flip
				    * it for writing.
				    */
					buffer.flip();
				}

				/*
				 * Attempt to read more information into
				 * this buffer at a later time, eventually
				 * returning to this handler.
				 */
				channel.read(buffer, attachment, this);
			}

			@Override
			public void failed(Throwable t, A a) {
				t.printStackTrace();
			}
		});
	}

	@Override
	public void failed(Throwable t, A a) {
		t.printStackTrace();
	}

	/**
	 * Attempt to read a single {@code byte} from
	 * the {@link ByteBuffer} passed to this method,
	 * representing an opcode of an {@link IncomingPacket}.
	 */
	private void attemptReadOpcode() {
		if (currentOpcode != -1) {
			return;
		}

		currentOpcode = buffer.get() & 0xFF;

		currentSize--;

		// System.out.println("OPCODE: " + currentOpcode + " SIZE: " + currentSize);
	}

	/**
	 * Attempt to read a single {@code byte} from
	 * the {@link ByteBuffer} passed to this method,
	 * representing the length of an {@link IncomingPacket}.
	 *
	 * If the length of the {@link IncomingPacket} is longer
	 * than any {@link Packet} we've seen previously, then
	 * allocate a larger buffer, passing in any
	 * data left over from the original buffer.
	 */
	private void attemptReadLength(int result) {
		if (currentOpcode == -1) {
			return;
		}

		if (currentLength == -1 && result >= 1) {
			currentLength = buffer.get() & 0xFF;

			if (currentLength > buffer.capacity()) {
				buffer = ByteBuffer.allocateDirect(currentLength).put(buffer).flip();
			}

			currentSize--;

			//System.out.println("LENGTH: " + currentLength + " SIZE: " + currentSize + " " + buffer);
		}
	}

	/**
	 * Attempt to process the {@link IncomingPacket} formed
	 * by the data in the {@link ByteBuffer}.
	 *
	 * @param packetable
	 *      Either a {@link Client} or the {@link Server},
	 *      depending on which entity is attempting to
	 *      process the {@link IncomingPacket}.
	 */
	private boolean attemptProcessPacket(Packetable packetable) {
		if (currentOpcode == -1 || currentLength == -1) {
			return false;
		}

		if (currentSize >= currentLength) {
			currentSize -= currentLength;

			packetable.getPackets()[currentOpcode].read(buffer);

			resetCurrentAttributes();

			return buffer.hasRemaining();
		}

		return false;
	}

	/**
	 * Resets both {@code currentOpcode} and
	 * {@code currentLength} to their default
	 * values of {@code -1} to denote that a
	 * new {@link IncomingPacket} will arrive.
	 */
	private void resetCurrentAttributes() {
		currentOpcode = currentLength = -1;
	}

}
