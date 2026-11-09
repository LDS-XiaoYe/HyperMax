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

/**
 * Seals and opens the watermark payload.
 *
 * <p>Implementations must authenticate the payload; a plaintext or best-effort fallback is not an
 * acceptable implementation.</p>
 */
public interface WatermarkCipher {

    /**
     * @throws WatermarkUnavailableException when the key is missing or unusable
     */
    byte[] encrypt(byte[] plaintext);

    /**
     * @throws WatermarkCorruptException     when the payload is not authentic or malformed
     * @throws WatermarkUnavailableException when the key is missing or unusable
     */
    byte[] decrypt(byte[] payload);
}
