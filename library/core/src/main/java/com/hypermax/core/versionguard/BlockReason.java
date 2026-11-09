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
 * Reason why the version check refused to let the app continue.
 */
public enum BlockReason {
    /** Installed versionCode is lower than the highest one recorded on this device. */
    DOWNGRADE,
    /** The watermark exists but cannot be authenticated, parsed or bound to this app. */
    WATERMARK_CORRUPT,
    /** The watermark file or the watermark key could not be read. */
    WATERMARK_READ_FAILED,
    /** A new watermark could not be persisted, so the check cannot be trusted. */
    WATERMARK_WRITE_FAILED
}
