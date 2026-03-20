package com.github.ghmxr.ftpshare;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatDelegate;

import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.NetworkStatusMonitor;

public class MyApplication extends Application {

    public static final Handler handler = new Handler(Looper.getMainLooper());
    private static MyApplication myApplication;

    public static Context getGlobalBaseContext() {
        return myApplication.getApplicationContext();
    }

    @Override
    public void onCreate() {
        myApplication = this;
        super.onCreate();
        NetworkStatusMonitor.init(this);
        AppCompatDelegate.setDefaultNightMode(CommonUtils.getSettingSharedPreferences(this)
                .getInt(Constants.PreferenceConsts.NIGHT_MODE, Constants.PreferenceConsts.NIGHT_MODE_DEFAULT));
    }
}
