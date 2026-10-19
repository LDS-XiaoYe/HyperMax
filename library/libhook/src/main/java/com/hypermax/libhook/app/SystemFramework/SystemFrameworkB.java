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
package com.hypermax.libhook.app.SystemFramework;

import com.hchen.database.HookBase;
import com.hypermax.common.utils.PrefsBridge;
import com.hypermax.libhook.appbase.systemframework.GlobalActionBootstrap;
import com.hypermax.libhook.appbase.systemframework.ModulePackageTrust;
import com.hypermax.libhook.base.BaseLoad;

/**
 * HyperMax system_server entry for HyperOS 4.
 *
 * <p>The launcher on OS4 is a Flutter/Rust process with no Java Activity, so its Dock backdrop is
 * owned by WindowManager instead of the launcher itself. {@code HomeDockWindow} installs that
 * backdrop and serves the layout configuration over the WMS transaction channel; the two
 * bootstraps below are the minimum needed for the module package to be trusted and for launcher
 * gesture actions to be dispatched.</p>
 */
@HookBase(targetPackage = "system", minSdk = 36)
public class SystemFrameworkB extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        // HYOS/Flutter launcher has no Java Activity entry. WMS owns its backdrop instead.
        initHook(new com.hypermax.libhook.rules.home.dock.HomeDockWindow(),
            com.hypermax.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion(4f));

        initHook(new ModulePackageTrust(), PrefsBridge.getBoolean("home_gesture_enable"));
        initHook(new GlobalActionBootstrap(), PrefsBridge.getBoolean("home_gesture_enable"));
    }
}
