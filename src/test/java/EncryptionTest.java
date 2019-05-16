import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simplenet.Client;
import simplenet.Server;
import simplenet.packet.Packet;
import simplenet.utility.exposed.cryptography.CryptographicFunction;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests encryption and decryption of specific {@link Cipher} algorithms.
 *
 * @author Hwiggy <https://github.com/Hwiggy>
 */
class EncryptionTest {
    
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
        startTest(server, 9000, a, b, c, d, Cipher::doFinal, Cipher::doFinal);
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
        startTest(server, 9001, a, b, c, d, Cipher::update, Cipher::update);
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
        startTest(server, 9003, a, b, c, d, Cipher::doFinal, Cipher::doFinal);
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
                           CryptographicFunction decryption) throws InterruptedException {
        server.onConnect(client -> {
            client.setEncryption(serverEncryption, encryption);
            client.setDecryption(serverDecryption, decryption);
            
            client.readBytes(128, it -> {
                assertArrayEquals(encryptBytes, it);
                latch.countDown();
            });
            
            client.readString(it -> {
                assertEquals("Hello World!", it);
                latch.countDown();
            });
            
            client.readLong(it -> {
                assertEquals(54735436752L, it);
                latch.countDown();
            });
            
            client.readDouble(it -> {
                assertEquals(23.1231, it);
                latch.countDown();
            });
            
            client.readByte(it -> {
                assertEquals(0x00, it);
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
                    .writeAndFlush(client);
        });
        
        client.connect("localhost", port);
    }
    
}