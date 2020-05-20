package com.ljfth.ecgviewlib.BackUsing;

import android.os.Environment;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.algorithm4.library.algorithm4library.Algorithm4Library;
import com.algorithm4.library.algorithm4library.Algorithm4SensorLib;
import com.ljfth.ecgviewlib.Constant;
import com.ljfth.ecgviewlib.EcgWaveView;
import com.ljfth.ecgviewlib.base.BaseActivity;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static java.lang.Thread.sleep;


//该类作为一个后台数据处理线程，处理串口接收的数据，完成文件存储、绘图线程数据供给
public class CMyDeamonProcess implements Runnable {
    private void Print2Hex(byte[] data, int start, int len) {
        String print = "";
        if (data.length >= len) {
            for (int i = 0; i < len; i++) {
                String strTmp = Integer.toHexString(data[i + start] & 0xFF).toUpperCase();
                print += strTmp + " ";
            }
        }
        Log.e("DeamonProcess", "Protocol : " + print);
    }

    private void Print2Hex(double[][] data, int len) {
        String print = "";
        if (data.length >= len) {
            for (int i = 0; i < len; i++) {
                String strTmp = Integer.toHexString((int) data[9][i]).toUpperCase();
                print += strTmp + " ";
            }
        }
        Log.e("DeamonProcess", "Int Protocol : " + print);
//        HashMap<String, Object> params = new HashMap<>();
//        params.put("action", "aaaa");
//        params.put("data", data);
//        EventBus.getDefault().post(params);
    }

