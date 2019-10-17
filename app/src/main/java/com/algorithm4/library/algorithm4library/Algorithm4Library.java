package com.algorithm4.library.algorithm4library;

import android.util.Log;

public class Algorithm4Library {
    static {
        try {
            System.loadLibrary("algorithm4library");
            Log.i("Algorithm", "动态库加载成功！！！");
            System.out.print("动态库加载成功！！！");
        } catch (Throwable e) {
            Log.d("Algorithm","algorithm4library库异常 ："+e.toString());
            System.out.print("algorithm4library库异常:" + e.toString());
        }
    }

    public native static boolean InitSingleInstance();

    //public native static int addData(byte data[][], int nProtocolLen, int nArrayLen);

    public native static void SetTargetSampling(int SampleRate[]);

    public native static void getValue(double Data[]);

    //public native static void getSampledData(double SampledData[][]);

    public native static void getSampledData(double SampledData[][], double SampleDataLen[]);
    //public native static void getSampledDataEX(double SampleDataLen[]);

    public native static int addRecvData(byte data[], int nDataLen);

    public native static int GeneralCmd4Dev(int cmdType, byte cmdTransfer[]);

    public native static boolean GetCmdResult(int cmdType, byte cmdTransfer[]);

    public native static int GetCentorMac(byte CentorMac[]);
}  