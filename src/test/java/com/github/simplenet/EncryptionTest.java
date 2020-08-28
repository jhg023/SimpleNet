package com.github.simplenet;/*
 * MIT License
 *
 * Copyright (c) 2020 Jacob Glickman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.github.simplenet.packet.Packet;
import com.github.simplenet.utility.exposed.cryptography.CryptographicFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.github.simplenet.utility.exposed.cryptography.CryptographicFunction.DO_FINAL;
import static com.github.simplenet.utility.exposed.cryptography.CryptographicFunction.UPDATE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests encryption and decryption of specific {@link Cipher} algorithms.
 *
 * @author Hwiggy <https://github.com/Hwiggy>
 */
final class EncryptionTest {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final byte[] sharedSecret = new byte[16];

    private final byte[] encryptBytes = new byte[128];

    private CountDownLatch latch;

    @BeforeEach
    void beforeEach() {
        random.nextBytes(sharedSecret);
        random.nextBytes(encryptBytes);
        latch = new CountDownLatch(5);
    }

    @AfterEach
    void afterEach() throws InterruptedException {
        if (!latch.await(1L, TimeUnit.SECONDS)) {
            Assertions.fail();
        }
    }

    @Test
    void testPaddingAES() throws GeneralSecurityException {
        startTest(9000, "AES/CBC/PKCS5Padding", DO_FINAL, DO_FINAL);
    }

    @Test
    void testNoPaddingAES() throws GeneralSecurityException {
        startTest(9001, "AES/CFB8/NoPadding", UPDATE, UPDATE);
    }

    @Test
    void testNoPaddingAESFinal() throws GeneralSecurityException {
        startTest(9002, "AES/CFB8/NoPadding", DO_FINAL, DO_FINAL);
    }

    private Cipher[] initCiphers(String cipher) throws GeneralSecurityException {
        var ivSpec = new IvParameterSpec(sharedSecret);
        var keySpec = new SecretKeySpec(sharedSecret, "AES");

        var serverEncryption = Cipher.getInstance(cipher);
        var serverDecryption = Cipher.getInstance(cipher);
        var clientEncryption = Cipher.getInstance(cipher);
        var clientDecryption = Cipher.getInstance(cipher);

        serverEncryption.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        serverDecryption.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        clientEncryption.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        clientDecryption.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return new Cipher[] { serverEncryption, serverDecryption, clientEncryption, clientDecryption };
    }

    private void startTest(int port, String algorithm, CryptographicFunction encryption,
                           CryptographicFunction decryption) throws GeneralSecurityException {
        var server = new Server();

        server.bind("localhost", port);

        var ciphers = initCiphers(algorithm);

        server.onConnect(client -> {
            client.setEncryption(ciphers[0], encryption);
            client.setDecryption(ciphers[1], decryption);

            client.read(128 + 14 + 8 + 8 + 1, buffer -> {
                byte[] data = new byte[128];
                buffer.get(data);
                assertArrayEquals(encryptBytes, data);
                latch.countDown();

                int length = buffer.getShort();
                data = new byte[length];
                buffer.get(data);
                assertEquals("Hello World!", new String(data, StandardCharsets.UTF_8));
                latch.countDown();

                assertEquals(54735436752L, buffer.getLong());
                latch.countDown();

                assertEquals(23.1231, buffer.getDouble());
                latch.countDown();

                assertEquals(0x00, buffer.get());
                latch.countDown();
            });
        });

        var client = new Client();

        client.onConnect(() -> {
            client.setEncryption(ciphers[2], encryption);
            client.setDecryption(ciphers[3], decryption);
            Packet.builder().putBytes(encryptBytes).putString("Hello World!").putLong(54735436752L).putDouble(23.1231)
                .putByte(0x00).queueAndFlush(client);
        });

        client.connect("localhost", port);
    }
}
