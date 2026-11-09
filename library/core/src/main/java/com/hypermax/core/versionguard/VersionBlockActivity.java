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
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hypermax.core.R;

import fan.appcompat.app.AppCompatActivity;

/**
 * Terminal screen shown when the version check refuses to let the app continue.
 *
 * <p>It only offers "update" and "exit"; there is no way to ignore the check, and the back gesture
 * simply closes the app because the blocked activities have already finished.</p>
 */
public final class VersionBlockActivity extends AppCompatActivity {

    private static final String EXTRA_REASON = "com.hypermax.core.versionguard.REASON";
    private static final String EXTRA_CURRENT_VERSION_CODE =
        "com.hypermax.core.versionguard.CURRENT_VERSION_CODE";
    private static final String EXTRA_REQUIRED_VERSION_CODE =
        "com.hypermax.core.versionguard.REQUIRED_VERSION_CODE";

    /**
     * HyperMax has no release channel yet, so the update action opens the project repository
     * instead of a download URL that does not exist.
     */
    private static final String PROJECT_URL = "https://github.com/LDS-XiaoYe/HyperMax";

    public static void start(Activity activity, VersionGuardState.Blocked state) {
        Intent intent = new Intent(activity, VersionBlockActivity.class);
        intent.putExtra(EXTRA_REASON, state.reason.name());
        intent.putExtra(EXTRA_CURRENT_VERSION_CODE, state.currentVersionCode);
        if (state.requiredVersionCode != null) {
            intent.putExtra(EXTRA_REQUIRED_VERSION_CODE, state.requiredVersionCode.longValue());
        }
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.version_block_activity);

        Intent intent = getIntent();
        BlockReason reason = readReason(intent);
        boolean downgrade = reason == BlockReason.DOWNGRADE;
        long currentVersionCode = intent.getLongExtra(EXTRA_CURRENT_VERSION_CODE, 0L);
        long requiredVersionCode = intent.getLongExtra(EXTRA_REQUIRED_VERSION_CODE, -1L);

        ((TextView) findViewById(R.id.version_block_title)).setText(downgrade
            ? R.string.version_block_downgrade_title
            : R.string.version_block_corrupt_title);
        ((TextView) findViewById(R.id.version_block_message)).setText(downgrade
            ? R.string.version_block_downgrade_message
            : R.string.version_block_corrupt_message);
        ((TextView) findViewById(R.id.version_block_details)).setText(
            buildDetails(downgrade, currentVersionCode, requiredVersionCode));

        findViewById(R.id.version_block_update).setOnClickListener(view -> openProjectPage());
        findViewById(R.id.version_block_exit).setOnClickListener(view -> finishAffinity());
    }

    private static BlockReason readReason(Intent intent) {
        String name = intent == null ? null : intent.getStringExtra(EXTRA_REASON);
        if (name != null) {
            for (BlockReason reason : BlockReason.values()) {
                if (reason.name().equals(name)) {
                    return reason;
                }
            }
        }
        return BlockReason.WATERMARK_CORRUPT;
    }

    private String buildDetails(boolean downgrade, long currentVersionCode, long requiredVersionCode) {
        StringBuilder details = new StringBuilder();
        if (currentVersionCode > 0) {
            details.append(getString(R.string.version_block_current_version, currentVersionCode));
        }
        if (downgrade && requiredVersionCode > 0) {
            if (details.length() > 0) {
                details.append('\n');
            }
            details.append(getString(R.string.version_block_required_version, requiredVersionCode));
        }
        return details.toString();
    }

    private void openProjectPage() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.version_block_no_browser, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }
}
