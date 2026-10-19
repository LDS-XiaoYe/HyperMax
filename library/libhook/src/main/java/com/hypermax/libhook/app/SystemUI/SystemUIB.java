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
package com.hypermax.libhook.app.SystemUI;

import static com.hypermax.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.hypermax.common.utils.PrefsBridge;
import com.hypermax.libhook.appbase.systemui.StatusBarActionBootstrap;
import com.hypermax.libhook.base.BaseLoad;
import com.hypermax.libhook.rules.home.dock.DockGlassRendererSystemUI;
import com.hypermax.libhook.rules.home.gesture.HomeExtraGesturesOS4;
import com.hypermax.libhook.rules.home.gesture.HomeQuickSwitchSystemUIOS4;
import com.hypermax.libhook.rules.systemui.controlcenter.NotificationWeather;
import com.hypermax.libhook.rules.systemui.controlcenter.OldWeather;
import com.hypermax.libhook.rules.systemui.navigation.BackGestureWidthOS4;
import com.hypermax.libhook.rules.systemui.plugin.NewPluginHelperKt;
import com.hypermax.libhook.rules.systemui.statusbar.clock.StatusBarClockNew;
import com.hypermax.libhook.utils.hookapi.systemui.MiuiStub;

/**
 * HyperMax SystemUI entry for HyperOS 4 (SDK 36+).
 *
 * <p>HyperMax is a launcher-focused module, so this entry only carries the pieces that must live
 * in the SystemUI process: the unlock/return motion hand-off for the launcher Dock backdrop,
 * the launcher-side gestures that SystemUI dispatches, the back-gesture hot zone, and the
 * status-bar clock/weather surfaces that the launcher layout depends on.</p>
 *
 * <p>Every other SystemUI rule inherited from the upstream project is intentionally not installed
 * here; those settings no longer exist in this module's UI.</p>
 */
@HookBase(targetPackage = "com.android.systemui", minSdk = 36)
public class SystemUIB extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        // Launcher Dock / gesture bridge. These must exist before the launcher asks for a frame.
        initHook(new HomeExtraGesturesOS4(), isHyperOSVersion(4f));
        initHook(new HomeQuickSwitchSystemUIOS4(), isHyperOSVersion(4f));
        initHook(new DockGlassRendererSystemUI(), isHyperOSVersion(4f));
        MiuiStub.createHook();

        // Back-gesture hot zone shared between launcher and SystemUI on OS4.
        initHook(new BackGestureWidthOS4(), isHyperOSVersion(4f));

        // Plugin bridge and launcher action dispatch (home_gesture_enable).
        initHook(NewPluginHelperKt.INSTANCE);
        initHook(new StatusBarActionBootstrap(), PrefsBridge.getBoolean("home_gesture_enable")
            || com.hypermax.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion(4f));

        // Status-bar clock (seconds hand) and weather chip on the control center.
        initHook(StatusBarClockNew.INSTANCE, PrefsBridge.getBoolean("system_ui_statusbar_clock_all_status_enable"));
        initHook(OldWeather.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_show_weather"));
    }
}
