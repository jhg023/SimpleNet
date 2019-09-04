/*
 * MIT License
 *
 * Copyright (c) 2019 Jacob Glickman
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

import com.github.simplenet.Client;
import com.github.simplenet.Server;
import com.github.simplenet.packet.Packet;
import com.github.simplenet.utility.exposed.cryptography.CryptographicFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests encryption and decryption of specific {@link Cipher} algorithms.
 *
 * @author Hwiggy <https://github.com/Hwiggy>
 */
class EncryptionTest {

    private static final CryptographicFunction DO_FINAL = (cipher, data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.doFinal(data, output);
        return output;
    };

    private static final CryptographicFunction UPDATE = (cipher, data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.update(data, output);
        return output;
    };

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
    void testPaddingAES() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException {
        final Server server = new Server();
        server.bind("localhost", 9000);
        final String AES = "AES/CBC/PKCS5Padding";
        final Cipher
                a = Cipher.getInstance(AES),
                b = Cipher.getInstance(AES),
                c = Cipher.getInstance(AES),
                d = Cipher.getInstance(AES);
        initCiphers(a, b, c, d);
        startTest(server, 9000, a, b, c, d, DO_FINAL, DO_FINAL);
    }
    
    @Test
    void testNoPaddingAES() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException {
        final Server server = new Server();
        server.bind("localhost", 9001);
        final String AES = "AES/CFB8/NoPadding";
        final Cipher
                a = Cipher.getInstance(AES),
                b = Cipher.getInstance(AES),
                c = Cipher.getInstance(AES),
                d = Cipher.getInstance(AES);
        initCiphers(a, b, c, d);
        startTest(server, 9001, a, b, c, d, UPDATE, UPDATE);
    }
    
    @Test
    void testNoPaddingAESFinal() throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException {
        final Server server = new Server();
        server.bind("localhost", 9003);
        final String AES = "AES/CFB8/NoPadding";
        final Cipher
                a = Cipher.getInstance(AES),
                b = Cipher.getInstance(AES),
                c = Cipher.getInstance(AES),
                d = Cipher.getInstance(AES);
        initCiphers(a, b, c, d);
        startTest(server, 9003, a, b, c, d, DO_FINAL, DO_FINAL);
    }
    
    private void initCiphers(
            Cipher serverEncryption,
            Cipher serverDecryption,
            Cipher clientEncryption,
            Cipher clientDecryption
    ) throws InvalidAlgorithmParameterException, InvalidKeyException {
        final IvParameterSpec ivSpec = new IvParameterSpec(sharedSecret);
        final SecretKeySpec keySpec = new SecretKeySpec(sharedSecret, "AES");
        serverEncryption.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        serverDecryption.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        clientEncryption.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        clientDecryption.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    }
    
    private void startTest(Server server, int port, Cipher serverEncryption, Cipher serverDecryption,
            Cipher clientEncryption, Cipher clientDecryption, CryptographicFunction encryption,
                           CryptographicFunction decryption) {
        server.onConnect(client -> {
            client.setEncryption(serverEncryption, encryption);
            client.setDecryption(serverDecryption, decryption);

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
        
        final Client client = new Client();
        
        client.onConnect(() -> {
            client.setEncryption(clientEncryption, encryption);
            client.setDecryption(clientDecryption, decryption);
            Packet.builder()
                    .putBytes(encryptBytes)
                    .putString("Hello World!")
                    .putLong(54735436752L)
                    .putDouble(23.1231)
                    .putByte(0x00)
                    .queueAndFlush(client);
        });
        
        client.connect("localhost", port);
    }
    
}