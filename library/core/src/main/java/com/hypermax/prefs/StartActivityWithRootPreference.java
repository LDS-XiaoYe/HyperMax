/*
  * This file is part of HyperMax.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.hypermax.prefs;

import static com.hypermax.common.utils.ShellUtils.checkRootPermission;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.hypermax.core.R;
import com.hypermax.common.log.AndroidLog;

import java.io.DataOutputStream;
import java.io.IOException;

public class StartActivityWithRootPreference extends Preference {

    private final String targetActivityClass;

    public StartActivityWithRootPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        targetActivityClass = attrs.getAttributeValue("http://schemas.android.com/apk/res-custom", "targetActivityClass");
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setOnClickListener(v -> launchActivityWithRoot());
    }

    private void launchActivityWithRoot() {
        if (checkRootPermission() == 0) {
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                os.writeBytes("am start -n " + targetActivityClass + "\n");
                os.flush();
                os.writeBytes("exit\n");
                os.flush();
                suProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                AndroidLog.e("StartActivityWithRootPreference", "com.hypermax", "Failed to start activity \"" + targetActivityClass + "\" with root", e);
            }
        } else {
            Toast.makeText(this.getContext(), R.string.start_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
