package com.ljfth.ecgviewlib;

import android.app.Application;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;

public class EcgApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Beta.autoCheckUpgrade = true;
//         延迟Bugly初始化防止影响应用启动速度
        Beta.initDelay = 10*1000;
        // 升级检查周期设置,设置升级检查周期为60s(默认检查周期为0s)，60s内SDK不重复向后台请求策略);
        Beta.upgradeCheckPeriod = 10 * 1000;

        // ecg-mast 77518f0114
        Bugly.init(getApplicationContext(), "77518f0114", true);

    }
}
