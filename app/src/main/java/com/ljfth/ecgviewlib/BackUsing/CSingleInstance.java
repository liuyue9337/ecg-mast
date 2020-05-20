package com.ljfth.ecgviewlib.BackUsing;

import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.algorithm4.library.algorithm4library.Algorithm4Library;
import com.algorithm4.library.algorithm4library.Algorithm4SensorLib;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.util.concurrent.atomic.AtomicInteger;




public class CSingleInstance {
    //将同步内容下方到if内部，提高了执行的效率，不必每次获取对象时都进行同步，只有第一次才同步，创建了以后就没必要了。
    private static volatile CSingleInstance instance = null;

    private CSingleInstance() {
        //Algorithm4Library.InitSingleInstance();
        Algorithm4SensorLib.InitSingleInstance();
        //初始化生命体征参数变量为-1
        for (int i = 0; i < m_dVitalSign4Record.length; i++) {
            m_dVitalSign4Record[i] = -1;
            m_dVitalSign[i] = -1;
        }
    }

    public static CSingleInstance getInstance() {
        if (instance == null) {
            synchronized (CSingleInstance.class) {
                if (instance == null) {
                    instance = new CSingleInstance();
                }
            }
        }
        return instance;
    }

    //For Deamon Thread
    public AtomicInteger m_atomDeamonProcess = new AtomicInteger(0);

    //For Usb Manage
    public AtomicInteger m_atomUsbManage = new AtomicInteger(0);

    //For Serial Port
    public UsbManager mUsbManager = null;
    public UsbSerialPort mPort = null;
    public SerialInputOutputManager mSerialIoManager = null;
    public boolean m_bUSB_Sta = false;
    public boolean m_bUSB_Is_Ready = false;

    //For NIBP Ctl Sta Record
    public int m_nNibpCtlPosition = 1;  //默认为停止状态

    public void updateReceivedData(byte[] data) {
        if (data.length > 0) {
            int nWriteIndex = m_atomWriteIndex.getAndAdd(data.length);
            int nCurBuffLen = m_atomBuffDataLen.addAndGet(0);
            if (m_nRecvBuffLen - nWriteIndex < data.length) {  //剩余空间不足以存储接收到的数据
                if (nCurBuffLen >= 2) {
                    //判断缓冲当前结尾是否为一包的结尾标识
                    if (m_cRecvBuffer[nWriteIndex - 1] == (byte) 0x55 &&
                            m_cRecvBuffer[nWriteIndex - 2] == (byte) 0x55) {
                        m_atomTailIndex.set(nWriteIndex - 1);
                        m_atomWriteIndex.set(0);
                        nWriteIndex = 0;
                    }
                    else {
                        int nLoop = nCurBuffLen - 1;
                        int nCurReadIndex = nWriteIndex - 2;
                        while (nLoop > 0) {
                            if (m_cRecvBuffer[nCurReadIndex] == (byte) 0xAA &&
                                    m_cRecvBuffer[nCurReadIndex + 1] == (byte) 0xAA) {
                                //找到包头，将不完整的数据拷贝至缓冲区头部
                                nWriteIndex = nWriteIndex - nCurReadIndex;
                                System.arraycopy(m_cRecvBuffer, nCurReadIndex, m_cRecvBuffer, 0, nWriteIndex);
                                m_atomWriteIndex.getAndSet(nWriteIndex);
                                m_atomTailIndex.getAndSet(nCurReadIndex - 1);
                                break;
                            } else if (m_cRecvBuffer[nCurReadIndex - 1] == (byte) 0x55 &&
                                    m_cRecvBuffer[nCurReadIndex - 2] == (byte) 0x55) {
                                //找到包尾，将剩余数据拷贝到起始位置，这种情况也会应对于数据协议包头出现丢包情况
                                nWriteIndex = nWriteIndex - nCurReadIndex - 2;
                                System.arraycopy(m_cRecvBuffer, nCurReadIndex + 2, m_cRecvBuffer, 0, nWriteIndex);
                                m_atomWriteIndex.getAndSet(nWriteIndex);
                                m_atomTailIndex.getAndSet(nCurReadIndex + 1);
                            }
                        }
                        nLoop--;
                        if (nLoop == 0) {
                            //缓冲区的数据已经全部校验过，既没有包头，也没有包尾，认为是非协议数据直接清空缓冲区然后使用
                            m_atomWriteIndex.getAndSet(0);
                            m_atomReadIndex.getAndSet(0);
                            m_atomBuffDataLen.getAndSet(0);
                            m_atomTailIndex.getAndSet(m_nRecvBuffLen);
                        }
                    }
                    //找到这一包不完整数据的起始位置，先拷贝至缓冲区头部，再将新数据拷贝到制定位置

                }
            } else if (nCurBuffLen > 0) {
                System.arraycopy(m_cRecvBuffer, m_atomReadIndex.get(), m_cRecvBuffer, 0, nCurBuffLen);
                m_atomWriteIndex.getAndSet(nCurBuffLen);
                m_atomReadIndex.getAndSet(0);
            } else {
                m_atomWriteIndex.getAndSet(0);
                m_atomReadIndex.getAndSet(0);
            }


            if (nCurBuffLen > m_nRecvBuffLen / 3 * 2) {
                //如果缓冲区域的数据超过2/3没有处理，那么认为读线程没有工作，那么初始化缓冲区状态。
                m_atomReadIndex.getAndSet(0);
                m_atomWriteIndex.getAndSet(0);
                m_atomBuffDataLen.getAndSet(0);
                nWriteIndex = 0;
            }
            System.arraycopy(data, 0, m_cRecvBuffer, nWriteIndex, data.length);
            m_atomBuffDataLen.addAndGet(data.length);
        }
    }

