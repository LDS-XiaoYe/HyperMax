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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hypermax.common.log.AndroidLog;

import java.io.File;
import java.util.function.Consumer;

/**
 * Process-wide entry point of the version guard.
 *
 * <p>Installed by {@code Application} before any activity starts; it wires the guard to Android
 * Keystore, to {@code noBackupFilesDir} and to the package manager, then queues one background
 * check. Every activity talks to this class, never to the crypto or file layer directly.</p>
 */
public final class VersionGuardManager {

    /** Watermark file name inside {@code noBackupFilesDir}. */
    public static final String WATERMARK_FILE_NAME = "hypermax.wm";
    /** Keystore alias suffix, appended to the application id. */
    public static final String KEY_ALIAS_SUFFIX = ".hypermax.watermark.v1";

    private static final String TAG = "VersionGuard";
    private static final Object INSTALL_LOCK = new Object();

    private static volatile VersionGuardManager sInstance;

    private final VersionGuard guard;

    private VersionGuardManager(VersionGuard guard) {
        this.guard = guard;
    }

    /**
     * Wires the guard for this process and starts the first check. Repeated calls are ignored.
     */
    public static void install(Context context) {
        Context appContext = context.getApplicationContext() == null
            ? context
            : context.getApplicationContext();
        synchronized (INSTALL_LOCK) {
            if (sInstance != null) {
                return;
            }
            String packageName = appContext.getPackageName();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            VersionGuard guard = new VersionGuard(
                packageName,
                new AndroidPackageVersionSource(appContext, packageName),
                new FramedAesGcmCipher(
                    packageName,
                    new KeystoreWatermarkKeyProvider(packageName + KEY_ALIAS_SUFFIX)
                ),
                new AtomicFileWatermarkStore(
                    new File(appContext.getNoBackupFilesDir(), WATERMARK_FILE_NAME)
                ),
                mainHandler::post,
                new VersionGuard.Logger() {
                    @Override
                    public void message(String text) {
                        AndroidLog.d(TAG, text);
                    }

                    @Override
                    public void failure(String text, Throwable error) {
                        AndroidLog.e(TAG, text, error);
                    }
                }
            );
            sInstance = new VersionGuardManager(guard);
            AndroidLog.i(TAG, "Installed for " + packageName + ", checking the local watermark.");
            guard.requestCheck();
        }
    }

    public static boolean isInstalled() {
        return sInstance != null;
    }

    public static VersionGuardState state() {
        VersionGuardManager instance = sInstance;
        return instance == null ? VersionGuardState.UNINITIALIZED : instance.guard.state();
    }

    /**
     * One-shot callback for the next terminal state, delivered on the main thread.
     */
    public static void awaitState(Consumer<VersionGuardState> callback) {
        VersionGuardManager instance = sInstance;
        if (instance == null) {
            AndroidLog.e(TAG, "awaitState() called before install(); the state stays unverified.");
            return;
        }
        instance.guard.awaitState(callback);
    }
}
