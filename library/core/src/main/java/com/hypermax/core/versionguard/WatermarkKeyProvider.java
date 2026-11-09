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

import javax.crypto.SecretKey;

/**
 * Supplies the AES key used by {@link FramedAesGcmCipher}.
 *
 * <p>The production implementation reads the key from Android Keystore; tests use an in-memory key.
 * The key is never persisted by the guard itself.</p>
 */
public interface WatermarkKeyProvider {

    /**
     * @throws WatermarkUnavailableException when the key cannot be created or loaded
     */
    SecretKey getKey();
}
