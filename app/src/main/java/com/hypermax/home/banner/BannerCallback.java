package com.hypermax.home.banner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.hypermax.R;
import com.hypermax.common.log.AndroidLog;
import com.hypermax.dashboard.SubSettings;
import com.hypermax.home.helper.FrameworkWarningHelpFragment;
import com.hypermax.home.safemode.SafeModeFragment;
import com.hypermax.utils.SettingLauncherHelper;

public class BannerCallback implements View.OnClickListener {

    private static final String TAG = "BannerCallback";
    public static final String ACTION_OPEN_SAFE_MODE_SETTINGS =
        "com.hypermax.action.OPEN_SAFE_MODE_SETTINGS";
    public static final String ACTION_OPEN_FRAMEWORK_WARNING_HELP =
        "com.hypermax.action.OPEN_FRAMEWORK_WARNING_HELP";

    public BannerCallback() {}

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        if (!(tag instanceof BannerBean bean)) return;

        Context context = view.getContext();
        if (handleUrl(context, bean)) {
            return;
        }
        handleAction(context, bean);
    }

    private boolean handleUrl(Context context, BannerBean bean) {
        String url = bean.getUrl();
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to open URL: " + url, e);
            return false;
        }
    }

    private void handleAction(Context context, BannerBean bean) {
        String action = bean.getAction();
        if (TextUtils.isEmpty(action) || handleCustomAction(context, action)) {
            return;
        }

        try {
            context.startActivity(buildActionIntent(bean, action));
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to handle action: " + action, e);
        }
    }

    private Intent buildActionIntent(BannerBean bean, String action) {
        Intent intent = new Intent(action);
        if (!TextUtils.isEmpty(bean.getPkg())) {
            intent.setPackage(bean.getPkg());
        }
        if (!TextUtils.isEmpty(bean.getExtras())) {
            intent.putExtra("banner_extras", bean.getExtras());
        }
        return intent;
    }

    private boolean handleCustomAction(Context context, String action) {
        if (ACTION_OPEN_SAFE_MODE_SETTINGS.equals(action)) {
            SettingLauncherHelper.onStartSettingsForArguments(
                context,
                SubSettings.class,
                SafeModeFragment.class.getName(),
                com.hypermax.core.R.string.settings_safe_mode
            );
            return true;
        }
        if (ACTION_OPEN_FRAMEWORK_WARNING_HELP.equals(action)) {
            SettingLauncherHelper.onStartSettingsForArguments(
                context,
                SubSettings.class,
                FrameworkWarningHelpFragment.class.getName(),
                R.string.help_framework_warning_title
            );
            return true;
        }
        return false;
    }

}
