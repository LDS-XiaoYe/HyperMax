package com.hypermax.ui;

import android.content.Context;

import com.hypermax.utils.LanguageHelper;

import fan.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase));
    }
}
