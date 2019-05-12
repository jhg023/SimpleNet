import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import simplenet.Client;
import simplenet.Server;
import simplenet.packet.Packet;
import simplenet.utility.exposed.CryptoFunction;

public class EncryptionTest {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final byte[] sharedSecret = new byte[16];
    
    private final byte[] encryptBytes = new byte[128];
    
    @Test
    void init() {
        random.nextBytes(sharedSecret);
        random.nextBytes(encryptBytes);
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
    void testPaddingAESUpdate() throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException {
        /*
            Take a look at Cipher#update to see why this test doesnt read completely
            TL;DR: Block Cipher needs padding, I'm too lazy to write padding by hand.
         */
        final Server server = new Server();
        server.bind("localhost", 9002);
        final String AES = "AES/CBC/PKCS5Padding";
        final Cipher
                a = Cipher.getInstance(AES),
                b = Cipher.getInstance(AES),
                c = Cipher.getInstance(AES),
                d = Cipher.getInstance(AES);
        initCiphers(a, b, c, d);
        startTest(server, 9002, a, b, c, d, Cipher::update, Cipher::update);
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
    
    private void startTest(
            Server server,
            int port,
            Cipher serverEncryption,
            Cipher serverDecryption,
            Cipher clientEncryption,
            Cipher clientDecryption,
            CryptoFunction encryption,
            CryptoFunction decryption
    ) throws InterruptedException {
         server.onConnect(client -> {
            client.setEncryption(serverEncryption, encryption);
            client.setDecryption(serverDecryption, decryption);
            client.readBytes(128, it -> {
                System.out.println("Reading Bytes");
                assert(Arrays.equals(it, encryptBytes));
            });
            client.readString(it -> {
                System.out.println("Reading String");
                assert ("Hello World!".equals(it));
            });
            client.readLong(it -> {
                System.out.println("Reading Long");
                assert (it == 54735436752L);
            });
            client.readDouble(it -> {
                System.out.println("Reading Double");
                assert (it == 23.1231);
            });
            client.readByte(it -> {
                System.out.println("Reading Byte");
                assert (it == 0x00);
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
        Thread.sleep(2000);
    }
    
}