    private static void Print2Hex(byte []data, int len) {
        String print = "";
        if (data.length >= len) {
            for (int i = 0; i < len; i++) {
                String strTmp = Integer.toHexString(data[i] & 0xFF).toUpperCase();
                print += strTmp + " ";
            }
        }
        Log.e("DeamonProcess", "W Protocol : " + print);
    }

    public void updateReceivedDataExt(byte[] data) {
        if (data.length > 0) {
            if (data.length > m_nRecvEmptyLen) {

            }
            int nWriteIndex = m_atomWriteIndex.getAndAdd(data.length);

            //剩余空间不足以放下接收到的数据，分多次接收
            if (m_nRecvEmptyLen - nWriteIndex > data.length) {
                int nIndexData = 0;
                int nTailLen = 0;
                while (nIndexData != data.length) {
                    try {
                        nTailLen = m_nRecvEmptyLen - nWriteIndex;
                        if (nTailLen > data.length - nIndexData) {
                            //剩余空间放得下
                            System.arraycopy(data, nIndexData, m_cRecvBuffer, nWriteIndex, data.length - nIndexData);
                            nIndexData = data.length;
                        } else if (nTailLen > 0) {
                            //剩余空间放不下，先填充完剩余空间
                            System.arraycopy(data, nIndexData, m_cRecvBuffer, nWriteIndex, nTailLen);
                            nIndexData += nTailLen;
                        }
                    } catch (Exception e) {
                        Log.e("SingleInstance", "DeamonProcess 1 " + e.toString());
                    }

                    //写指针更新
                    nWriteIndex += (data.length);

//                    HashMap<String, String> params = new HashMap<>();
//                    //params.put("DrawViewBuff", "SpO2 " + Integer.toString(nSpO2BufferLen) + ", ECG " + Integer.toString(nECGBufferLen) + ", Resp " + Integer.toString(nRespBufferLen));
//                    params.put("RecvDemonListen", "This Recv Len is " + data.length);
//                    EventBus.getDefault().post(params);

                    //缓冲区长度更新
                    m_atomBuffDataLen.getAndAdd(data.length);

                    //记录需要写入文件的数据长度
                    m_atomFileDataLen.getAndAdd(data.length);

                    //Log.e("SingleInstance", "DeamonProcess  1  " + m_nRecvBuffLen + "  " + nWriteIndex + "  " + m_nRecvEmptyLen + "  " + (m_nRecvBuffLen - nWriteIndex));

                    //判断尾部剩余空间是否可以容纳最大包长度
                    if (m_nRecvBuffLen - nWriteIndex <= m_nRecvEmptyLen) {
                        Log.e("SingleInstance", "DeamonProcess  2 " + m_nRecvBuffLen + "  " + nWriteIndex + "  " + m_nRecvEmptyLen + "  " + (m_nRecvBuffLen - nWriteIndex));
                        m_atomTailIndex.getAndSet(nWriteIndex - 1);
                        m_atomWriteIndex.getAndSet(0);
                    }
                }
            } else {
                //Log.e("RecvData", "DeamonProcess data len is " + data.length + " WriteIndex is " + nWriteIndex);
                //Print2Hex(data, data.length);
                try {
                    System.arraycopy(data, 0, m_cRecvBuffer, nWriteIndex, data.length);
                } catch (Exception e) {
                    Log.e("SingleInstance", "DeamonProcess 3" + e.toString());
                }

                //写指针更新
                nWriteIndex += (data.length);

//                HashMap<String, String> params = new HashMap<>();
//                //params.put("DrawViewBuff", "SpO2 " + Integer.toString(nSpO2BufferLen) + ", ECG " + Integer.toString(nECGBufferLen) + ", Resp " + Integer.toString(nRespBufferLen));
//                params.put("RecvDemonListen", "This Recv Len is " + data.length);
//                EventBus.getDefault().post(params);

                //缓冲区长度更新
                m_atomBuffDataLen.getAndAdd(data.length);

                //记录需要写入文件的数据长度
                m_atomFileDataLen.getAndAdd(data.length);

                //Log.e("SingleInstance", "DeamonProcess  1  " + m_nRecvBuffLen + "  " + nWriteIndex + "  " + m_nRecvEmptyLen + "  " + (m_nRecvBuffLen - nWriteIndex));

                //判断尾部剩余空间是否可以容纳最大包长度
                if (m_nRecvBuffLen - nWriteIndex <= m_nRecvEmptyLen) {
                    Log.e("SingleInstance", "DeamonProcess  4 " + m_nRecvBuffLen + "  " + nWriteIndex + "  " + m_nRecvEmptyLen + "  " + (m_nRecvBuffLen - nWriteIndex));
                    m_atomTailIndex.getAndSet(nWriteIndex - 1);
                    m_atomWriteIndex.getAndSet(0);
                }
            }

            //将数据添加至Tcp发送缓冲区
            if (m_MyTcpTransfer != null) {
                if (m_MyTcpTransfer.getConnectedSta()) {
                    int nTcpBuffWriteIndex = m_atomTcpBuffWriteIndex.get();
                    int nTcpBuffTailLen = m_nTcpSendBuffLen - nTcpBuffWriteIndex;
                    if (nTcpBuffTailLen >= data.length) {
                        //直接拷贝
                        System.arraycopy(data, 0, m_cTcpSendBuffer, nTcpBuffWriteIndex, data.length);
                        m_atomTcpBuffWriteIndex.addAndGet(data.length);
                        m_atomTcpBuffLen.addAndGet(data.length);
                    } else {
                        //分两次拷贝
                        Log.e(" bodystmLiuYue ", "1---W : " + nTcpBuffWriteIndex + ", tail : " + nTcpBuffTailLen + ", Data len : " + data.length);
                        System.arraycopy(data, 0, m_cTcpSendBuffer, nTcpBuffWriteIndex, nTcpBuffTailLen);
                        Log.e(" bodystmLiuYue ", "2---W : " + nTcpBuffWriteIndex + ", tail : " + nTcpBuffTailLen + ", Data len : " + data.length);
                        System.arraycopy(data, nTcpBuffTailLen, m_cTcpSendBuffer, 0, data.length - nTcpBuffTailLen);
                        m_atomTcpBuffWriteIndex.set(data.length - nTcpBuffTailLen);
                        m_atomTcpBuffLen.addAndGet(data.length);
                    }
                } else {
                }
            }
        }
    }

