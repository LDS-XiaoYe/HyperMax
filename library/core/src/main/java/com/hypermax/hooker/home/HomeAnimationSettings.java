/*
 * This file is part of HyperMax.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.hypermax.hooker.home;

import com.hypermax.core.R;
import com.hypermax.dashboard.DashboardFragment;

/** HyperOS 4 launcher animation controls, applied through the launcher's native ratio hook. */
public class HomeAnimationSettings extends DashboardFragment {
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_animation;
    }
}
