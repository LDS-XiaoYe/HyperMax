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
package com.hypermax.libhook.app;

import com.hchen.database.HookBase;
import com.hypermax.libhook.base.BaseLoad;
import com.hypermax.libhook.rules.personalassistant.ClearGadgetBridge;

/**
 * HyperMax personal-assistant (negative screen) entry.
 *
 * <p>Only the gadget bridge is kept: the OS4 launcher writes a private marker through its native
 * serializer, and this receiver clears the stale marker so widgets can be re-serialized after a
 * launcher restart. Everything else that used to be installed here is out of scope for HyperMax.</p>
 */
@HookBase(targetPackage = "com.miui.personalassistant")
public class PersonalAssistant extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        // Home's guarded native serializer emits the private marker only while its switch is on.
        // Do not gate this receiver on a stale startup preference snapshot.
        initHook(new ClearGadgetBridge(),
            com.hypermax.libhook.utils.api.DeviceHelper.System.isHyperOSVersion(4f));
    }
}
