package com.hypermax.home.helper;

import androidx.preference.Preference;

import com.hypermax.R;
import com.hypermax.dashboard.SettingsPreferenceFragment;
import com.hypermax.prefs.LayoutPreference;
import com.hypermax.utils.FrameworkStatusManager;
import com.hypermax.utils.ScopeManager;

public class FrameworkWarningHelpFragment extends SettingsPreferenceFragment {

    private Preference mHelpFrameworkWarning;
    private LayoutPreference mCompatibleCard;
    private LayoutPreference mServiceErrorCard;
    private LayoutPreference mPendingCard;
    private LayoutPreference mIncompatibleCard;
    private final ScopeManager.ServiceStateListener mServiceStateListener =
        service -> updateSummary();

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_help_framework_warning;
    }

    @Override
    public void initPrefs() {
        setTitle(com.hypermax.core.R.string.help);
        mCompatibleCard = findPreference("prefs_key_layout_framework_compatible");
        mServiceErrorCard = findPreference("prefs_key_layout_framework_service_error");
        mPendingCard = findPreference("prefs_key_layout_framework_pending");
        mIncompatibleCard = findPreference("prefs_key_layout_framework_incompatible");
        mHelpFrameworkWarning = findPreference("prefs_key_textview_help_framework_warning");
        updateSummary();
    }

    @Override
    public void onStart() {
        super.onStart();
        ScopeManager.addServiceStateListener(mServiceStateListener, true);
    }

    @Override
    public void onStop() {
        ScopeManager.removeServiceStateListener(mServiceStateListener);
        super.onStop();
    }

    private void updateSummary() {
        if (getContext() == null) {
            return;
        }

        FrameworkStatusManager.Status status = FrameworkStatusManager.getCurrentStatus();
        updateCards(status);

        if (mHelpFrameworkWarning != null) {
            mHelpFrameworkWarning.setSummary(FrameworkStatusManager.buildHelpSummary(requireContext()));
        }
    }

    private void updateCards(FrameworkStatusManager.Status status) {
        if (mCompatibleCard != null) {
            mCompatibleCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.COMPATIBLE);
        }
        if (mIncompatibleCard != null) {
            mIncompatibleCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.API_TOO_LOW);
        }
        if (mServiceErrorCard != null) {
            mServiceErrorCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.SERVICE_ERROR);
        }
        if (mPendingCard != null) {
            mPendingCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.PENDING);
        }
    }
}
