package com.algorithm4.library.algorithm4library;

import android.util.Log;

public class Algorithm4SensorLib {
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

    /**
     * 初始化算法句柄
     * @return
     */
    public native static boolean InitSingleInstance();

    /**
     * 设置目标采样率
     * @param SampleRate
     */
    public native static void SetTargetSampling(int SampleRate[]);

    /**
     * 获取参数计算结果
     * @param Data
     */
    public native static void getValue(double Data[]);

    /**
     * 获取波形数据
     * @param SampledData
     */
    public native static void getSampledData(double SampledData[]);

    /**
     * 添加数据接口
     * @param data
     * @param nDataLen
     * @return
     */
    public native static int addRecvData(byte data[], int nDataLen);

    /**
     * 构建命令
     * @param cmdType
     * @param cmdTransfer
     * @return
     */
    public native static int GeneralCmd4Dev(int cmdType, byte cmdTransfer[]);

    /**
     * 获取命令的返回结果
     * @param cmdType
     * @param cmdTransfer
     * @return
     */
    public native static boolean GetCmdResult(int cmdType, byte cmdTransfer[]);

    /**
     * 获取传感器信息
     * @param cmdTransfer   一个长度为256的byte数组
     * @return
     */
    public native static boolean GetSensorMsg(byte cmdTransfer[]);

    /**
     * 获取集中器的Mac地址
     * @param CentorMac
     * @return
     */
    public native static int GetCentorMac(byte CentorMac[]);
}
