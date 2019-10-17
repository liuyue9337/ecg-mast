package com.ljfth.ecgviewlib.BackUsing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStorageDirectory;

public class LifeSingUpdateReceiver extends BroadcastReceiver {

    CSingleInstance gTransferData = CSingleInstance.getInstance();

    private String getDateStr(Date date) {
        SimpleDateFormat sdf_yMdHms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf_yMdHms.format(date);
    }

    private String getDateSavePath(Context context) {


        Date date = new Date();
        SimpleDateFormat sdf_yMd = new SimpleDateFormat("yyyy-MM-dd");
        String strDate_yMd = sdf_yMd.format(date);
//        return context.getExternalFilesDir(null).getAbsolutePath() + "/" + strDate_yMd + "_bodystm.txt";

        //判断文件夹是否存在,如果不存在则创建文件夹
        String strDir = Environment.getExternalStoragePublicDirectory("bodystm").getAbsolutePath();
        File file = new File(strDir);
        if (!file.exists()) {
            file.mkdir();
        }
        return strDir + "/" + strDate_yMd + "_bodystm.txt";
    }

    private byte[] getWriteMsg(String msg) {
        String strMsg = getDateStr(new Date());
        strMsg += "," + gTransferData.m_dVitalSign4Record[CSingleInstance.VitalSignTemp] + "\n";
        if (strMsg.getBytes().length <= 0) {
            return null;
        } else {
            return strMsg.getBytes();
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "BroadcastReceiver ...", Toast.LENGTH_LONG).show();
        //打开文件
        String fileName = getDateSavePath(context);
        if (fileName.isEmpty() || fileName == null) {
            Log.i("bodsytm", "File name get error !!!");
            Log.i("bodystm", "1--BroadcastReceiver onReceive is Running ..., cur time is " + new Date().toString());
            LifeSignParamRecord.sendUpdateBroadcast(context);
            return;
        } else {
            Log.i("bodystm", "File path is " + fileName);
        }
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            outputStream = new FileOutputStream(fileName, true);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            Log.i("bodystm", "2--BroadcastReceiver onReceive is Running ..., cur time is " + new Date().toString());
            LifeSignParamRecord.sendUpdateBroadcast(context);
        }

        //写入文件
        byte[] writeBytes = getWriteMsg("");
        try {
            if (writeBytes != null)
                bufferedOutputStream.write(writeBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭文件
            try {
                Uri uri = Uri.fromFile(new File(fileName));
                Intent intent1 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
                context.sendBroadcast(intent1);

                bufferedOutputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.i("bodystm", "3--BroadcastReceiver onReceive is Running ..., cur time is " + new Date().toString());
                LifeSignParamRecord.sendUpdateBroadcast(context);
            }
        }
    }
}