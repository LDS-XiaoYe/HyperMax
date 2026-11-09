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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * AES-256-GCM key held by Android Keystore.
 *
 * <p>The key is generated on first use, never leaves the Keystore and is never written to
 * preferences or to a plain file. It requires no biometric or lock screen authentication, so the
 * check also works before the first unlock of a direct-boot aware device.</p>
 */
public final class KeystoreWatermarkKeyProvider implements WatermarkKeyProvider {

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final int KEY_SIZE = 256;

    private final String alias;
    private volatile SecretKey cachedKey;

    public KeystoreWatermarkKeyProvider(String alias) {
        this.alias = alias;
    }

    @Override
    public SecretKey getKey() {
        SecretKey key = cachedKey;
        if (key == null) {
            key = loadOrCreateKey();
            cachedKey = key;
        }
        return key;
    }

    private SecretKey loadOrCreateKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(alias, null);
            if (entry instanceof KeyStore.SecretKeyEntry secretKeyEntry) {
                return secretKeyEntry.getSecretKey();
            }
            return createKey();
        } catch (GeneralSecurityException | IOException error) {
            throw new WatermarkUnavailableException(
                "Unable to load the version watermark key from Android Keystore", error);
        }
    }

    private SecretKey createKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            generator.init(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setRandomizedEncryptionRequired(true)
                .build());
            return generator.generateKey();
        } catch (GeneralSecurityException error) {
            throw new WatermarkUnavailableException(
                "Unable to create the version watermark key in Android Keystore", error);
        }
    }
}