    public final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                }

                @Override
                public void onNewData(final byte[] data) {
                    updateReceivedDataExt(data);
                    CSingleInstance mSingIn = CSingleInstance.getInstance();
                    int nWriteIndex = mSingIn.m_atomWriteIndex.getAndAdd(data.length);
                    System.arraycopy(data, 0, mSingIn.m_cRecvBuffer, nWriteIndex, nWriteIndex);
                    mSingIn.m_atomBuffDataLen.addAndGet(data.length);
                }
            };

    public void stopIoManager() {
        if (mSerialIoManager != null) {
//            Toast.makeText(MainActivity.this, "Stopping io manager ..", Toast.LENGTH_SHORT).show();
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    public void startIoManager() {
        if (mPort != null) {
//            Toast.makeText(MainActivity.this, "Starting io manager ..", Toast.LENGTH_SHORT).show();
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
        }
    }

    public final int m_nRecvBuffLen = 1024 * 100;
    public final int m_nRecvEmptyLen = 256;
    public final int m_nFileWriteBase = m_nRecvBuffLen / 32;    //8KB存储一次
    public byte[] m_cRecvBuffer = new byte[m_nRecvBuffLen];
    public AtomicInteger m_atomReadIndex = new AtomicInteger(0);
    public AtomicInteger m_atomWriteIndex = new AtomicInteger(0);
    public AtomicInteger m_atomBuffDataLen = new AtomicInteger(0);
    public AtomicInteger m_atomTailIndex = new AtomicInteger(0);
    public AtomicInteger m_atomFileReadIndex = new AtomicInteger(0);
    public AtomicInteger m_atomFileDataLen = new AtomicInteger(0);

    //前端生命体征数值显示
    public static final int VitalSign4SpO2 = 0;
    public static final int VitalSign4PR = 1;
    public static final int VitalSign4PI = 2;
    public static final int VitalSignHR = 3;
    public static final int VitalSignRR = 4;
    public static final int VitalSignTemp = 5;
    public static final int VitalSignSBP = 6;
    public static final int VitalSignDBP = 7;
    public static final int VitalSignABP = 8;
    public static final int VitalSignPWV = 9;
    public static final int VitalSignTypeMax = 23;      //10

    public static final int SampledDataRed = 0;
    public static final int SampledDataIred = 1;
    public static final int SampledDataECGI = 2;
    public static final int SampledDataECGII = 3;
    public static final int SampledDataECGIII = 4;
    public static final int SampledDataECGV = 5;
    public static final int SampledDataECGavR = 6;
    public static final int SampledDataECGavF = 7;
    public static final int SampledDataECGavL = 8;
    public static final int SampledDataResp = 9;
    public static final int SampledDataPWV1 = 10;
    public static final int SampledDataPWV2 = 11;
    public static final int SampledDataTypeMax = 22;    //12
    public double m_dVitalSign[] = new double[VitalSignTypeMax];
    public double m_dVitalSign4Record[] = new double[VitalSignTypeMax];
    public int m_dNoDataContinueTime = 0;

    //设备UUID
    public static final int UUID_TEMP_FFF0 = 0xFFF0;
    public static final int UUID_TEMP_FFF1 = 0xFFF1;
    public static final int UUID_SpO2S_1910 = 0x1910;
    public static final int UUID_SpO2L_1911 = 0x1911;
    public static final int UUID_ECG_3001E_180F = 0x180F;
    public static final int UUID_ECG_3001C_180E = 0x180E;
    public static final int UUID_HPMS_1D10 = 0x1D10;
    public static final int UUID_Medxing_FFB0 = 0xFFB0;
    public static final int UUID_TKBP_FFE0 = 0xFFE0;

    //前端生命体征波形绘制
    //SpO2
    public static final int ViewArrayLen = 1024;
    public AtomicInteger m_atomSpO2RIndex = new AtomicInteger(0);
    public AtomicInteger m_atomSpO2WIndex = new AtomicInteger(0);
    public AtomicInteger m_atomSpO2Len = new AtomicInteger(0);
    //ECG
    public AtomicInteger m_atomECGRIndex = new AtomicInteger(0);
    public AtomicInteger m_atomECGWIndex = new AtomicInteger(0);
    public AtomicInteger m_atomECGLen = new AtomicInteger(0);
    //Resp
    public AtomicInteger m_atomRespRIndex = new AtomicInteger(0);
    public AtomicInteger m_atomRespWIndex = new AtomicInteger(0);
    public AtomicInteger m_atomRespLen = new AtomicInteger(0);

    public double m_dViewData[][] = new double[SampledDataTypeMax][ViewArrayLen];
    public int m_nViewDataSta[][] = new int[SampledDataTypeMax][ViewArrayLen];      //0:Real 1:Add

    public CMyDeamonProcess m_MyDeamonProcess = null;
    public CMyTcpTransfer m_MyTcpTransfer = null;

    //Tcp 转发服务使用
    public final int m_nTcpSendBuffLen = 1024 * 10;
    public AtomicInteger m_atomTcpBuffReadIndex = new AtomicInteger(0);
    public AtomicInteger m_atomTcpBuffWriteIndex = new AtomicInteger(0);
    public AtomicInteger m_atomTcpBuffLen = new AtomicInteger(0);
    public byte[] m_cTcpSendBuffer = new byte[m_nTcpSendBuffLen];

    //记录是否进行默认的通道选择（在设备连接后第一次有数据时选择有数据的Ecg通道）
    public boolean needAutoSelectEcgChannel = true;
    public long disconnectTime = System.currentTimeMillis();
    //记录心电通道选择下拉item
    public int ecgChannelItem = 0;
    //心电通道下拉对应的绘图m_dViewData选择
    public int ecgViewDataItem = SampledDataECGII;

    public Handler mainActiveHandler = null;



}
