package com.moko.mokoplugpro.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.moko.mokoplugpro.BuildConfig;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.databinding.ActivityAboutProBinding;
import com.moko.mokoplugpro.utils.Utils;


public class AboutActivity extends BaseActivity<ActivityAboutProBinding> {

    @Override
    protected void onCreate() {
        if (!BuildConfig.IS_LIBRARY) {
            mBind.tvSoftVersion.setText(String.format(getString(R.string.version_info), Utils.getVersionInfo(this)));
        }
    }

    @Override
    protected ActivityAboutProBinding getViewBinding() {
        return ActivityAboutProBinding.inflate(getLayoutInflater());
    }

    public void openURL(View view) {
        if (isWindowLocked())
            return;
        Uri uri = Uri.parse("https://" + getString(R.string.company_website));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void back(View view) {
        finish();
    }
}
