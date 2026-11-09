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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.content.pm.PackageInfoCompat;

/**
 * Reads the versionCode HyperMax is actually installed with.
 *
 * <p>{@link PackageInfoCompat#getLongVersionCode} keeps the long form of the version code and hides
 * the difference between the deprecated {@code versionCode} field and {@code longVersionCode}.</p>
 */
public final class AndroidPackageVersionSource implements VersionGuard.PackageVersionSource {

    private final Context context;
    private final String packageName;

    public AndroidPackageVersionSource(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    @Override
    public VersionGuard.PackageVersion readInstalledVersion() {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(packageName, 0);
            return new VersionGuard.PackageVersion(
                PackageInfoCompat.getLongVersionCode(info),
                info.versionName == null ? "" : info.versionName
            );
        } catch (PackageManager.NameNotFoundException error) {
            throw new IllegalStateException("Unable to read the installed version of " + packageName, error);
        }
    }
}
