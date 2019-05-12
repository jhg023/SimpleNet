package simplenet.utility.exposed;

import simplenet.Client;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;

/**
 * This interface allows end users to specify their own cryptography schemes for Clients.
 *
 * Relevant methods in {@link Client}:
 * {@link Client#setEncryptionFunction(CryptoFunction)}
 * {@link Client#setDecryptionFunction(CryptoFunction)}
 *
 */
public interface CryptoFunction {
    /**
     * The functional method for this interface, allowing the user to change how the input Cipher operates
     * @param cipher The performing {@link Cipher}
     * @param data The data to perform on
     * @return The data after transformations have been performed by the Cipher
     * @throws GeneralSecurityException if there was a problem with the performing Cipher
     */
    byte[] perform(Cipher cipher, byte[] data) throws GeneralSecurityException;

    /**
     * Overloaded method for SimpleNet to take control of the thrown GeneralSecurityException, if present.
     * @param cipher The performing {@link Cipher}
     * @param data The data to perform on
     * @param consumer The {@link Consumer} responsible for handling the potential exception
     * @return The data after transformations have been performed by the Cipher
     */
    default byte[] performExceptionally(Cipher cipher, byte[] data, Consumer<GeneralSecurityException> consumer){
        try {
            return perform(cipher, data);
        } catch (GeneralSecurityException e) {
            consumer.accept(e);
        }
        return data;
    }
}
