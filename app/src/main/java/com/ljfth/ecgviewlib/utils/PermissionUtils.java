package com.ljfth.ecgviewlib.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * 关于申请授权
 * 只需要在主界面申请一次即可
 * 在其他子activity，自动授权
 * */
public class PermissionUtils {
    //这是要申请的权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * 解决安卓6.0以上版本不能读取外部存储权限的问题
     *
     * @param activity
     * @param requestCode
     * @return
     */
    public static boolean isGrantExternalRW(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int storagePermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //检测是否有权限，如果没有权限，就需要申请
            if (storagePermission != PackageManager.PERMISSION_GRANTED ) {
                //申请权限
                try {
                    activity.requestPermissions(PERMISSIONS_STORAGE, requestCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //返回false。说明没有授权
                return false;
            }
        }
        //说明已经授权
        return true;
    }
}