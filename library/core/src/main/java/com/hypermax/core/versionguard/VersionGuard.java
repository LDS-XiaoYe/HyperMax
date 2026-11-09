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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Local, offline downgrade guard.
 *
 * <p>A single check is <em>read, decrypt, compare, update if needed, commit state</em>, executed
 * serially so concurrent callers cannot interleave a check with a write. Once a check has committed
 * a state the same process keeps it; a later check re-reads the persisted watermark and therefore
 * converges on the same verdict instead of silently lifting a block.</p>
 *
 * <p>This class deliberately has no Android dependency: the Keystore, the file system and the
 * PackageManager are injected, so the decision logic is testable on a plain JVM.</p>
 */
public final class VersionGuard {

    /** Sentinel used when the installed versionCode itself cannot be read. */
    public static final long UNKNOWN_VERSION_CODE = 0L;

    private static final ExecutorService CHECK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "hypermax-version-guard");
        thread.setDaemon(true);
        return thread;
    });

    /** Logging boundary; the guard itself never touches a log framework. */
    public interface Logger {

        void message(String text);

        void failure(String text, @Nullable Throwable error);
    }

    /** Installed version of the app, as reported by the platform package manager. */
    public static final class PackageVersion {

        public final long versionCode;
        public final String versionName;

        public PackageVersion(long versionCode, String versionName) {
            this.versionCode = versionCode;
            this.versionName = versionName;
        }
    }

    public interface PackageVersionSource {

        PackageVersion readInstalledVersion();
    }

    private final String packageName;
    private final PackageVersionSource versionSource;
    private final WatermarkCipher cipher;
    private final WatermarkStore store;
    private final Executor callbackExecutor;
    private final Logger logger;

    private final CopyOnWriteArrayList<Consumer<VersionGuardState>> waiters = new CopyOnWriteArrayList<>();
    private final AtomicBoolean checkScheduled = new AtomicBoolean();
    /**
     * Guards the waiter list and every write of {@link #state}. It is never held while the
     * watermark file or the Keystore is touched, so reading the state from the main thread cannot
     * be blocked by a running check.
     */
    private final Object stateLock = new Object();

    private volatile VersionGuardState state = VersionGuardState.UNINITIALIZED;

    public VersionGuard(String packageName, PackageVersionSource versionSource, WatermarkCipher cipher,
                        WatermarkStore store, Executor callbackExecutor, @Nullable Logger logger) {
        this.packageName = packageName;
        this.versionSource = versionSource;
        this.cipher = cipher;
        this.store = store;
        this.callbackExecutor = callbackExecutor;
        this.logger = logger;
    }

    public VersionGuardState state() {
        return state;
    }

    /**
     * Starts one background check. Repeated calls while a check is already queued are ignored so a
     * burst of activity creations cannot spawn parallel verifications.
     */
    public void requestCheck() {
        if (!checkScheduled.compareAndSet(false, true)) {
            return;
        }
        synchronized (stateLock) {
            if (!state.isResolved()) {
                state = VersionGuardState.CHECKING;
            }
        }
        CHECK_EXECUTOR.execute(() -> {
            try {
                checkNow();
            } finally {
                checkScheduled.set(false);
            }
        });
    }

    /**
     * Runs one check and commits its result.
     *
     * <p>Package private on purpose: the crypto and file work must never run on the main thread, so
     * the only production entry point is {@link #requestCheck()}. Tests call it directly.</p>
     */
    VersionGuardState checkNow() {
        synchronized (this) {
            VersionGuardState next;
            try {
                next = verify();
            } catch (RuntimeException error) {
                failure("Unexpected failure while verifying the version watermark", error);
                next = VersionGuardState.blocked(
                    BlockReason.WATERMARK_READ_FAILED, UNKNOWN_VERSION_CODE, null);
            }
            return publish(next);
        }
    }

    /**
     * Registers a one-shot callback that receives the next terminal state. The callback always runs
     * through the executor supplied at construction, never on the caller's thread.
     */
    public void awaitState(Consumer<VersionGuardState> callback) {
        VersionGuardState current = state;
        if (!current.isResolved()) {
            synchronized (stateLock) {
                current = state;
                if (!current.isResolved()) {
                    waiters.add(callback);
                    return;
                }
            }
        }
        final VersionGuardState resolved = current;
        callbackExecutor.execute(() -> callback.accept(resolved));
    }

    private VersionGuardState verify() {
        PackageVersion installed;
        try {
            installed = versionSource.readInstalledVersion();
        } catch (RuntimeException error) {
            failure("Unable to read the installed package version", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_READ_FAILED, UNKNOWN_VERSION_CODE, null);
        }
        if (installed.versionCode <= 0) {
            failure("Package manager reported an invalid versionCode " + installed.versionCode, null);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_READ_FAILED, installed.versionCode, null);
        }

        byte[] payload;
        try {
            payload = store.read();
        } catch (IOException error) {
            failure("Unable to read the version watermark", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_READ_FAILED, installed.versionCode, null);
        }

        if (payload == null) {
            message("No version watermark yet, recording versionCode " + installed.versionCode);
            return writeWatermark(installed);
        }

        VersionWatermark watermark;
        try {
            watermark = VersionWatermark.decode(cipher.decrypt(payload));
            watermark.requireCompatibleWith(packageName);
        } catch (WatermarkCorruptException error) {
            failure("Version watermark is corrupt", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_CORRUPT, installed.versionCode, null);
        } catch (RuntimeException error) {
            failure("Unable to decrypt the version watermark", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_READ_FAILED, installed.versionCode, null);
        }

        if (installed.versionCode < watermark.maxVersionCode) {
            message("Version downgrade detected: installed " + installed.versionCode +
                " is lower than the recorded " + watermark.maxVersionCode);
            return VersionGuardState.blocked(
                BlockReason.DOWNGRADE, installed.versionCode, watermark.maxVersionCode);
        }
        if (installed.versionCode == watermark.maxVersionCode) {
            return VersionGuardState.allowed(installed.versionCode);
        }

        message("Version upgraded from " + watermark.maxVersionCode + " to " + installed.versionCode);
        return writeWatermark(installed);
    }

    private VersionGuardState writeWatermark(PackageVersion installed) {
        VersionWatermark watermark = VersionWatermark.create(
            packageName, installed.versionCode, installed.versionName, System.currentTimeMillis());

        byte[] payload;
        try {
            payload = cipher.encrypt(watermark.encode());
        } catch (RuntimeException error) {
            failure("Unable to encrypt the version watermark", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_WRITE_FAILED, installed.versionCode, null);
        }

        try {
            store.write(payload);
        } catch (IOException error) {
            failure("Unable to persist the version watermark", error);
            return VersionGuardState.blocked(
                BlockReason.WATERMARK_WRITE_FAILED, installed.versionCode, null);
        }
        return VersionGuardState.allowed(installed.versionCode);
    }

    private VersionGuardState publish(VersionGuardState next) {
        List<Consumer<VersionGuardState>> pending = null;
        synchronized (stateLock) {
            state = next;
            if (next.isResolved() && !waiters.isEmpty()) {
                pending = new ArrayList<>(waiters);
                waiters.clear();
            }
        }
        if (pending != null) {
            for (Consumer<VersionGuardState> waiter : pending) {
                callbackExecutor.execute(() -> waiter.accept(next));
            }
        }
        return next;
    }

    private void message(@NonNull String text) {
        if (logger != null) {
            logger.message(text);
        }
    }

    private void failure(@NonNull String text, @Nullable Throwable error) {
        if (logger != null) {
            logger.failure(text, error);
        }
    }
}
