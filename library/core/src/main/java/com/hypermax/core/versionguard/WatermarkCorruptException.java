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
 * The watermark file exists but is not authentic, not complete or not bound to this app.
 *
 * <p>Only thrown when the stored data itself is at fault; a missing key or an unreadable file is
 * reported through {@link WatermarkUnavailableException} instead.</p>
 */
public class WatermarkCorruptException extends RuntimeException {

    public WatermarkCorruptException(String message) {
        super(message);
    }

    public WatermarkCorruptException(String message, Throwable cause) {
        super(message, cause);
    }
}
