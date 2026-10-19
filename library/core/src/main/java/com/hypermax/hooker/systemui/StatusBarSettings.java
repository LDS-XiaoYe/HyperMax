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
package com.hypermax.hooker.systemui;

import com.hypermax.core.R;
import com.hypermax.dashboard.DashboardFragment;

/**
 * HyperMax status-bar settings.
 *
 * <p>Only the clock surface is kept: the module's status-bar footprint is the seconds hand and the
 * indicator styling that the launcher capsule shares. Icon/battery/network pages from upstream are
 * intentionally not exposed.</p>
 */
public class StatusBarSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar;
    }

    @Override
    public void initPrefs() {
        // The clock page is a plain fragment jump declared in XML; nothing to bind here.
    }
}
