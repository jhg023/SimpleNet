/*
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
package com.github.simplenet.utility.exposed.cryptography;

import com.github.simplenet.Client;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

/**
 * Provides users with the capability to specify their own cryptographic scheme(s) for encrypting/decrypting data.
 *
 * @see Client#setEncryptionCipher(Cipher)
 * @see Client#setDecryptionCipher(Cipher)
 */
@FunctionalInterface
public interface CryptographicFunction {

    CryptographicFunction DO_FINAL = (Cipher cipher, ByteBuffer data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.doFinal(data, output);
        return output;
    };

    CryptographicFunction UPDATE = (Cipher cipher, ByteBuffer data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.update(data, output);
        return output;
    };

    /**
     * Performs encryption/decryption of a {@link ByteBuffer} given a {@link Cipher cipher}.
     *
     * @param cipher The {@link Cipher} used to encrypt/decrypt the specified {@link ByteBuffer}.
     * @param buffer The {@link ByteBuffer} to encrypt/decrypt.
     * @return The modified data after it has been encrypted/decrypted by the {@link Cipher cipher}
     * @throws GeneralSecurityException if an exception occurred while encrypting/decrypting.
     */
    ByteBuffer apply(Cipher cipher, ByteBuffer buffer) throws GeneralSecurityException;
   
}
