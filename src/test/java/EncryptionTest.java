import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import simplenet.Client;
import simplenet.Server;
import simplenet.packet.Packet;

public class EncryptionTest {
    private final String RSA = "RSA/ECB/PKCS1Padding", AES = "AES/CBC/PKCS5Padding";
    private final Server server = new Server();
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final byte[] sharedSecret = new byte[16];
    private KeyPairGenerator keyPairGenerator;
    private Cipher rsaClient, rsaServer;
    private Cipher serverEncryption, serverDecryption;
    private Cipher clientEncryption, clientDecryption;
    private final byte[] encryptBytes = new byte[128];
    
    @Test
    void init() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InterruptedException {
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        random.nextBytes(sharedSecret);
        
        final KeyPair rsaKeys = keyPairGenerator.generateKeyPair();
        final PrivateKey privateKey = rsaKeys.getPrivate();
        final PublicKey publicKey = rsaKeys.getPublic();
        
        rsaClient = Cipher.getInstance(RSA);
        rsaServer = Cipher.getInstance(RSA);
        rsaClient.init(Cipher.ENCRYPT_MODE, publicKey);
        rsaServer.init(Cipher.DECRYPT_MODE, privateKey);
        
        byte[] encrypted = rsaClient.doFinal(sharedSecret),
                decrypted = rsaServer.doFinal(encrypted);
        assert(Arrays.equals(sharedSecret, decrypted));
        final IvParameterSpec spec = new IvParameterSpec(decrypted);
        
        serverEncryption = Cipher.getInstance(AES);
        serverDecryption = Cipher.getInstance(AES);
        clientEncryption = Cipher.getInstance(AES);
        clientDecryption = Cipher.getInstance(AES);
        serverEncryption.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(decrypted, "AES"), spec);
        serverDecryption.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decrypted, "AES"), spec);
        clientEncryption.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(decrypted, "AES"), spec);
        clientDecryption.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decrypted, "AES"), spec);
        
        server.onConnect(client -> {
            client.setEncryption(serverEncryption);
            client.setDecryption(serverDecryption);
            client.readBytes(128, it -> System.out.println(Arrays.toString(it)));
            client.readString(System.out::println);
            
        });
        
        server.bind("localhost", 9000);
        random.nextBytes(encryptBytes);
        System.out.println(Arrays.toString(encryptBytes));
        
        final Client client = new Client();
        client.onConnect(() -> {
            client.setEncryption(clientEncryption);
            client.setDecryption(clientDecryption);
            Packet.builder().putBytes(encryptBytes).putString("Hello World!").writeAndFlush(client);
        });
        client.connect("localhost", 9000);
        Thread.sleep(5000);
    }
    
}
