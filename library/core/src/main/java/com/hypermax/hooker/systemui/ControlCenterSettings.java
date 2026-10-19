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
 * HyperMax control-center settings.
 *
 * <p>Only the weather chip is kept, because the launcher layout (search bar / capsule) reads its
 * height. Notification, tile and media-card customization from upstream is not part of HyperMax.</p>
 */
public class ControlCenterSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center;
    }

    @Override
    public void initPrefs() {
        // The weather switch is a plain preference; no picker or seekbar binding is needed.
    }
}
