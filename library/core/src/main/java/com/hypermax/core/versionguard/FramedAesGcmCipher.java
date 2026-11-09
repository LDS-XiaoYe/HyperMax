/*
 * This file is part of HyperMax.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.hypermax.core.versionguard;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM framing used for the watermark file.
 *
 * <pre>
 * Magic      4 bytes   "HMW1"
 * IV        12 bytes   fresh random per encryption
 * Ciphertext N bytes   AES-GCM ciphertext followed by the 128-bit tag
 * </pre>
 *
 * <p>Additional authenticated data is {@code <applicationId>|1|HMW1}; the trailing {@code 1} belongs
 * to the file format, not to the payload schema, so a payload schema change does not invalidate
 * existing files.</p>
 */
public final class FramedAesGcmCipher implements WatermarkCipher {

    private static final byte[] MAGIC = {'H', 'M', 'W', '1'};
    private static final int MAGIC_LENGTH = MAGIC.length;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 16;
    private static final int MIN_LENGTH = MAGIC_LENGTH + IV_LENGTH + TAG_LENGTH;
    private static final int TAG_BITS = TAG_LENGTH * 8;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String AAD_FORMAT_VERSION = "1";

    private final String packageName;
    private final WatermarkKeyProvider keyProvider;
    private final SecureRandom random = new SecureRandom();

    public FramedAesGcmCipher(String packageName, WatermarkKeyProvider keyProvider) {
        this.packageName = packageName;
        this.keyProvider = keyProvider;
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad());
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] framed = new byte[MAGIC_LENGTH + IV_LENGTH + ciphertext.length];
            System.arraycopy(MAGIC, 0, framed, 0, MAGIC_LENGTH);
            System.arraycopy(iv, 0, framed, MAGIC_LENGTH, IV_LENGTH);
            System.arraycopy(ciphertext, 0, framed, MAGIC_LENGTH + IV_LENGTH, ciphertext.length);
            return framed;
        } catch (GeneralSecurityException error) {
            throw new WatermarkUnavailableException("Unable to encrypt the version watermark", error);
        }
    }

    @Override
    public byte[] decrypt(byte[] payload) {
        if (payload == null || payload.length < MIN_LENGTH) {
            throw new WatermarkCorruptException("Watermark payload is truncated");
        }
        for (int index = 0; index < MAGIC_LENGTH; index++) {
            if (payload[index] != MAGIC[index]) {
                throw new WatermarkCorruptException("Watermark magic mismatch");
            }
        }
        byte[] iv = Arrays.copyOfRange(payload, MAGIC_LENGTH, MAGIC_LENGTH + IV_LENGTH);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyProvider.getKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad());
            return cipher.doFinal(payload, MAGIC_LENGTH + IV_LENGTH,
                payload.length - MAGIC_LENGTH - IV_LENGTH);
        } catch (AEADBadTagException | IllegalBlockSizeException error) {
            throw new WatermarkCorruptException("Watermark authentication failed", error);
        } catch (GeneralSecurityException error) {
            throw new WatermarkUnavailableException("Unable to decrypt the version watermark", error);
        }
    }

    private byte[] aad() {
        return (packageName + '|' + AAD_FORMAT_VERSION + "|HMW1").getBytes(StandardCharsets.UTF_8);
    }
}