    public void saveData4Test(byte[] data) {
        //存储数据
        if (outputStream == null || bufferedOutputStream == null) {
            dataSavePath = getDateSavePath();
            Log.e("LogFilePath", dataSavePath);
            try {
                outputStream = new FileOutputStream(dataSavePath);
                bufferedOutputStream = new BufferedOutputStream(outputStream);
                //bufferedOutputStream.write(data);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("LogFilePath", dataSavePath + e.toString());
            } finally {
                /*
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                        bufferedOutputStream = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                        outputStream = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                */
            }
        }

        try {
            bufferedOutputStream.write(data);
            //Log.e("WriteTest", "data is " + data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    //主动调用一次获取原始数据操作使得内部数据缓冲区置为0
    public void clearAlgInternalValue() {
        //清除算法库内部波形缓冲数据
        final int SampledBufferLen = 256;
        double sampledData[][] = new double[CSingleInstance.getInstance().SampledDataTypeMax][SampledBufferLen];
        double sampleDataLen[] = new double[CSingleInstance.getInstance().SampledDataTypeMax];
        //Algorithm4Library.getSampledData(sampledData, sampleDataLen);
    }

    byte[] tempSensorMac = new byte[6];
    byte[] spo2SensorMac = new byte[6];
    byte[] ecgSensorMac = new byte[6];
    byte[] nibpSensorMac = new byte[6];
    byte[] hpmsSensorMac = new byte[6];
    final int SampledBufferLen = 1024;
    double sampledData[] = new double[SampledBufferLen*6];  //只获取（ECG-I, ECG-II, ECG-V, Resp, SpO2-IR)5种波形
    CSingleInstance gTransferData = CSingleInstance.getInstance();

    public byte[] getNibpSensorMac() {
        return nibpSensorMac;
    }

    public byte[] getHpmsSensorMac() {
        return hpmsSensorMac;
    }

    void analysisSensorMsg() {
        byte sensorMsg[] = new  byte[256];
        Algorithm4SensorLib.GetSensorMsg(sensorMsg);
        int nBaseLen = 7;
        int nValidDataStart = 1;
        for (int i = 0; i < sensorMsg[0] / nBaseLen; i++) {
            int nDevType = sensorMsg[i * nBaseLen + 6 + nValidDataStart];
            switch (nDevType) {
                case Constant.BLE_DEV_TEMP:
                {
                    System.arraycopy(sensorMsg, i * nBaseLen + nValidDataStart, tempSensorMac, 0, 6);
                }break;
                case Constant.BLE_DEV_SPO2:
                {
                    System.arraycopy(sensorMsg, i * nBaseLen + nValidDataStart, spo2SensorMac, 0, 6);
                }break;
                case Constant.BLE_DEV_ECG:
                {
                    System.arraycopy(sensorMsg, i * nBaseLen + nValidDataStart, ecgSensorMac, 0, 6);
                }break;
                case Constant.BLE_DEV_NIBP:
                {
                    System.arraycopy(sensorMsg, i * nBaseLen + nValidDataStart, nibpSensorMac, 0, 6);
                }break;
                case Constant.BLE_DEV_HPMS:
                {
                    System.arraycopy(sensorMsg, i * nBaseLen + nValidDataStart, hpmsSensorMac, 0, 6);
                }break;
                default:
                    break;
            }
        }
    }

    /**
     * 判断传感器Mac地址是否有效
     * @param sensorMac
     * @return
     */
    boolean checkSensorIsValid(byte[] sensorMac) {
        byte[] equal = new byte[6];
        if (Arrays.equals(sensorMac, equal)) {
            return false;
        }
        return true;
    }

    /**
     * 设置目标采样率
     */
    void  setTargetSamplingRate() {
        int[] nSampledRate = new int[32];

        int nIndex = 1;
        //GeneralData
        if (checkSensorIsValid(hpmsSensorMac)) {
            //多参设备优先
            //心电
            for (int i = 0; i < 6; i++) {
                nSampledRate[nIndex++] = hpmsSensorMac[i];
            }
            nSampledRate[nIndex++] = Constant.VitalSignECG;
            nSampledRate[nIndex++] = Constant.TagetSampled4Draw;
            //呼吸
            for (int i = 0; i < 6; i++) {
                nSampledRate[nIndex++] = hpmsSensorMac[i];
            }
            nSampledRate[nIndex++] = Constant.VitalSignResp;
            nSampledRate[nIndex++] = Constant.TagetSampled4Draw / 2;
            //血氧
            for (int i = 0; i < 6; i++) {
                nSampledRate[nIndex++] = hpmsSensorMac[i];
            }
            nSampledRate[nIndex++] = Constant.VitalSignSPO2;
            nSampledRate[nIndex++] = Constant.TagetSampled4Draw;
        } else {
            //单传感器设备
            //心电
            if (checkSensorIsValid(ecgSensorMac)) {
                //心电
                for (int i = 0; i < 6; i++) {
                    nSampledRate[nIndex++] = ecgSensorMac[i];
                }
                nSampledRate[nIndex++] = Constant.VitalSignECG;
                nSampledRate[nIndex++] = Constant.TagetSampled4Draw;
                //呼吸
                for (int i = 0; i < 6; i++) {
                    nSampledRate[nIndex++] = ecgSensorMac[i];
                }
                nSampledRate[nIndex++] = Constant.VitalSignResp;
                nSampledRate[nIndex++] = Constant.TagetSampled4Draw / 2;
            }
            //血氧
            if (checkSensorIsValid(spo2SensorMac)){
                for (int i = 0; i < 6; i++) {
                    nSampledRate[nIndex++] = spo2SensorMac[i];
                }
                nSampledRate[nIndex++] = Constant.VitalSignSPO2;
                nSampledRate[nIndex++] = Constant.TagetSampled4Draw;
            }
        }

        nSampledRate[0] = nIndex - 1;
        Algorithm4SensorLib.SetTargetSampling(nSampledRate);
    }

    /**
     * 获取波形数据
     */
    void getSampledData() {
        //GeneralData
        sampledData[0] = 0;
        int nIndex = 1;
        int mIndex = 0;
        if (checkSensorIsValid(hpmsSensorMac)) {
            //多参设备优先
            //心电
            for (int i = 0; i < 6; i++) {
                sampledData[nIndex * SampledBufferLen + mIndex++] = hpmsSensorMac[i] & 0xFF;
            }
            sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgI;
            nIndex++;
            mIndex = 0;

            for (int i = 0; i < 6; i++) {
                sampledData[nIndex * SampledBufferLen + mIndex++] = hpmsSensorMac[i] & 0xFF;
            }
            sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgII;
            nIndex++;
            mIndex = 0;

            for (int i = 0; i < 6; i++) {
                sampledData[nIndex * SampledBufferLen + mIndex++] = hpmsSensorMac[i] & 0xFF;
            }
            sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgV;
            nIndex++;
            mIndex = 0;

            //呼吸
            for (int i = 0; i < 6; i++) {
                sampledData[nIndex * SampledBufferLen + mIndex++] = hpmsSensorMac[i] & 0xFF;
            }
            sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeResp;
            nIndex++;
            mIndex = 0;

            //血氧
            for (int i = 0; i < 6; i++) {
                sampledData[nIndex * SampledBufferLen + mIndex++] = hpmsSensorMac[i] & 0xFF;
            }
            sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeIred;
            nIndex++;
            mIndex = 0;

            sampledData[0] = nIndex - 1;
        } else {
            if (checkSensorIsValid(ecgSensorMac)) {
                //心电
                for (int i = 0; i < 6; i++) {
                    sampledData[nIndex * SampledBufferLen + mIndex++] = ecgSensorMac[i] & 0xFF;
                }
                sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgI;
                nIndex++;
                mIndex = 0;

                for (int i = 0; i < 6; i++) {
                    sampledData[nIndex * SampledBufferLen + mIndex++] = ecgSensorMac[i] & 0xFF;
                }
                sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgII;
                nIndex++;
                mIndex = 0;

                for (int i = 0; i < 6; i++) {
                    sampledData[nIndex * SampledBufferLen + mIndex++] = ecgSensorMac[i] & 0xFF;
                }
                sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeEcgV;
                nIndex++;
                mIndex = 0;

                //呼吸
                for (int i = 0; i < 6; i++) {
                    sampledData[nIndex * SampledBufferLen + mIndex++] = ecgSensorMac[i] & 0xFF;
                }
                sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeResp;
                nIndex++;
                mIndex = 0;
            }

            //血氧
            if (checkSensorIsValid(spo2SensorMac)) {
                //呼吸
                for (int i = 0; i < 6; i++) {
                    sampledData[nIndex * SampledBufferLen + mIndex++] = spo2SensorMac[i] & 0xFF;
                }
                sampledData[nIndex * SampledBufferLen + mIndex++] = Constant.WaveTypeIred;
                nIndex++;
                mIndex = 0;
            }

            sampledData[0] = nIndex - 1;
        }
        Algorithm4SensorLib.getSampledData(sampledData);
        getValidEcgWave();
    }

    /**
     * 获取生命体征参数信息
     */
    void getValue() {
        double[] dVitualSignVal = new double[256];
        int nIndex = 1;

        if (checkSensorIsValid(hpmsSensorMac)) {
            //多参设备优先
            //Hpms Temp
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamTemp;
            dVitualSignVal[nIndex++] = -1;
            //Hpms HR
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamHR;
            dVitualSignVal[nIndex++] = -1;
            //Hpms Resp
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamRR;
            dVitualSignVal[nIndex++] = -1;
            //Hpms SpO2
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamSpO2;
            dVitualSignVal[nIndex++] = -1;
            //Hpms BPM
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamPR;
            dVitualSignVal[nIndex++] = -1;
            //Hpms PI
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamPI;
            dVitualSignVal[nIndex++] = -1;
            //Hpms SBP
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamSBP;
            dVitualSignVal[nIndex++] = -1;
            //Hpms DBP
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamDBP;
            dVitualSignVal[nIndex++] = -1;
            //Hpms ABP
            dVitualSignVal[nIndex++] = hpmsSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = hpmsSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamABP;
            dVitualSignVal[nIndex++] = -1;

            dVitualSignVal[0] = nIndex - 1;
        } else {
            //Temp
            dVitualSignVal[nIndex++] = tempSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = tempSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = tempSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = tempSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = tempSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = tempSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamTemp;
            dVitualSignVal[nIndex++] = -2;
            //HR
            dVitualSignVal[nIndex++] = ecgSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamHR;
            dVitualSignVal[nIndex++] = -2;
            //Resp
            dVitualSignVal[nIndex++] = ecgSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = ecgSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamRR;
            dVitualSignVal[nIndex++] = -2;
            //SpO2
            dVitualSignVal[nIndex++] = spo2SensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamSpO2;
            dVitualSignVal[nIndex++] = -2;
            //BPM
            dVitualSignVal[nIndex++] = spo2SensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamPR;
            dVitualSignVal[nIndex++] = -2;
            //PI
            dVitualSignVal[nIndex++] = spo2SensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = spo2SensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamPI;
            dVitualSignVal[nIndex++] = -2;
            //SBP
            dVitualSignVal[nIndex++] = nibpSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamSBP;
            dVitualSignVal[nIndex++] = -2;
            //DBP
            dVitualSignVal[nIndex++] = nibpSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamDBP;
            dVitualSignVal[nIndex++] = -2;
            //ABP
            dVitualSignVal[nIndex++] = nibpSensorMac[0] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[1] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[2] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[3] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[4] & 0xFF;
            dVitualSignVal[nIndex++] = nibpSensorMac[5] & 0xFF;
            dVitualSignVal[nIndex++] = Constant.VitalSignParamABP;
            dVitualSignVal[nIndex++] = -2;

            dVitualSignVal[0] = nIndex - 1;
        }
        Algorithm4SensorLib.getValue(dVitualSignVal);
        //Algorithm4Library.getValue(gTransferData.m_dVitalSign);
        nIndex = 1;
        for (int i = 0; i < (int)(dVitualSignVal[0]) / 8; i++) {
            gTransferData.m_dVitalSign[(int)dVitualSignVal[nIndex + 8 * i + 6]] = dVitualSignVal[nIndex + 8 * i + 7];
        }
    }

    /**
     * 确认有波形的心电数据通道
     * @return
     *  -1:处理，保持原来的状态
     */
    private int curValidEcgWaveChannel = -1;
    private int curValidEcgWaveLen = -1;
    void getValidEcgWave() {
        int nIndex = 1;
        int nLoop = (int)sampledData[0];
        int nEcgI = 0, nEcgII = 0, nEcgV = 0;

        for (int i = 0; i < nLoop; i++) {
            int nType = (int) sampledData[(nIndex + i) * SampledBufferLen + 6];
            int validLen = (int) (sampledData[(nIndex + i) * SampledBufferLen + 7]);
            switch (nType) {
                case Constant.WaveTypeEcgI:
                    nEcgI = validLen;
                    break;
                case Constant.WaveTypeEcgII:
                    nEcgII = validLen;
                    break;
                case Constant.WaveTypeEcgV:
                    nEcgV = validLen;
                    break;
                default:
                    break;
            }
        }

        long subTime = System.currentTimeMillis() - gTransferData.disconnectTime;
        if (nEcgII > 0) {
            curValidEcgWaveChannel = Constant.WaveTypeEcgII;
            curValidEcgWaveLen = nEcgII;
            if (gTransferData.needAutoSelectEcgChannel && subTime > 500) {
                gTransferData.needAutoSelectEcgChannel = false;
                gTransferData.ecgChannelItem = 0;
                gTransferData.ecgViewDataItem = gTransferData.SampledDataECGII;
                //向MainActive发送消息
                Message msg = new Message();
                msg.what = 10; //AutoSelectEcgChannel = 10;
                gTransferData.mainActiveHandler.sendMessage(msg);

            }
            //return Constant.WaveTypeEcgII;
        } else if (nEcgI > 0) {
            curValidEcgWaveChannel = Constant.WaveTypeEcgI;
            if (gTransferData.needAutoSelectEcgChannel && subTime > 500) {
                gTransferData.needAutoSelectEcgChannel = false;
                gTransferData.ecgChannelItem = 1;
                gTransferData.ecgViewDataItem = gTransferData.SampledDataECGI;
                //向MainActive发送消息
                Message msg = new Message();
                msg.what = 10; //AutoSelectEcgChannel = 10;
                gTransferData.mainActiveHandler.sendMessage(msg);
            }
            curValidEcgWaveLen = nEcgI;
            //return Constant.WaveTypeEcgI;
        } else if (nEcgV > 0) {
            curValidEcgWaveChannel = Constant.WaveTypeEcgV;
            curValidEcgWaveLen = nEcgV;
            if (gTransferData.needAutoSelectEcgChannel && subTime > 500) {
                gTransferData.needAutoSelectEcgChannel = false;
                gTransferData.ecgViewDataItem = gTransferData.SampledDataECGV;
                //向MainActive发送消息
                Message msg = new Message();
                msg.what = 10; //AutoSelectEcgChannel = 10;
                gTransferData.mainActiveHandler.sendMessage(msg);
            }
            //return Constant.WaveTypeEcgV;
        }
        //return -1;
    }

    /**
     * 处理心电波形数据
     */
    void dealEcgWaveData(int validLen, int nIndex, int i){

        //由于此处要处理三个通道的波形数据，必须定位到第一个通道位置

        switch (curValidEcgWaveChannel) {
            case Constant.WaveTypeEcgI: {
            } break;
            case Constant.WaveTypeEcgII: {
                i = i - 1;
            } break;
            case Constant.WaveTypeEcgV: {
                i = i - 2;
            } break;
        }

        //                                Log.i("bodystm", "ECG I len is " + String.valueOf(validLen));
//                                System.arraycopy(sampledData, (int)((nIndex + i) * SampledBufferLen + 7), gTransferData.sampledData[gTransferData.SampledDataECGI], 0, validLen + 1);
        int nWriteIndex = 0;
        int nLen = 0;
//                                    String print = "";
        for (int j = 0; j < validLen; j++) {
//                        Log.e("bodystm ECG R ", "DeamonProcess ecg Len is " + sampledData[CSingleInstance.SampledDataECGI][0]);

            //String strTmp = Double.toString(sampledData[gTransferData.SampledDataECGI][i]);
            //print += strTmp + ", ";

            nWriteIndex = gTransferData.m_atomECGWIndex.getAndAdd(0);
            if ((int)(sampledData[(nIndex + i) * SampledBufferLen + 7]) > 0) {
                gTransferData.m_dViewData[gTransferData.SampledDataECGI][nWriteIndex] = sampledData[(nIndex + i) * SampledBufferLen + 8 + j];
            } else {
                gTransferData.m_dViewData[gTransferData.SampledDataECGI][nWriteIndex] = 0.0f;
            }
            if ((int)(sampledData[(nIndex + i + 1) * SampledBufferLen + 7]) > 0) {
                gTransferData.m_dViewData[gTransferData.SampledDataECGII][nWriteIndex] = sampledData[(nIndex + i + 1) * SampledBufferLen + 8 + j];
            } else {
                gTransferData.m_dViewData[gTransferData.SampledDataECGII][nWriteIndex] = 0.0f;
            }
            if ((int)(sampledData[(nIndex + i + 2) * SampledBufferLen + 7]) > 0) {
                gTransferData.m_dViewData[gTransferData.SampledDataECGV][nWriteIndex] = sampledData[(nIndex + i + 2) * SampledBufferLen + 8 + j];
            } else {
                gTransferData.m_dViewData[gTransferData.SampledDataECGV][nWriteIndex] = 0.0f;
            }
            nWriteIndex++;
            nWriteIndex %= CSingleInstance.ViewArrayLen;
            gTransferData.m_atomECGWIndex.getAndSet(nWriteIndex);
        }

        //Show Data ECG I
        // Log.e("DeamonProcess", "double Protocol : " + print);
        //saveData4Test(print.getBytes());
        gTransferData.m_atomECGWIndex.getAndSet(nWriteIndex);
        nLen = gTransferData.m_atomECGLen.addAndGet(validLen);

        //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
        if (nLen > CSingleInstance.ViewArrayLen) {
            gTransferData.m_atomECGRIndex.getAndSet(0);
            gTransferData.m_atomECGLen.getAndSet(0);
            gTransferData.m_atomECGWIndex.getAndSet(0);
        }
    }

    /**
     * 处理呼吸波形数据
     */
    void dealRespWaveData(int validLen, int nIndex, int i) {
//                                Log.i("bodystm", "Resp len is " + String.valueOf(validLen));
        int nWriteIndex = 0;
        int nLen = 0;
        String print = "";
        for (int j = 0; j < validLen; j++) {
//                                        String strTmp = Double.toString(sampledData[gTransferData.SampledDataResp][i]);
//                                        print += strTmp + ", ";

            nWriteIndex = gTransferData.m_atomRespWIndex.getAndAdd(0);
            gTransferData.m_dViewData[gTransferData.SampledDataResp][nWriteIndex] = sampledData[(nIndex + i) * SampledBufferLen + 8 + j];
            nWriteIndex++;
            nWriteIndex %= CSingleInstance.ViewArrayLen;
            gTransferData.m_atomRespWIndex.getAndSet(nWriteIndex);
        }
        //saveData4Test(print.getBytes());
        gTransferData.m_atomRespWIndex.getAndSet(nWriteIndex);
        nLen = gTransferData.m_atomRespLen.addAndGet(validLen);

        //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
        if (nLen > CSingleInstance.ViewArrayLen) {
            gTransferData.m_atomRespRIndex.getAndSet(0);
            gTransferData.m_atomRespLen.getAndSet(0);
            gTransferData.m_atomRespWIndex.getAndSet(0);
        }
    }

    /**
     * 处理血氧红光波形数据
     * @param validLen
     * @param nIndex
     * @param i
     */
    void dealSpO2IredWaveData(int validLen, int nIndex, int i) {
//                                Log.i("bodystm", "Ired len is " + String.valueOf(validLen));
        //System.arraycopy(sampledData, (int)((nIndex + i) * SampledBufferLen + 7), sampledData[gTransferData.SampledDataECGI], 0, validLen + 1);
        //if ((int) (sampledData[CSingleInstance.SampledDataRed][0]) > 0) {
//                    Log.e("bodystm SpO2 R ", "DeamonProcess SpO2 Len is " + sampledData[CSingleInstance.SampledDataRed][0]);
        int nWriteIndex = 0;
        int nLen = 0;
        String print = "";
        for (int j = 0; j < validLen; j++) {
//                                        String strTmp = Double.toString(sampledData[gTransferData.SampledDataRed][i]);
//                                        print += strTmp + ", ";
            nWriteIndex = gTransferData.m_atomSpO2WIndex.getAndAdd(0);
            //gTransferData.m_dViewData[gTransferData.SampledDataRed][nWriteIndex] = sampledData[(int)((nIndex + i) * SampledBufferLen + 8 + j)];
            gTransferData.m_dViewData[gTransferData.SampledDataIred][nWriteIndex] = sampledData[(int)((nIndex + i) * SampledBufferLen + 8 + j)];
            gTransferData.m_nViewDataSta[gTransferData.SampledDataIred][nWriteIndex] = 0;
            nWriteIndex++;
            nWriteIndex %= CSingleInstance.ViewArrayLen;
            gTransferData.m_atomSpO2WIndex.getAndSet(nWriteIndex);
        }
//                                saveData4Test(print.getBytes());
        gTransferData.m_atomSpO2WIndex.getAndSet(nWriteIndex);
        nLen = gTransferData.m_atomSpO2Len.addAndGet(validLen);

        //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
        if (nLen > CSingleInstance.ViewArrayLen) {
            gTransferData.m_atomSpO2RIndex.getAndSet(0);
            gTransferData.m_atomSpO2Len.getAndSet(0);
            gTransferData.m_atomSpO2WIndex.getAndSet(0);
        }
        //   }
    }

    public void run() {
        int nCurBuffLen = 0, nCurReadIndex = 0, nCurTailIndex = 0, nCurWriteIndex = 0;
        int nCurFileLen = 0, nCurFileReadIndex = 0;

        final int SampledBufferLen = 1024;
        //double sampledData[][] = new double[gTransferData.SampledDataTypeMax][SampledBufferLen];
        //double sampleDataLen[] = new double[gTransferData.SampledDataTypeMax];
        byte[] tmpBuff = new byte[1024 * 512];
        Log.e("DeamonProcess", "run functoin is running ...  ");

        Date nowData = new Date();

        final int addDataMaxLen = 1024 * 2;

        byte cmdTransfer[] = new  byte[256];


        while (true) {

            //判断缓冲区长度
            nCurBuffLen = gTransferData.m_atomBuffDataLen.getAndAdd(0);
            if (nCurBuffLen > 0) {
                gTransferData.m_dNoDataContinueTime = 0;

                nCurReadIndex = gTransferData.m_atomReadIndex.getAndAdd(0);
                nCurTailIndex = gTransferData.m_atomTailIndex.getAndAdd(0);
                nCurWriteIndex = gTransferData.m_atomWriteIndex.getAndAdd(0);

                //Log.e("DeamonProcess", "buffer len is great than 0, is " + nCurBuffLen + " r " + nCurReadIndex + " W " + nCurWriteIndex + " T " + nCurTailIndex);

                if (nCurWriteIndex < nCurReadIndex) {
                    //只读取Tail部分，剩余的下次处理
                    int nTailLen = nCurTailIndex - nCurReadIndex + 1;
                    if (nTailLen > addDataMaxLen) {
                        nTailLen = addDataMaxLen;

                        gTransferData.m_atomReadIndex.getAndAdd(nTailLen);
                        int nCurLen = gTransferData.m_atomBuffDataLen.addAndGet(-nTailLen);
                        //Log.e("DeamonProcess", "addRecvData1 tmpBuffLen " + tmpBuff.length + " Real Len " + nTailLen + "nCurBuffLen" + nCurLen + " ReadIndex " + nCurReadIndex + " WriteIndexz " + nCurWriteIndex);
                    }
                    else {
                        gTransferData.m_atomReadIndex.getAndSet(0);
                        int nCurLen = gTransferData.m_atomBuffDataLen.addAndGet(-nTailLen);
                        //Log.e("DeamonProcess", "addRecvData1 tmpBuffLen " + tmpBuff.length + " Real Len " + nTailLen + "nCurBuffLen" + nCurLen + " ReadIndex " + nCurReadIndex + " WriteIndexz " + nCurWriteIndex);
                    }

                    try {
                        System.arraycopy(gTransferData.m_cRecvBuffer, nCurReadIndex, tmpBuff, 0, nTailLen);

                    } catch (Exception e) {
                        //Log.e("DeamonProcess", "addRecvData1 tmpBuffLen " + tmpBuff.length + " Real Len " + nTailLen + "  " + e.toString() + " ReadIndex " + nCurReadIndex + " WriteIndexz " + nCurWriteIndex);
                        System.arraycopy(gTransferData.m_cRecvBuffer, nCurReadIndex, tmpBuff, 0, nTailLen);
                    }
                    //add数据
//                    Algorithm4Library.addRecvData(tmpBuff, nTailLen);
                    Print2Hex(gTransferData.m_cRecvBuffer, nCurReadIndex, nTailLen);
                    Algorithm4SensorLib.addRecvData(tmpBuff, nTailLen);
                    //Print2Hex(tmpBuff, 0, nTailLen);
                    //获取Sensor信息
                    analysisSensorMsg();
                    //设置目标采样率
                    setTargetSamplingRate();
                    //获取波形数据
                    getSampledData();
                    //获取参数信息
                    getValue();

//                    byte[] trabsfer = new byte[256];
//                    boolean bool =  Algorithm4Library.GetCmdResult(Constant.TYPE_SEARCH, trabsfer);
//                    Print2Hex(trabsfer,0, (int)(trabsfer[0]));
                } else {
                    //直接从ReadIndex位置开始处理
                    try {
                        if (nCurBuffLen > addDataMaxLen) {
                            nCurBuffLen = addDataMaxLen;
                        }
                        System.arraycopy(gTransferData.m_cRecvBuffer, nCurReadIndex, tmpBuff, 0, nCurBuffLen);
                    } catch (Exception e) {
                        //Log.e("DeamonProcess", "addRecvData2 tmpBuffLen " + tmpBuff.length + " Real Len " + nCurBuffLen + "  " + e.toString() + " ReadIndex " + nCurReadIndex + " WriteIndexz " + nCurWriteIndex);
                        System.arraycopy(gTransferData.m_cRecvBuffer, nCurReadIndex, tmpBuff, 0, nCurBuffLen);
                    }
                    //Algorithm4Library.addRecvData(tmpBuff, nCurBuffLen);
                    Print2Hex(gTransferData.m_cRecvBuffer, nCurReadIndex, nCurBuffLen);
                    Algorithm4SensorLib.addRecvData(tmpBuff, nCurBuffLen);
                    //Print2Hex(tmpBuff, 0, nCurBuffLen);
                    //Algorithm4Library.getSampledData(sampledData, sampleDataLen);
                    //Algorithm4Library.getValue(gTransferData.m_dVitalSign);
                    //获取Sensor信息
                    analysisSensorMsg();
                    //设置目标采样率
                    setTargetSamplingRate();
                    //获取波形数据
                    getSampledData();
                    //获取参数信息
                    getValue();

//                    byte[] trabsfer = new byte[256];
//                    boolean bool =  Algorithm4Library.GetCmdResult(Constant.TYPE_SEARCH, trabsfer);
//                    Print2Hex(trabsfer,0, (int)(trabsfer[0]));

                    gTransferData.m_atomReadIndex.getAndAdd(nCurBuffLen);
                    int nCurLen = gTransferData.m_atomBuffDataLen.addAndGet(-nCurBuffLen);
                    //Log.e("DeamonProcess", "addRecvData2 tmpBuffLen " + tmpBuff.length + " Real Len " + nCurBuffLen + "nCurBuffLen" + nCurLen + " ReadIndex " + nCurReadIndex + " WriteIndexz " + nCurWriteIndex);
 /*
                    Log.e("DeamonProcess", "addRecvData2 Sampled  " + " 0:" + sampledData[0][0] + " 1:" + sampledData[1][0] + " 2:" + sampledData[2][0] + " 3:" + sampledData[3][0] + " 4:" + sampledData[4][0] + " 5:" + sampledData[5][0] + " 6:" + sampledData[6][0] + " 7:" + sampledData[7][0] + " 8:" + sampledData[8][0] + " 9:" + sampledData[9][0] +
                            " 9-1:" + sampledData[9][1] + " 9-2" +
                            ":" + sampledData[9][2] + " 9-3:" + sampledData[9][3] + " 9-4:" + sampledData[9][4] + " 9-5:" + sampledData[9][5] +
                            " 9-6:" + sampledData[9][6] + " 9-7:" + sampledData[9][7] + " 10:" + sampledData[10][0] + " 11:" + sampledData[11][0]);
                    String print = "";
                    for (int i = 0; i < 125; i++) {
                        String strTmp = Integer.toHexString((int)(sampledData[9][i]) & 0xFF).toUpperCase();
                        print += strTmp + " ";
                    }
                    Log.e("DeamonProcess", "Int Protocol : " + print);
                    */
                }

                //获得拿到的数据给绘图线程使用
                int nSpO2BufferLen = 0, nECGBufferLen = 0, nRespBufferLen = 0;
                int nIndex = 1;
                int nLoop = (int)sampledData[0];
                for (int i = 0; i < nLoop; i++) {
                    int nType = (int)sampledData[(nIndex + i) * SampledBufferLen + 6];
                    int validLen = (int)(sampledData[(nIndex + i) * SampledBufferLen + 7]);
                    switch (nType) {
                        case Constant.WaveTypeEcgI:
                            if (validLen > 0) {
                                if (curValidEcgWaveChannel == nType) {
                                    dealEcgWaveData(validLen, nIndex, i);
                                    nECGBufferLen = gTransferData.m_atomECGLen.addAndGet(0);
                                }
                            }
                            break;
                        case Constant.WaveTypeEcgII:
                            if (validLen > 0) {
                                if (curValidEcgWaveChannel == nType) {
                                    dealEcgWaveData(validLen, nIndex, i);
                                    nECGBufferLen = gTransferData.m_atomECGLen.addAndGet(0);
                                }
                            }
                            break;
                        case Constant.WaveTypeEcgV:
                            if (validLen > 0) {
                                if (curValidEcgWaveChannel == nType) {
                                    dealEcgWaveData(validLen, nIndex, i);
                                    nECGBufferLen = gTransferData.m_atomECGLen.addAndGet(0);
                                }
                            }
                            break;
                        case Constant.WaveTypeResp:
                            if (validLen > 0) {
                                dealRespWaveData(validLen, nIndex, i);
                                nRespBufferLen = gTransferData.m_atomRespLen.addAndGet(0);
                            }
                            break;
                        case Constant.WaveTypeIred:
                            if (validLen > 0) {
                                dealSpO2IredWaveData(validLen, nIndex, i);
                                nSpO2BufferLen = gTransferData.m_atomSpO2Len.addAndGet(0);
                            }
                            break;
                        default:
                            break;
                    }
                }

                /*
                if ((int) (sampledData[CSingleInstance.SampledDataRed][0]) > 0) {
//                    Log.e("bodystm SpO2 R ", "DeamonProcess SpO2 Len is " + sampledData[CSingleInstance.SampledDataRed][0]);
                    int nWriteIndex = 0;
                    int nLen = 0;
                    String print = "";
                    for (int i = 1; i < (int) (sampledData[CSingleInstance.SampledDataRed][0] + 1) && i < SampledBufferLen; i++) {
                        String strTmp = Double.toString(sampledData[gTransferData.SampledDataRed][i]);
                        print += strTmp + ", ";
                        nWriteIndex = gTransferData.m_atomSpO2WIndex.getAndAdd(0);
                        gTransferData.m_dViewData[gTransferData.SampledDataRed][nWriteIndex] = sampledData[gTransferData.SampledDataRed][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataIred][nWriteIndex] = sampledData[gTransferData.SampledDataIred][i];
                        gTransferData.m_nViewDataSta[gTransferData.SampledDataIred][nWriteIndex] = 0;
                        nWriteIndex++;
                        nWriteIndex %= CSingleInstance.ViewArrayLen;
                        gTransferData.m_atomSpO2WIndex.getAndSet(nWriteIndex);
                    }

//                    saveData4Test(print.getBytes());

                    gTransferData.m_atomSpO2WIndex.getAndSet(nWriteIndex);
                    nLen = gTransferData.m_atomSpO2Len.addAndGet((int) (sampledData[CSingleInstance.SampledDataRed][0]));

                    //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
                    if (nLen > CSingleInstance.ViewArrayLen) {
                        gTransferData.m_atomSpO2RIndex.getAndSet(0);
                        gTransferData.m_atomSpO2Len.getAndSet(0);
                        gTransferData.m_atomSpO2WIndex.getAndSet(0);
                    }
                }
                nSpO2BufferLen = gTransferData.m_atomSpO2Len.addAndGet(0);

                if ((int) (sampledData[CSingleInstance.SampledDataECGI][0]) > 0) {
                    int nWriteIndex = 0;
                    int nLen = 0;
                    String print = "";
                    for (int i = 1; i < (sampledData[CSingleInstance.SampledDataECGI][0] + 1)  && i < SampledBufferLen; i++) {
//                        Log.e("bodystm ECG R ", "DeamonProcess ecg Len is " + sampledData[CSingleInstance.SampledDataECGI][0]);

                        String strTmp = Double.toString(sampledData[gTransferData.SampledDataECGI][i]);
                        print += strTmp + ", ";

                        nWriteIndex = gTransferData.m_atomECGWIndex.getAndAdd(0);
                        gTransferData.m_dViewData[gTransferData.SampledDataECGI][nWriteIndex] = sampledData[gTransferData.SampledDataECGI][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGII][nWriteIndex] = sampledData[gTransferData.SampledDataECGII][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGIII][nWriteIndex] = sampledData[gTransferData.SampledDataECGIII][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGV][nWriteIndex] = sampledData[gTransferData.SampledDataECGV][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGavF][nWriteIndex] = sampledData[gTransferData.SampledDataECGavF][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGavL][nWriteIndex] = sampledData[gTransferData.SampledDataECGavL][i];
                        gTransferData.m_dViewData[gTransferData.SampledDataECGavR][nWriteIndex] = sampledData[gTransferData.SampledDataECGavR][i];
                        nWriteIndex++;
                        nWriteIndex %= CSingleInstance.ViewArrayLen;
                        gTransferData.m_atomECGWIndex.getAndSet(nWriteIndex);
                    }

                    //Show Data ECG I
                    // Log.e("DeamonProcess", "double Protocol : " + print);
                    //saveData4Test(print.getBytes());
                    gTransferData.m_atomECGWIndex.getAndSet(nWriteIndex);
                    nLen = gTransferData.m_atomECGLen.addAndGet((int) (sampledData[CSingleInstance.SampledDataECGI][0]));

                    //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
                    if (nLen > CSingleInstance.ViewArrayLen) {
                        gTransferData.m_atomECGRIndex.getAndSet(0);
                        gTransferData.m_atomECGLen.getAndSet(0);
                        gTransferData.m_atomECGWIndex.getAndSet(0);
                    }
                }
                nECGBufferLen = gTransferData.m_atomECGLen.addAndGet(0);

                if ((int) (sampledData[CSingleInstance.SampledDataResp][0]) > 0) {
//                    Log.e("bodystm Resp R ", "DeamonProcess Resp Len is " + sampledData[CSingleInstance.SampledDataResp][0]);
                    int nWriteIndex = 0;
                    int nLen = 0;
                    String print = "";
                    for (int i = 1; (i < sampledData[CSingleInstance.SampledDataResp][0] + 1)  && i < SampledBufferLen; i++) {
                        String strTmp = Double.toString(sampledData[gTransferData.SampledDataResp][i]);
                        print += strTmp + ", ";

                        nWriteIndex = gTransferData.m_atomRespWIndex.getAndAdd(0);
                        gTransferData.m_dViewData[gTransferData.SampledDataResp][nWriteIndex] = sampledData[gTransferData.SampledDataResp][i];
                        nWriteIndex++;
                        nWriteIndex %= CSingleInstance.ViewArrayLen;
                        gTransferData.m_atomRespWIndex.getAndSet(nWriteIndex);
                    }
                    //saveData4Test(print.getBytes());
                    gTransferData.m_atomRespWIndex.getAndSet(nWriteIndex);
                    nLen = gTransferData.m_atomRespLen.addAndGet((int) (sampledData[CSingleInstance.SampledDataResp][0]));

                    //判断缓冲去长度大于绘图缓冲去长度，则认为绘图线程停止工作，将程度初始化为0，读写指针也初始化为0
                    if (nLen > CSingleInstance.ViewArrayLen) {
                        gTransferData.m_atomRespRIndex.getAndSet(0);
                        gTransferData.m_atomRespLen.getAndSet(0);
                        gTransferData.m_atomRespWIndex.getAndSet(0);
                    }
                }
                nRespBufferLen = gTransferData.m_atomRespLen.addAndGet(0);
                 */

                Date tmpData = new Date();
                if (nowData.getTime() + 500 < tmpData.getTime()) {
                    nowData = tmpData;
                    HashMap<String, String> params = new HashMap<>();
                    //params.put("DrawViewBuff", "SpO2 " + Integer.toString(nSpO2BufferLen) + ", ECG " + Integer.toString(nECGBufferLen) + ", Resp " + Integer.toString(nRespBufferLen));
                    String writeData = String.format("S %03d, E %03d, R %03d, R : %d, W : %d, L : %d,T : %d", nSpO2BufferLen, nECGBufferLen, nRespBufferLen,
                            gTransferData.m_atomReadIndex.getAndAdd(0), gTransferData.m_atomWriteIndex.getAndAdd(0), gTransferData.m_atomBuffDataLen.getAndAdd(0), gTransferData.m_atomTailIndex.getAndAdd(0));
                    params.put("DrawViewBuff", writeData);
                    EventBus.getDefault().post(params);
//                    if (gTransferData.m_atomBuffDataLen.getAndAdd(0) < 0 || true) {
//                        saveData4Test(writeData.getBytes());
//                    }
                }

                //拷贝获得的生命体征参数给全局存储用
                gTransferData.m_dVitalSign4Record = gTransferData.m_dVitalSign;

                //判断文件是否需要存储
                /*
                nCurFileLen = gTransferData.m_atomFileDataLen.getAndAdd(0);
                nCurFileReadIndex = gTransferData.m_atomFileReadIndex.getAndAdd(0);
                if (nCurFileLen > gTransferData.m_nFileWriteBase) {
                    int nTailFileLen = nCurTailIndex - nCurFileReadIndex + 1;
                    if (nCurFileLen > nTailFileLen) {
                        if (nTailFileLen <= 0) {
                            //将数据全部写入，因读指针已经到结尾
                            nCurFileReadIndex = 0;
                            saveData(gTransferData.m_cRecvBuffer, nCurFileReadIndex, nCurFileLen);
                            gTransferData.m_atomFileReadIndex.getAndSet(nCurFileLen);
                        } else {
                            //写入Tail部分
                            saveData(gTransferData.m_cRecvBuffer, nCurFileReadIndex, nTailFileLen);
                            gTransferData.m_atomFileDataLen.getAndAdd(-nTailFileLen);
                            gTransferData.m_atomFileReadIndex.getAndSet(0);
                        }
                    } else {
                        //将数据全部写入
                        saveData(gTransferData.m_cRecvBuffer, nCurFileReadIndex, nCurFileLen);
                        gTransferData.m_atomFileReadIndex.getAndAdd(nCurFileLen);
                    }
                }
                */
            } else {
                try {
                    sleep(10);
                    gTransferData.m_dNoDataContinueTime++;
                    //超过5秒没有数据就清空记录数组，（心跳是1秒一次、温度是4秒一次）
                    if (gTransferData.m_dNoDataContinueTime * 10 > 5*1000) {
                        //防止溢出
                        gTransferData.m_dNoDataContinueTime = 5*1000/10 + 1;

                        for (int i = 0; i < gTransferData.m_dVitalSign4Record.length; i++) {
                            gTransferData.m_dVitalSign4Record[i] = -1;
                        }
                    }
//                    Date tmpData = new Date();
//                    if (nowData.getTime() + 1000 < tmpData.getTime()) {
//                        nowData = tmpData;
//                        HashMap<String, String> params = new HashMap<>();
//                        //params.put("DrawViewBuff", "SpO2 " + Integer.toString(nSpO2BufferLen) + ", ECG " + Integer.toString(nECGBufferLen) + ", Resp " + Integer.toString(nRespBufferLen));
//                        params.put("DrawViewBuff", String.format("SpO2_%03d, ECG_%03d, Resp_%03d", gTransferData.m_atomSpO2Len.addAndGet(0),
//                                gTransferData.m_atomECGLen.addAndGet(0),
//                                gTransferData.m_atomRespLen.addAndGet(0)));
//                        EventBus.getDefault().post(params);
//                    }
                    //Log.e("DeamonProcess", "Back Thread is sleep 500 ms");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getDateSavePath() {
        //return getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis();
        //return Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + "liuyue2.txt";
        //return "/storage/emulated/0/Android/data/" + System.currentTimeMillis() + "liuyue2.txt";
        return "/data/data/com.ljfth.ecgviewlib/cache/" + System.currentTimeMillis() + "liuyue2.txt";
    }

    //获得文件大小是否大于1024*1024*10， 10MB
    private boolean CheckFileSize(File file) {
        if (file.length() >= 1024 * 1024 * 10) {
            return true;
        }
        return false;
    }

    // 遍历文件 获得 时间、文件映射表
    private void getFiles(HashMap<Date, File> fileMap, String filePath) {
        File[] allFiles = new File(filePath).listFiles();
        if (allFiles != null) { // 若文件不为空，则遍历文件长度
            for (int i = 0; i < allFiles.length; i++) {
                File file = allFiles[i];
                if (file.isFile() && file.getName().endsWith(".body")) {
                    //获得文件创建时间
                    Date date = getFileCreateDataFromFileName(file.getName());
                    if (date != null) {
                        fileMap.put(date, file);
                    }
                }
            }
        }
    }

    //从文件名获得创建时间
    Date getFileCreateDataFromFileName(String fileName) {
        //日期格式为：yyyy_MM_dd__HH_mm_ss
        String dateStr = "yyyy_MM_dd__HH_mm_ss.body";
        Date date;
        if (fileName.endsWith(".body")) {
            if (fileName.length() >= dateStr.length()) {
                dateStr = fileName.substring(fileName.length() - dateStr.length(), fileName.length() - ".body".length());
            } else {
                return null;
            }
        }
        try {
            Log.i("MyDeamonProcess", fileName + " " + dateStr);
            date = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return date;
    }

    private FileOutputStream outputStream;
    private BufferedOutputStream bufferedOutputStream;
    private String dataSavePath = "";
    private String dataSaveFile = "";

    //存储数据（10MB一个文件存储）
    private void saveData(byte[] data, int nOffset, int nLen) {
        //检测存储文件路径是否存在，是否满足存储条件
        if (dataSavePath.isEmpty()) {
            dataSavePath = getDateSavePath();
        }

        if (dataSaveFile.isEmpty()) {
            //检测文件存储路径找到最新的文件，查看文件大小
            HashMap<Date, File> fileMap = new HashMap<Date, File>();
            getFiles(fileMap, dataSavePath);
            if (fileMap.isEmpty()) {
                //Create File
                dataSaveFile = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()) + ".body";
                try {
                    outputStream = new FileOutputStream(dataSavePath + "/" + dataSaveFile);
                    bufferedOutputStream = new BufferedOutputStream(outputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                //找到最新的文件查看是否满足存储需求
                Set<Date> set = fileMap.keySet();
                Date lasted = null;
                for (Date date : set) {
                    if (lasted == null) {
                        lasted = date;
                    } else {
                        if (lasted.getTime() > date.getTime()) {
                            lasted = date;
                        }
                    }
                }
                dataSaveFile = fileMap.get(lasted).getName();
                if (CheckFileSize(fileMap.get(lasted))) {
                    dataSaveFile = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()) + ".body";
                    try {
                        outputStream = new FileOutputStream(dataSavePath + "/" + dataSaveFile);
                        bufferedOutputStream = new BufferedOutputStream(outputStream);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            //Check File
            if (CheckFileSize(new File(dataSavePath + "/" + dataSaveFile))) {
                dataSaveFile = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()) + ".body";
            }

            try {
                outputStream = new FileOutputStream(dataSavePath + "/" + dataSaveFile);
                bufferedOutputStream = new BufferedOutputStream(outputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            bufferedOutputStream.write(data, nOffset, nLen);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                    bufferedOutputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                    outputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
