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

import androidx.annotation.Nullable;

/**
 * Immutable result of a version check.
 *
 * <p>Only {@link Allowed} means the business features may be opened. Every other state, including
 * the transient ones, must be treated as "not verified yet".</p>
 */
public abstract class VersionGuardState {

    /** No check has been requested yet. */
    public static final VersionGuardState UNINITIALIZED = new Uninitialized();
    /** A check is running; the result is not known yet. */
    public static final VersionGuardState CHECKING = new Checking();

    private VersionGuardState() {
    }

    public static Allowed allowed(long currentVersionCode) {
        return new Allowed(currentVersionCode);
    }

    public static Blocked blocked(BlockReason reason, long currentVersionCode,
                                  @Nullable Long requiredVersionCode) {
        return new Blocked(reason, currentVersionCode, requiredVersionCode);
    }

    /** The installed version is verified, the business features may be used. */
    public boolean isAllowed() {
        return false;
    }

    /** A terminal state, either {@link Allowed} or {@link Blocked}. */
    public boolean isResolved() {
        return false;
    }

    public static final class Uninitialized extends VersionGuardState {

        private Uninitialized() {
        }

        @Override
        public String toString() {
            return "Uninitialized";
        }
    }

    public static final class Checking extends VersionGuardState {

        private Checking() {
        }

        @Override
        public String toString() {
            return "Checking";
        }
    }

    public static final class Allowed extends VersionGuardState {

        public final long currentVersionCode;

        private Allowed(long currentVersionCode) {
            this.currentVersionCode = currentVersionCode;
        }

        @Override
        public boolean isAllowed() {
            return true;
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        @Override
        public String toString() {
            return "Allowed{currentVersionCode=" + currentVersionCode + '}';
        }
    }

    public static final class Blocked extends VersionGuardState {

        public final BlockReason reason;
        public final long currentVersionCode;
        /** Highest versionCode ever recorded locally, null when the watermark could not be read. */
        @Nullable
        public final Long requiredVersionCode;

        private Blocked(BlockReason reason, long currentVersionCode,
                        @Nullable Long requiredVersionCode) {
            this.reason = reason;
            this.currentVersionCode = currentVersionCode;
            this.requiredVersionCode = requiredVersionCode;
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        @Override
        public String toString() {
            return "Blocked{reason=" + reason +
                ", currentVersionCode=" + currentVersionCode +
                ", requiredVersionCode=" + requiredVersionCode + '}';
        }
    }
}
