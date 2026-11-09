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

import android.app.Activity;
import android.content.Intent;

import com.hypermax.common.log.AndroidLog;

/**
 * Single interception point for the whole app.
 *
 * <p>Activities that own business pages ask {@link #intercept(Activity)} before they create their
 * content, and the launcher entry asks {@link #resolve(Activity, Callback)} so it can wait for the
 * background check without blocking the main thread.</p>
 */
public final class VersionGate {

    private static final String TAG = "VersionGate";

    public interface Callback {

        /** The installed version is verified; the caller may open its business page. */
        void onResolved(VersionGuardState state);
    }

    private VersionGate() {
    }

    /**
     * @return true when the caller must stop immediately, either because a block screen was opened
     * or because the check is still running and the page may not be shown yet
     */
    public static boolean intercept(Activity activity) {
        VersionGuardState state = VersionGuardManager.state();
        if (state.isAllowed()) {
            return false;
        }
        if (state instanceof VersionGuardState.Blocked blocked) {
            showBlocked(activity, blocked);
            return true;
        }
        if (!VersionGuardManager.isInstalled()) {
            showBlocked(activity, unverifiedState());
            return true;
        }
        // 校验尚未结束：不在业务页里等待，交回启动页统一决策
        AndroidLog.w(TAG, "Version check is still running, deferring "
            + activity.getClass().getSimpleName() + " to the launcher entry.");
        Intent launcherIntent = activity.getPackageManager()
            .getLaunchIntentForPackage(activity.getPackageName());
        if (launcherIntent != null) {
            activity.startActivity(launcherIntent);
        }
        activity.finish();
        return true;
    }

    /**
     * Waits for the first terminal state and either lets the caller continue or shows the block
     * screen. The callback is invoked on the main thread.
     */
    public static void resolve(Activity activity, Callback callback) {
        VersionGuardState state = VersionGuardManager.state();
        if (state.isAllowed()) {
            callback.onResolved(state);
            return;
        }
        if (state instanceof VersionGuardState.Blocked blocked) {
            showBlocked(activity, blocked);
            return;
        }
        if (!VersionGuardManager.isInstalled()) {
            showBlocked(activity, unverifiedState());
            return;
        }
        VersionGuardManager.awaitState(resolved -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            if (resolved instanceof VersionGuardState.Blocked blocked) {
                showBlocked(activity, blocked);
            } else if (resolved.isAllowed()) {
                callback.onResolved(resolved);
            }
        });
    }

    private static void showBlocked(Activity activity, VersionGuardState.Blocked state) {
        AndroidLog.w(TAG, "Blocking " + activity.getClass().getSimpleName() + ": " + state);
        VersionBlockActivity.start(activity, state);
        activity.finish();
    }

    /**
     * Used when the guard was never installed, which means the watermark cannot be verified at all.
     * The app stays closed rather than running without a check.
     */
    private static VersionGuardState.Blocked unverifiedState() {
        AndroidLog.e(TAG, "VersionGuard is not installed, the watermark cannot be verified.");
        return VersionGuardState.blocked(
            BlockReason.WATERMARK_READ_FAILED, VersionGuard.UNKNOWN_VERSION_CODE, null);
    }
}
