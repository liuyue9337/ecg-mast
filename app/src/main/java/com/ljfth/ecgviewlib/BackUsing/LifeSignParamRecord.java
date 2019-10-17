package com.ljfth.ecgviewlib.BackUsing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class LifeSignParamRecord {

    public static AlarmManager getAlarmManager(Context ctx) {
        return (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * 开启一个定时器任务
     * @param ctx
     */
    public static void sendUpdateBroadcast(Context ctx) {
        int nWaitTime = 1000 * 60;
        Log.i("bodystm", "LifeSignParamRecord sendUpdateBroadcast ... ... " + nWaitTime + "ms.");
        AlarmManager am = getAlarmManager(ctx);
        Intent i = new Intent(ctx, LifeSingUpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, i, 0);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + nWaitTime, pendingIntent);
    }

    /**
     * 取消定时器任务
     * @param ctx
     */
    public static void cancelUpdateBroadcast(Context ctx) {
        Log.i("bodystm", "LifeSignParamRecord cancelUpdateBroadcast ... ... ");
        AlarmManager am = getAlarmManager(ctx);
        Intent i = new Intent(ctx, LifeSingUpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, i, 0);
        am.cancel(pendingIntent);
    }
}

