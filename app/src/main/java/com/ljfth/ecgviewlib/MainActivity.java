package com.ljfth.ecgviewlib;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.usb.UsbDeviceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.algorithm4.library.algorithm4library.Algorithm4Library;
import com.algorithm4.library.algorithm4library.Algorithm4SensorLib;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.ljfth.ecgviewlib.BackUsing.CSingleInstance;
import com.ljfth.ecgviewlib.base.BaseActivity;
import com.ljfth.ecgviewlib.base.UsbService;
import com.ljfth.ecgviewlib.utils.PermissionUtils;
import com.ljfth.ecgviewlib.utils.StringUtils;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import android.view.MotionEvent;
import android.view.View;

import butterknife.BindView;
import butterknife.OnClick;


public class MainActivity extends BaseActivity implements View.OnTouchListener {
    private RelativeLayout graph_father;
    private LinearLayout linearLayout;
    private EcgWaveView bcgWaveViewSpO2;
    private EcgWaveView bcgWaveViewECG;
    private EcgWaveView bcgWaveViewCNIBP;
    private EcgWaveView bcgWaveViewResp;
    private View view;

    private TextView mTitleTextView;
    private TextView mUsbLog;
    private TextView mTemperature;

    private TextView mTextViewHR;
    private TextView mTextViewRate;
    private TextView mTextViewBPM;
    private TextView mTextViewmmHg;
    private TextView mTextViewNibpMBP;
    private TextView mTextViewRR;
    private TextView mTextViewPI;

    private TextView mTextViewSpO2Ctl;
    private TextView mTextViewEcgCtl;
    private TextView mTextViewTempCtl;
    private TextView mTextViewNibpCtl;
    private Spinner mSpinnerNibpCtl;
    private Spinner mSpinnerEcgChannel;
    private int mNibpCtlStopJump = 0;   //如果大于0，表示程序内部控制的停止，否则为外部按钮控制的停止
    private TextView spo2View;

    private String mVersionName = "1.7.9";
    private TextView mTextVersionShow;


    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.text_title)
    TextView textTitle;

    private int dataCount = 0;
    private byte[][] Data_ = new byte[10][112];
    private int dataSaveCount = 0;
    private String dataSavePath;
    private static int SaveCountMax = 1000;

    private FileOutputStream outputStream;
    private BufferedOutputStream bufferedOutputStream;
    private Intent mServiceIntent;

    private UsbConnection mConnection = new UsbConnection();
    private UsbService.UsbBinder mBinder;

    //绘图Timer
    private Timer DrawTimer;

    //串口发送队列

    //媒体播放器
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private String[] mItems;
    private String[] mEcgChannalList;

    //Handler
    private final int RefreshView = 0;  //轮询一次刷新
    private final int RefreshView2 = 1; //轮询两次刷新
    private final int AdjustTimer = 3;  //调整轮询间隔为理论计算值
    private final int AdjustTimerAdd = 4; //调整轮询间隔为实际调整值，加快Timer
    private final int AdjustTimerSub = 5; //调整轮询间隔为实际调整值，减少Timer
    private final int SetDevSta = 6;    //设置界面状态信息
    private final int AdjustStatus = 7; //输出当前调整状态
    private final int AutoSelectEcgChannel = 10; //自动选择有数据的ECG通道

    byte[] getCentorMac() {
        byte [] tmp = new byte[6];
        //Algorithm4Library.GetCentorMac(tmp);
        Algorithm4SensorLib.GetCentorMac(tmp);
        return tmp;
    }

    static private int myi = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (bUSB_Sta == true) {
                mTitleTextView.setText("USB ACCESS");
                byte[] byteMac = getCentorMac();
                String CentorMac = String.format("%02X%02X%02X%02X%02X%02X", byteMac[0], byteMac[1], byteMac[2], byteMac[3], byteMac[4], byteMac[5]);
                mTextVersionShow.setText(mVersionName + " (" + CentorMac +")");
            } else {
//                mTextVersionShow.setText(mVersionName + " - " + myi++);
                byte[] byteMac = getCentorMac();
                String CentorMac = String.format("%02X%02X%02X%02X%02X%02X", byteMac[0], byteMac[1], byteMac[2], byteMac[3], byteMac[4], byteMac[5]);
                mTextVersionShow.setText(mVersionName + "   " + CentorMac);
            }
            switch (msg.what) {
                case SetDevSta:
                    //DealDevConSta();
                    DealDevConStaExt();
                    break;
                case RefreshView2:
                    if (bcgWaveViewResp != null)
                        bcgWaveViewResp.invalidate();
                case RefreshView:
                    if (bcgWaveViewSpO2 != null)
                        bcgWaveViewSpO2.invalidate();
                    if (bcgWaveViewECG != null)
                        bcgWaveViewECG.invalidate();
                    //bcgWaveView3.invalidate();
                    break;
                case AdjustTimer: {
                    if (DrawTimer != null) {
                        DrawTimer.cancel();
                    }
                    DrawTimer = new Timer();
                    m_TimerLoop = new MyTimerTask();
                    int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (1.0));
                    //spo2View.setText("Loop1 " + loopTimer);
                    Log.e("TimerFlag", "TimerLoop " + loopTimer);
                    DrawTimer.schedule(m_TimerLoop, loopTimer, loopTimer);
                }
                break;
                case AdjustTimerAdd: {
                    if (DrawTimer != null) {
                        DrawTimer.cancel();
                    }
                    DrawTimer = new Timer();
                    m_TimerLoop = new MyTimerTask();
                    int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (0.8));
                    Log.e("TimerFlag", "TimerLoop " + loopTimer);
                    //spo2View.setText("Loop2 " + loopTimer);
                    try {
                        DrawTimer.schedule(m_TimerLoop, loopTimer, loopTimer);
                    } catch (Exception e) {
                        Log.e("TimerFlag", e.toString());
                        throw e;
                    }
                }
                break;
                case AdjustTimerSub: {
                    if (DrawTimer != null) {
                        DrawTimer.cancel();
                    }
                    DrawTimer = new Timer();
                    m_TimerLoop = new MyTimerTask();
                    int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (1.2));
                    Log.e("TimerFlag", "TimerLoop " + loopTimer);
                    //spo2View.setText("Loop2 " + loopTimer);
                    try {
                        DrawTimer.schedule(m_TimerLoop, loopTimer, loopTimer);
                    } catch (Exception e) {
                        Log.e("TimerFlag", e.toString());
                        throw e;
                    }
                }
                break;
                case AdjustStatus: {
                    spo2View.setText("Normal");
                }
                break;
                case AdjustStatus + 1: {
                    spo2View.setText("Low");
                }
                break;
                case AdjustStatus + 2: {
                    spo2View.setText("High");
                }
                break;
                case AutoSelectEcgChannel: {
                    //选择Ecg通道
                    if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGI) {
                        mSpinnerEcgChannel.setSelection(0, true);
                    } else if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGII) {
                        mSpinnerEcgChannel.setSelection(1, true);
                    } else if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGV) {
                        mSpinnerEcgChannel.setSelection(2, true);
                    }
                }
                break;
                default:
                    break;
            }
            RefreshVitalSignVal();
        }
    };

    private boolean bLastNibpState = true; //上一次测量的状态，false失败，true成功
    private int nLastSBP = -1, nLastDBP = -1, nLastMBP = -1;
    private int nStateSaveCount = 0;    //确保某一个状态只执行一次
    private int nNibpStopIsFound = 0;
    private boolean bNibpParamRefresh = false;  //决定血压的测量结果是否需要更新，false 不需要，true 需要

    private void RefreshVitalSignVal() {
        if (!bUSB_Sta) {
            mTextViewEcgCtl.setText("ECG OFF");
            mTextViewSpO2Ctl.setText("SpO2 OFF");
            mTextViewTempCtl.setText("Temp OFF");
            if (Constant.NIBP_FUNC_ENABLE) {
                mTextViewNibpCtl.setText("NIBP OFF");
            }
        }
        //ECG
        if (mTextViewHR != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSignHR] == -1 || mTextViewEcgCtl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTextViewHR.setText("---");
            } else {
                mTextViewHR.setText(String.format("%d", (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSignHR])));
            }
        }
        //Resp
        if (mTextViewRR != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSignRR] == -1 || mTextViewEcgCtl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTextViewRR.setText("---");
            } else {
                mTextViewRR.setText(String.format("%d", (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSignRR])));
            }
        }
        //SpO2
        if (mTextViewRate != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSign4SpO2] == -1 || mTextViewSpO2Ctl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTextViewRate.setText("---");
            } else {
                mTextViewRate.setText(String.format("%.0f", gTransferData.m_dVitalSign[CSingleInstance.VitalSign4SpO2]));
            }
        }
        //PR
        if (mTextViewBPM != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSign4PR] == -1 || mTextViewSpO2Ctl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTextViewBPM.setText("---");
            } else {
                mTextViewBPM.setText(String.format("%d", (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSign4PR])));
            }
        }
        //PI
        if (mTextViewPI != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSign4PI] == -1 || mTextViewSpO2Ctl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTextViewPI.setText("---");
            } else {
                mTextViewPI.setText(String.format("%.1f", gTransferData.m_dVitalSign[CSingleInstance.VitalSign4PI]));
            }
        }
        //Temp
        if (mTemperature != null) {
            if (gTransferData.m_dVitalSign[CSingleInstance.VitalSignTemp] == -1 || mTextViewTempCtl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                mTemperature.setText("---");
            } else {
                mTemperature.setText(String.format("%.1f", gTransferData.m_dVitalSign[CSingleInstance.VitalSignTemp]));
            }
        }
        //SBP&&DBP
        if (Constant.NIBP_FUNC_ENABLE) {
            if (mTextViewmmHg != null) {
                {
                    int nSBP = (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSignSBP]);
                    int nDBP = (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSignDBP]);
                    int nABP = (int) (gTransferData.m_dVitalSign[CSingleInstance.VitalSignABP]);
//                    Log.i("liuyue", "SBP " + nSBP + ", DBP " + nDBP + ", ABP " + nABP);
//                    if (mSpinnerNibpCtl.getSelectedItem().toString().equals("停止")) {
//                        if (!bLastNibpState) {
//                            nDBP = -1;
//                            nSBP = 6;
//                        } else {
////                            nSBP = nLastSBP;
////                            nDBP = nLastDBP;
////                            nABP = nLastMBP;
//                        }
//                    }

//                   nLastSBP = nSBP2;
//                   nLastDBP = nDBP2;
//                   nLastMBP = nABP2;
//
//                   if (mSpinnerNibpCtl.getSelectedItem().toString().equals("停止")) {
//                        if (nLastDBP == -1) {
//                            if (nLastSBP == 1 || nLastSBP == 2 || nLastSBP == 3 || nLastSBP == 5 ) {
//                                nSBP = nLastSBP;
//                                nDBP = nLastDBP;
//                            }
//                        }
//                   }
//

                    //只有识别USB模块以后算法句柄才正常工作，所以当还没接如模块时手动修改值，显示设备未连接
                    if (mTextViewNibpCtl.getText().toString().endsWith("OFF") || !bUSB_Sta) {
                        nDBP = -1;
                        nSBP = 3;
                        nLastSBP = nSBP;
                        nLastDBP = nDBP;
                        nLastMBP = nABP;
                    } else if (mTextViewNibpCtl.getText().toString().endsWith("ON") && mTextViewmmHg.getText().toString().endsWith("设备未连接")) {
                        nDBP = -1;
                        nSBP = 5;
                        nLastSBP = nSBP;
                        nLastDBP = nDBP;
                        nLastMBP = nABP;
                    }

                    if (nDBP == -1) {
                        //命令码解析
                        switch (nSBP) {
                            case 0: {
                                //开始测量
                                mTextViewmmHg.setText("开始测量");
                                bLastNibpState = false;
                                nStateSaveCount = 0;
                                nNibpStopIsFound = 0;
                                //收到开始测量命令返回，需要刷新结果。
                                bNibpParamRefresh = true;
                            }
                            break;
                            case 1: {
                                //下位机设备未响应
                                mTextViewmmHg.setText("设备未响应");
                                mTextViewNibpMBP.setText("");
                                //mSpinnerNibpCtl.setSelection(1, true);
//                                nLastSBP = nSBP2;
//                                nLastDBP = nDBP2;
//                                nLastMBP = nABP2;
                                bLastNibpState = true;
                                if (mSpinnerNibpCtl.getSelectedItem().toString().equals("开始")
                                        && !mSpinnerNibpCtl.getSelectedItem().toString().equals("停止") && nStateSaveCount != 1) {
                                    mNibpCtlStopJump = 1;
                                    mSpinnerNibpCtl.setSelection(1);
                                }
                                nStateSaveCount = 1;
                                nNibpStopIsFound = 0;

                            }
                            break;
                            case 2: {
                                //测量失败
                                mTextViewmmHg.setText("测量失败");
                                mTextViewNibpMBP.setText("");
                                //mSpinnerNibpCtl.setSelection(1, true);
                                bLastNibpState = true;
                                if (mSpinnerNibpCtl.getSelectedItem().toString().equals("开始")
                                        && !mSpinnerNibpCtl.getSelectedItem().toString().equals("停止") && nStateSaveCount != 2) {
                                    mNibpCtlStopJump = 1;
                                    mSpinnerNibpCtl.setSelection(1);
                                }
                                nStateSaveCount = 2;
                                nNibpStopIsFound = 0;
                            }
                            break;
                            case 3: {
                                //蓝牙未连接
                                mTextViewmmHg.setText("设备未连接");
                                mTextViewNibpMBP.setText("");
                                //mSpinnerNibpCtl.setSelection(1, true);
//                                nLastSBP = nSBP2;
//                                nLastDBP = nDBP2;
//                                nLastMBP = nABP2;
                                bLastNibpState = true;
                                if (mSpinnerNibpCtl.getSelectedItem().toString().equals("开始")
                                        && !mSpinnerNibpCtl.getSelectedItem().toString().equals("停止") && nStateSaveCount != 3) {
                                    mNibpCtlStopJump = 1;
                                    mSpinnerNibpCtl.setSelection(1);
                                }
                                nStateSaveCount = 3;
                                nNibpStopIsFound = 0;

                            }
                            break;
                            case 4: {
                                //显示默认值--/--
//                                mTextViewmmHg.setText("--/--");
//                                mTextViewNibpMBP.setText("");
//                                bLastNibpState = true;
                            }
                            break;
                            case 5: {
                                //连接已恢复
                                mTextViewmmHg.setText("连接已恢复");
                                mTextViewNibpMBP.setText("");
                                //mSpinnerNibpCtl.setSelection(1, true);
//                                nLastSBP = nSBP2;
//                                nLastDBP = nDBP2;
//                                nLastMBP = nABP2;
                                bLastNibpState = true;
                                if (mSpinnerNibpCtl.getSelectedItem().toString().equals("开始")
                                        && !mSpinnerNibpCtl.getSelectedItem().toString().equals("停止") && nStateSaveCount != 5) {
                                    mNibpCtlStopJump = 1;
                                    mSpinnerNibpCtl.setSelection(1);
                                }
                                nStateSaveCount = 5;
                                nNibpStopIsFound = 0;

                            }
                            break;
                            case 6: {
                                //停止测量
                                //if (nStateSaveCount != 6) {
                                mTextViewmmHg.setText("停止测量");
                                mTextViewNibpMBP.setText("");
                                bLastNibpState = true;
                                //}
                                nNibpStopIsFound = 1;
                                nStateSaveCount = 6;
                            }
                            break;
                            default:
                                break;
                        }
                    } else if (nDBP == 65535) {
                        //if (nNibpStopIsFound != 1) {
                        mTextViewmmHg.setText("" + nSBP);
                        mTextViewNibpMBP.setText("");
                        bLastNibpState = false;
                        //收到实时数据时，需要刷新结果。
                        bNibpParamRefresh = true;

                        // }
                    } else {
                        mTextViewmmHg.setText((nSBP) + "/" + (nDBP));
                        mTextViewNibpMBP.setText("(" + nABP + ")");
                        bLastNibpState = true;
//                        nLastSBP = nSBP2;
//                        nLastDBP = nDBP2;
//                        nLastMBP = nABP2;
                        Log.e("liuyue", "=========mSpinnerNibpCtl.getSelectedItem().toString()========" + mSpinnerNibpCtl.getSelectedItem().toString());
                        if (mSpinnerNibpCtl.getSelectedItem().toString().equals("开始")
                                && !mSpinnerNibpCtl.getSelectedItem().toString().equals("停止") && bNibpParamRefresh) {
                            mNibpCtlStopJump = 1;
                            // 错误的，这个要你自定义
                            SpinnerAdapter adapter = mSpinnerNibpCtl.getAdapter();

//                            // 建立数据源
//
//                            // 建立Adapter并且绑定数据源
                            //ArrayAdapter<String> _Adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mItems);
//                            //绑定 Adapter到控件
                           // mSpinnerNibpCtl.setAdapter(_Adapter);
                            mSpinnerNibpCtl.setSelection(1, true);
//                            mSpinnerNibpCtl.clearAnimation();
                            bNibpParamRefresh = false;
                        }
                    }
                }
            }
        }
    }

    //TimerTask
    private int TimerTaskLoopCount = 0;
    int nNewY = 100;
    int nDrawFlag4SpO2 = 0, nDrawFlag4ECG = 0, nDrawFlag4Resp = 0;
    int nBaseBufferLen = Constant.TagetSampled4Draw * 2;    //默认缓冲数据长度2S

    int nSpO2NormalStatus = 0;  //当血氧绘图缓冲区达到正常绘图（绘图缓冲区首次达到预存储长度，给动态调整函数CheckDrawBuffLen使用）0:不满足，1:满足
    int nEcgNormalStatus = 0;   //当心电绘图缓冲区达到正常绘图（绘图缓冲区首次达到预存储长度，给动态调整函数CheckDrawBuffLen使用）0:不满足，1:满足
    int nRespNormalStatus = 0;  //当呼吸绘图缓冲区达到正常绘图（绘图缓冲区首次达到预存储长度，给动态调整函数CheckDrawBuffLen使用）0:不满足，1:满足

    long lLastTime = System.currentTimeMillis();

    boolean m_bMyTimerTaskRunningFlag = true;

    //记录HPMS设备连接状态，区别血压发送指令类型
    private boolean bHpmsIsConnected = false;

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            //判斷Mesure
            if (mSpinnerNibpCtl.getSelectedItemPosition() == 2) {
                if (System.currentTimeMillis() - lLastTime >= 5 * 60 * 1000) {
                    lLastTime = System.currentTimeMillis();
                    //构建指令的预备数据
                    byte[] transfer = new byte[256];
                    //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    byte[] nibpSensorMac = gTransferData.m_MyDeamonProcess.getNibpSensorMac();
                    byte[] hpmsSensorMac = gTransferData.m_MyDeamonProcess.getHpmsSensorMac();
                    if (bHpmsIsConnected) {
                        System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                    } else {
                        System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                    }
                    Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    if (transfer[0] > 0 && transfer[0] < 256) {
                        byte[] Send = new byte[transfer[0]];
                        System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                        if (writeIoManage(Send) == true) {
                            System.out.printf("发送开始测量数据 ... %d bytes\n", transfer[0]);
                        } else {
                            System.out.printf("发送开始测量数据 ... 失败\n");
                        }
                    } else {
                        //show error
                        System.out.printf("构建指令错误 ... 失败\n");
                    }
                }
            } else if (mSpinnerNibpCtl.getSelectedItemPosition() == 3) {
                if (System.currentTimeMillis() - lLastTime >= 10 * 60 * 1000) {
                    lLastTime = System.currentTimeMillis();
                    //构建指令的预备数据
                    byte[] transfer = new byte[256];
                    //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    byte[] nibpSensorMac = gTransferData.m_MyDeamonProcess.getNibpSensorMac();
                    byte[] hpmsSensorMac = gTransferData.m_MyDeamonProcess.getHpmsSensorMac();
                    if (bHpmsIsConnected) {
                        System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                    } else {
                        System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                    }
                    Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    if (transfer[0] > 0 && transfer[0] < 256) {
                        byte[] Send = new byte[transfer[0]];
                        System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                        if (writeIoManage(Send) == true) {
                            System.out.printf("发送开始测量数据 ... %d bytes\n", transfer[0]);
                        } else {
                            System.out.printf("发送开始测量数据 ... 失败\n");
                        }
                    } else {
                        //show error
                        System.out.printf("构建指令错误 ... 失败\n");
                    }
                }
            } else if (mSpinnerNibpCtl.getSelectedItemPosition() == 4) {
                if (System.currentTimeMillis() - lLastTime >= 15 * 60 * 1000) {
                    lLastTime = System.currentTimeMillis();
                    //构建指令的预备数据
                    byte[] transfer = new byte[256];
                    //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    byte[] nibpSensorMac = gTransferData.m_MyDeamonProcess.getNibpSensorMac();
                    byte[] hpmsSensorMac = gTransferData.m_MyDeamonProcess.getHpmsSensorMac();
                    if (bHpmsIsConnected) {
                        System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                    } else {
                        System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                    }
                    Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                    if (transfer[0] > 0 && transfer[0] < 256) {
                        byte[] Send = new byte[transfer[0]];
                        System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                        if (writeIoManage(Send) == true) {
                            System.out.printf("发送开始测量数据 ... %d bytes\n", transfer[0]);
                        } else {
                            System.out.printf("发送开始测量数据 ... 失败\n");
                        }
                    } else {
                        //show error
                        System.out.printf("构建指令错误 ... 失败\n");
                    }
                }
            } else {
                lLastTime = System.currentTimeMillis();
            }

            if (bcgWaveViewSpO2 != null) {
                //血氧
                nBaseBufferLen = Constant.TagetSampled4Draw * 2;
                int nLenSpO2 = gTransferData.m_atomSpO2Len.getAndAdd(0);
                if (nLenSpO2 > 0) {
                    if (nLenSpO2 >= nBaseBufferLen || nDrawFlag4SpO2 != 0) {
                        if (nLenSpO2 >= nBaseBufferLen) {
                            nDrawFlag4SpO2 = 1;
                            nSpO2NormalStatus = 1;
                        }
                        int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);

//                        if (gTransferData.m_nViewDataSta[gTransferData.SampledDataIred][nReadIndexSpO2] == 1) {
//                            bcgWaveViewSpO2.setWaveColor(Color.RED);
//                        } else {
//                            bcgWaveViewSpO2.setWaveColor(Color.argb(255, 43, 172, 246));
//                        }

                        //检测长度是否超过原采样率的3.5倍
                        if (bcgWaveViewSpO2.bCheckIs0Coordinate()) {
                            Log.i("bcgWaveViewSpO2", "SpO2 buf len " + nLenSpO2 + " X is " + bcgWaveViewSpO2.getXCoordinate());
                        }
                        if (nLenSpO2 > Constant.TagetSampled4Draw * 3.5 && nSpO2NormalStatus == 1 && bcgWaveViewSpO2.bCheckIs0Coordinate()) {
                            //删除旧数据，保留长度在采样率的2.5倍
                            int nSubLen = nLenSpO2 - Constant.TagetSampled4Draw * 2;
                            nReadIndexSpO2 += nSubLen;
                            nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                            nNewY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2]);
                            gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                            gTransferData.m_atomSpO2Len.getAndAdd(-nSubLen);
                        } else {
                            nReadIndexSpO2++;
                            nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                            nNewY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2]);
                            gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                            gTransferData.m_atomSpO2Len.getAndAdd(-1);
                        }

                        //设置当前脉率值，当脉率有效则根据脉率间协同采样率获得区间极值
                        bcgWaveViewSpO2.setnRefPauseRageVal((int) gTransferData.m_dVitalSign[CSingleInstance.VitalSign4PR]);
                        bcgWaveViewSpO2.drawWave(nNewY, true);
                        //saveData4Test((Integer.toString(bcgWaveView1.ecgy) + ",").getBytes());
                    } else {
                        nNewY = bcgWaveViewSpO2.getY_max();
//                        bcgWaveViewSpO2.setWaveColor(Color.WHITE);
                        bcgWaveViewSpO2.drawWave(nNewY, false);
                    }
                } else {
                    nDrawFlag4SpO2 = 0;
                    nSpO2NormalStatus = 0;
                    nNewY = bcgWaveViewSpO2.getY_max();
//                    bcgWaveViewSpO2.setWaveColor(Color.BLUE);
                    bcgWaveViewSpO2.drawWave(nNewY, false);
                }
            }

            //心电
            if (bcgWaveViewECG != null) {
                nBaseBufferLen = Constant.TagetSampled4Draw * 2;
                int nLenECG = gTransferData.m_atomECGLen.getAndAdd(0);
                if (nLenECG > 0) {
                    if (nLenECG >= nBaseBufferLen || nDrawFlag4ECG != 0) {
                        if (nLenECG >= nBaseBufferLen) {
                            nDrawFlag4ECG = 1;
                            nEcgNormalStatus = 1;
                        }
                        int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);

                        //检测长度是否超过原采样率的3倍
                        if (bcgWaveViewECG.bCheckIs0Coordinate()) {
                            Log.i("bcgWaveViewECG", "Ecg buf len " + nLenECG + " X is " + bcgWaveViewECG.getXCoordinate());
                        }
                        if (nLenECG > Constant.TagetSampled4Draw * 3.5 && nEcgNormalStatus == 1 && bcgWaveViewECG.bCheckIs0Coordinate()) {
                            //删除旧数据，保留长度在采样率的2.5倍
                            int nSubLen = nLenECG - Constant.TagetSampled4Draw * 2;
                            nReadIndexECG += nSubLen;
                            nReadIndexECG %= CSingleInstance.ViewArrayLen;
                            nNewY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG]);
                            gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                            gTransferData.m_atomECGLen.getAndAdd(-nSubLen);
                        } else {
                            nReadIndexECG++;
                            nReadIndexECG %= CSingleInstance.ViewArrayLen;
                            nNewY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG]);
                            gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                            gTransferData.m_atomECGLen.getAndAdd(-1);
                        }


                        //mUsbLog.setText("ECG " + nLenECG);

                        //bcgWaveView2.drawWave(20000);
                        //saveData4Test((Integer.toString(nNewY) + ",").getBytes());
//                        bcgWaveViewECG.setWaveColor(Color.GREEN);
                        bcgWaveViewECG.drawWave(-nNewY, true);
                        //Log.i("ECGDraw", "ECG buf len " + nLenECG + " Src " + nNewY + " Src2 " + tmp + " Des " + bcgWaveView2.ecgy + " Min " + bcgWaveView2.y_min + " Max " + bcgWaveView2.y_max);
                    } else {
                        nNewY = 0;
//                        bcgWaveViewECG.setWaveColor(Color.WHITE);
                        bcgWaveViewECG.drawWave(nNewY, false);
                    }
                } else {
                    nDrawFlag4ECG = 0;
                    nEcgNormalStatus = 0;
                    nNewY = 0;
//                    bcgWaveViewECG.setWaveColor(Color.WHITE);
                    //saveData4Test((Integer.toString(nNewY) + ",").getBytes());
                    bcgWaveViewECG.drawWave(nNewY, false);
                }
            }

            //血压
            if (bcgWaveViewCNIBP != null) {
                nNewY = bcgWaveViewCNIBP.getY_max();
                bcgWaveViewCNIBP.drawWave(nNewY, false);
            }

            //SendMesssage
            TimerTaskLoopCount++;
            TimerTaskLoopCount %= 2;
            if (TimerTaskLoopCount == 0) {
                Message msg = new Message();
                msg.what = RefreshView;
                handler.sendMessage(msg);
            } else {
                //呼吸
                if (bcgWaveViewResp != null) {
                    nBaseBufferLen = (Constant.TagetSampled4Draw / 2) * 2;
                    int nLenResp = gTransferData.m_atomRespLen.getAndAdd(0);
                    if (nLenResp > 0) {
                        if (nLenResp >= nBaseBufferLen || nDrawFlag4Resp != 0) {
                            if (nLenResp >= nBaseBufferLen) {
                                nDrawFlag4Resp = 1;
                                nRespNormalStatus = 1;
                            }
                            int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);

                            //检测长度是否超过原采样率的3倍
                            if (bcgWaveViewResp.bCheckIs0Coordinate()) {
                                Log.i("bcgWaveViewResp", "Resp buf len " + nLenResp + " X is " + bcgWaveViewResp.getXCoordinate());
                            }
                            if (nLenResp > Constant.TagetSampled4Draw / 2 * 3.5 && nEcgNormalStatus == 1 && bcgWaveViewResp.bCheckIs0Coordinate()) {
                                //删除旧数据，保留长度在采样率的2.5倍
                                int nSubLen = nLenResp - Constant.TagetSampled4Draw / 2 * 2;
                                nReadIndexResp += nSubLen;
                                nReadIndexResp %= CSingleInstance.ViewArrayLen;
                                nNewY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp]);
                                gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                                gTransferData.m_atomRespLen.getAndAdd(-nSubLen);
                            } else {
                                nReadIndexResp++;
                                nReadIndexResp %= CSingleInstance.ViewArrayLen;
                                nNewY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp]);
                                gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                                gTransferData.m_atomRespLen.getAndAdd(-1);
                            }

                            nNewY = nNewY * bcgWaveViewResp.getHeight() / 250;
                            bcgWaveViewResp.drawWave(nNewY, true);
                           // Log.i("Resp", ""+nNewY);
                            //saveData4Test((Integer.toString(nNewY) + ",").getBytes());
//                            bcgWaveViewResp.setWaveColor(Color.YELLOW);
                            //saveData4Test((Integer.toString(bcgWaveViewResp.ecgy) + ",").getBytes());
                        } else {
                            nNewY = bcgWaveViewResp.getY_max();
//                            bcgWaveViewResp.setWaveColor(Color.WHITE);
                            bcgWaveViewResp.drawWave(nNewY, false);
                        }
                    } else {
                        nDrawFlag4Resp = 0;
                        nRespNormalStatus = 0;
                        nNewY = bcgWaveViewResp.getY_max();
//                        bcgWaveViewResp.setWaveColor(Color.WHITE);
                        bcgWaveViewResp.drawWave(nNewY, false);
                    }

                }
                Message msg = new Message();
                msg.what = RefreshView2;
                handler.sendMessage(msg);
            }
            CheckDrawBuffLenExt();
            //CheckDrawBuffLen();
        }
    }

    private TimerTask m_TimerLoop = new MyTimerTask();

    private int TimerAdjust = 0;
    long SpO2AdjustTime = 0;
    long ECGAdjustTime = 0;
    long RespAdjustTime = 0;
    long AdjustOverTime = 50;   //ms


    int nSpO2AdjustCount = 0;
    int nECGAdjustCount = 0;
    int nRespAdjustCount = 0;

    void CheckDrawBuffLen() {
        //检测缓冲去长度，动态调整timer间隔
        int nSpO2BuffLen = gTransferData.m_atomSpO2Len.getAndAdd(0);
        int nECGBufferLen = gTransferData.m_atomECGLen.getAndAdd(0);
        int nRespBufferLen = gTransferData.m_atomRespLen.getAndAdd(0);

        //判断缓冲去长度
        if ((nSpO2BuffLen > Constant.TagetSampled4Draw * 3 || nSpO2BuffLen == 0) &&
                (nECGBufferLen > Constant.TagetSampled4Draw * 3 || nECGBufferLen == 0) &&
                (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 3 || nRespBufferLen == 0)) {
            //调整Timer  同增
            if (TimerAdjust != 1) {
                Message msg = new Message();
                msg.what = AdjustTimerAdd;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 2" + Constant.TagetSampled4Draw);
            } else {

            }
            TimerAdjust = 1;

            //这种情况下通过timer和去点两种方式同时调整
            long timeNow = System.currentTimeMillis();
            if (nSpO2BuffLen > Constant.TagetSampled4Draw * 2.5 && nSpO2NormalStatus == 1) {
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    nReadIndexSpO2 += 1;
                    nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(-1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            }
            if (nECGBufferLen > Constant.TagetSampled4Draw * 2.5 && nEcgNormalStatus == 1) {
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    nReadIndexECG += 1;
                    nReadIndexECG %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(-1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            }
            if (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 2.5 && nRespNormalStatus == 1) {
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    nReadIndexResp += 1;
                    nReadIndexResp %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(-1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            }

//            Message msg = new Message();
//            msg.what = AdjustStatus + 2;
//            handler.sendMessage(msg);

        } else if (((nSpO2BuffLen < Constant.TagetSampled4Draw * 1 || nSpO2BuffLen == 0) && (nSpO2NormalStatus == 1)) &&
                ((nECGBufferLen < Constant.TagetSampled4Draw * 1 || nECGBufferLen == 0) && (nEcgNormalStatus == 1)) &&
                ((nRespBufferLen < Constant.TagetSampled4Draw / 2 * 1 || nRespBufferLen == 0) && (nRespNormalStatus == 1))) {
            //调整Timer  同减
            if (TimerAdjust != 2) {
                Message msg = new Message();
                msg.what = AdjustTimerSub;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 2" + Constant.TagetSampled4Draw);
            } else {
                //什么都不做
            }
            TimerAdjust = 2;

//            Message msg = new Message();
//            msg.what = AdjustStatus + 1;
//            handler.sendMessage(msg);
        } else {
            //恢复Timer
            if (TimerAdjust != 0) {
                Message msg = new Message();
                msg.what = AdjustTimer;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 1" + Constant.TagetSampled4Draw);
            } else {
                //什么都不做
            }
            TimerAdjust = 0;

//            Message msg = new Message();
//            msg.what = AdjustStatus;
//            handler.sendMessage(msg);

            //协调不同buffer的长度，这种情况下有个别缓冲区不满足要求，通过增删点来适应
            long timeNow = System.currentTimeMillis();
            if (nSpO2BuffLen > Constant.TagetSampled4Draw * 2.5 && nSpO2NormalStatus == 1) {
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    nReadIndexSpO2 += 1;
                    nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(-1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            } else if (nSpO2BuffLen <= Constant.TagetSampled4Draw * 1 && nSpO2NormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][(nReadIndexSpO2) % CSingleInstance.ViewArrayLen]);
                    nReadIndexSpO2 = (nReadIndexSpO2 + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2]);
                    gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_nViewDataSta[gTransferData.SampledDataIred][nReadIndexSpO2] = 1;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            }

            if (nECGBufferLen > Constant.TagetSampled4Draw * 2.5 && nEcgNormalStatus == 1) {
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    nReadIndexECG += 1;
                    nReadIndexECG %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(-1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            } else if (nECGBufferLen <= Constant.TagetSampled4Draw * 1 && nEcgNormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][(nReadIndexECG) % CSingleInstance.ViewArrayLen]);
                    nReadIndexECG = (nReadIndexECG + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG]);
                    gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            }

            if (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 2.5 && nRespNormalStatus == 1) {
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 5)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    nReadIndexResp += 1;
                    nReadIndexResp %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(-1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            } else if (nRespBufferLen <= Constant.TagetSampled4Draw / 2 * 1 && nRespNormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][(nReadIndexResp) % CSingleInstance.ViewArrayLen]);
                    nReadIndexResp = (nReadIndexResp + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp]);
                    gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            }
        }
    }

    //修改Buffer调整方法为同增加快,同减少减慢，超过制定长度减去删去缓冲数据到预备缓冲长度
    void CheckDrawBuffLenExt() {
        //检测缓冲去长度，动态调整timer间隔
        int nSpO2BuffLen = gTransferData.m_atomSpO2Len.getAndAdd(0);
        int nECGBufferLen = gTransferData.m_atomECGLen.getAndAdd(0);
        int nRespBufferLen = gTransferData.m_atomRespLen.getAndAdd(0);

        //判断缓冲去长度
        if ((nSpO2BuffLen > Constant.TagetSampled4Draw * 3 || nSpO2BuffLen == 0) &&
                (nECGBufferLen > Constant.TagetSampled4Draw * 3 || nECGBufferLen == 0) &&
                (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 3 || nRespBufferLen == 0)) {
            //调整Timer  同增
            if (TimerAdjust != 1) {
                Message msg = new Message();
                msg.what = AdjustTimerAdd;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 2" + Constant.TagetSampled4Draw);
            } else {

            }
            TimerAdjust = 1;

            //TODO去点的操作在绘图操作x坐标为0时处理
            /*
            //这种情况下通过timer和去点两种方式同时调整
            long timeNow = System.currentTimeMillis();
            if (nSpO2BuffLen > Constant.TagetSampled4Draw * 2.5 && nSpO2NormalStatus == 1) {
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    nReadIndexSpO2 += 1;
                    nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(-1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            }
            if (nECGBufferLen > Constant.TagetSampled4Draw * 2.5 && nEcgNormalStatus == 1) {
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    nReadIndexECG += 1;
                    nReadIndexECG %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(-1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            }
            if (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 2.5 && nRespNormalStatus == 1) {
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    nReadIndexResp += 1;
                    nReadIndexResp %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(-1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            }
            */
//            Message msg = new Message();
//            msg.what = AdjustStatus + 2;
//            handler.sendMessage(msg);

        } else if (((nSpO2BuffLen < Constant.TagetSampled4Draw * 1 || nSpO2BuffLen == 0) && (nSpO2NormalStatus == 1)) &&
                ((nECGBufferLen < Constant.TagetSampled4Draw * 1 || nECGBufferLen == 0) && (nEcgNormalStatus == 1)) &&
                ((nRespBufferLen < Constant.TagetSampled4Draw / 2 * 1 || nRespBufferLen == 0) && (nRespNormalStatus == 1))) {
            //调整Timer  同减
            if (TimerAdjust != 2) {
                Message msg = new Message();
                msg.what = AdjustTimerSub;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 2" + Constant.TagetSampled4Draw);
            } else {
                //什么都不做
            }
            TimerAdjust = 2;

//            Message msg = new Message();
//            msg.what = AdjustStatus + 1;
//            handler.sendMessage(msg);
        } else {
            //恢复Timer
            if (TimerAdjust != 0) {
                Message msg = new Message();
                msg.what = AdjustTimer;
                handler.sendMessage(msg);
                Log.e("DrawTimer", "DrawTimer Timer 1" + Constant.TagetSampled4Draw);
            } else {
                //什么都不做
            }
            TimerAdjust = 0;

//            Message msg = new Message();
//            msg.what = AdjustStatus;
//            handler.sendMessage(msg);

            //协调不同buffer的长度，这种情况下有个别缓冲区不满足要求，通过增删点来适应
            long timeNow = System.currentTimeMillis();

            if (nSpO2BuffLen > Constant.TagetSampled4Draw * 2.5 && nSpO2NormalStatus == 1) {
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    nReadIndexSpO2 += 1;
                    nReadIndexSpO2 %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(-1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            } else if (nSpO2BuffLen <= Constant.TagetSampled4Draw * 1 && nSpO2NormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - SpO2AdjustTime > AdjustOverTime) || (nSpO2AdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexSpO2 = gTransferData.m_atomSpO2RIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][(nReadIndexSpO2) % CSingleInstance.ViewArrayLen]);
                    nReadIndexSpO2 = (nReadIndexSpO2 + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2]);
                    gTransferData.m_dViewData[gTransferData.SampledDataIred][nReadIndexSpO2] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_nViewDataSta[gTransferData.SampledDataIred][nReadIndexSpO2] = 1;
                    gTransferData.m_atomSpO2RIndex.getAndSet(nReadIndexSpO2);
                    gTransferData.m_atomSpO2Len.getAndAdd(1);
                    SpO2AdjustTime = timeNow;
                    nSpO2AdjustCount = 0;
                }
                nSpO2AdjustCount++;
            }

            //Log.i("bcgWaveViewEcg", "Ecg buf len " + nECGBufferLen);
            if (nECGBufferLen > Constant.TagetSampled4Draw * 2.5 && nEcgNormalStatus == 1) {
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    nReadIndexECG += 1;
                    nReadIndexECG %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(-1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            } else if (nECGBufferLen <= Constant.TagetSampled4Draw * 1 && nEcgNormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - ECGAdjustTime > AdjustOverTime) || (nECGAdjustCount > Constant.TagetSampled4Draw / 10)) {
                    int nReadIndexECG = gTransferData.m_atomECGRIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][(nReadIndexECG) % CSingleInstance.ViewArrayLen]);
                    nReadIndexECG = (nReadIndexECG + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG]);
                    gTransferData.m_dViewData[gTransferData.ecgViewDataItem][nReadIndexECG] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_atomECGRIndex.getAndSet(nReadIndexECG);
                    gTransferData.m_atomECGLen.getAndAdd(1);
                    ECGAdjustTime = timeNow;
                    nECGAdjustCount = 0;
                }
                nECGAdjustCount++;
            }

            if (nRespBufferLen > Constant.TagetSampled4Draw / 2 * 2.5 && nRespNormalStatus == 1) {
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 5)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    nReadIndexResp += 1;
                    nReadIndexResp %= CSingleInstance.ViewArrayLen;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(-1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            } else if (nRespBufferLen <= Constant.TagetSampled4Draw / 2 * 1 && nRespNormalStatus == 1) {
                //发现buffer在降低，适当填充点满足协调统一
                if ((timeNow - RespAdjustTime > AdjustOverTime) || (nRespAdjustCount > Constant.TagetSampled4Draw / 20)) {
                    int nReadIndexResp = gTransferData.m_atomRespRIndex.getAndAdd(0);
                    int nNowValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][(nReadIndexResp) % CSingleInstance.ViewArrayLen]);
                    nReadIndexResp = (nReadIndexResp + CSingleInstance.ViewArrayLen - 1) % CSingleInstance.ViewArrayLen;
                    //修改该点的值为斜率计算结果
                    int nLastValY = (int) (gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp]);
                    gTransferData.m_dViewData[gTransferData.SampledDataResp][nReadIndexResp] = (nNowValY + nLastValY) / 2;
                    gTransferData.m_atomRespRIndex.getAndSet(nReadIndexResp);
                    gTransferData.m_atomRespLen.getAndAdd(1);
                    RespAdjustTime = timeNow;
                    nRespAdjustCount = 0;
                }
                nRespAdjustCount++;
            }
        }
    }

    private void saveData4Test(byte[] data) {
        //存储数据
        if (outputStream == null || bufferedOutputStream == null) {
            dataSavePath = getDateSavePath();
            try {
                outputStream = new FileOutputStream(dataSavePath);
                bufferedOutputStream = new BufferedOutputStream(outputStream);
                //bufferedOutputStream.write(data);
            } catch (Exception e) {
                e.printStackTrace();
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


    private void saveData(byte[] data) {
        //存储数据
        dataSaveCount++;
        if (dataSaveCount == 1 || outputStream == null || bufferedOutputStream == null) {
            dataSavePath = getDateSavePath();
            try {
                outputStream = new FileOutputStream(dataSavePath);
                bufferedOutputStream = new BufferedOutputStream(outputStream);
                bufferedOutputStream.write(data);
            } catch (Exception e) {
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
        if (dataSaveCount <= SaveCountMax) {
            try {
                bufferedOutputStream.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (dataSaveCount == SaveCountMax) {
                    dataSaveCount = 0;
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
    }

    /*
    @Override
    protected void updateReceivedData(byte[] data) {
        String str = String.format("dLen %d", data.length);
        textTitle.setText(str);
        double sampledData[][] = new double[12][256];
        double sampleDataLen[] = new double[12];
        boolean isGetSampledData = false;

        try {
            Algorithm4Library.addRecvData(data, data.length);
            Log.e("ecg","===========传入数据长度===========" + data.length);
            Log.e("ecg","===========获得命令结果===========" + data.length);
            byte[] trabsfer = new byte[256];
            boolean bool =  Algorithm4Library.GetCmdResult(0, trabsfer);
            Log.e("ecg", "==============MainActivr返回指令===========" + Integer.toHexString(trabsfer[0]) + " index1 : " + Integer.toHexString(trabsfer[1] & 0xFF));

            Algorithm4Library.getSampledData(sampledData, sampleDataLen);
            //Algorithm4Library.getSampledData(sampledLen);
            isGetSampledData = true;
        } catch (Exception e) {
            textTitle.setText("Alg error0 " + e.toString());
        }

        if (isGetSampledData) {
            double data1[] = new double[10];
            try {
                Algorithm4Library.getValue(data1);
            } catch (Exception e) {
                textTitle.setText("Alg error1" + e.toString());
            }

            mTextViewRate.setText(String.format("%.0f", data1[0]));
            if (data1[0] <= StringUtils.string2Int(EcgSharedPrefrence.getSpo2Upper(MainActivity.this)) &&
                    data1[0] >= StringUtils.string2Int(EcgSharedPrefrence.getSpo2Floor(MainActivity.this))) {
                mTextViewRate.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            } else {
                mTextViewRate.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
            }
            mTextViewBPM.setText(String.format("%.1f", data1[1]));
            mTextViewPI.setText(String.format("%.1f", data1[2]));

            mTextViewHR.setText(String.format("%.1f", data1[3]));
            if (data1[3] <= StringUtils.string2Int(EcgSharedPrefrence.getEcgUpper(MainActivity.this)) &&
                    data1[3] >= StringUtils.string2Int(EcgSharedPrefrence.getEcgFloor(MainActivity.this))) {
                mTextViewHR.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green));
            } else {
                mTextViewHR.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
            }

            mTextViewRR.setText(String.format("%.1f", data1[4] < 0 ? 0 : data1[4]));
            if (data1[4] <= StringUtils.string2Int(EcgSharedPrefrence.getRespUpper(MainActivity.this))
                    && data1[4] >= StringUtils.string2Int(EcgSharedPrefrence.getRespFloor(MainActivity.this))) {
                mTextViewRR.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.resp_normal_color));
            } else {
                mTextViewRR.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
            }

            mTemperature.setText(String.format("%.1f", data1[5] < 0 ? 0 : data1[5]));
            if (data1[5] <= StringUtils.string2Int(EcgSharedPrefrence.getTempUpper(MainActivity.this))
                    && data1[5] >= StringUtils.string2Int(EcgSharedPrefrence.getTempFloor(MainActivity.this))) {
                mTemperature.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.resp_normal_color));
            } else {
                mTemperature.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
            }

            mTextViewmmHg.setText((data1[7] < 0 ? 0 : data1[7]) + "/" + (data1[6] < 0 ? 0 : data1[6]));
            String s = mTextViewmmHg.getText().toString().trim();
            if (!TextUtils.isEmpty(s)) {
                String sbpUpper = EcgSharedPrefrence.getSbpUpper(MainActivity.this);
                String sbpFloor = EcgSharedPrefrence.getSbpFloor(MainActivity.this);
                String dbpUpper = EcgSharedPrefrence.getDbpUpper(MainActivity.this);
                String dbpFloor = EcgSharedPrefrence.getDbpFloor(MainActivity.this);
                ForegroundColorSpan errorColor = new ForegroundColorSpan(Color.RED);
                ForegroundColorSpan normalColor = new ForegroundColorSpan(Color.WHITE);
                int index = s.indexOf("/");
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(s);
                if ((data1[7] > StringUtils.string2Int(sbpUpper) || data1[7] < StringUtils.string2Int(sbpFloor))
                        && (data1[6] < StringUtils.string2Int(dbpUpper) && data1[6] > StringUtils.string2Int(dbpFloor))) {
                    builder.setSpan(errorColor, 0, index, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    builder.setSpan(normalColor, index, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if ((data1[7] < StringUtils.string2Int(sbpUpper) && data1[7] > StringUtils.string2Int(sbpFloor))
                        && (data1[6] > StringUtils.string2Int(dbpUpper) || data1[6] < StringUtils.string2Int(dbpFloor))) {
                    builder.setSpan(normalColor, 0, index, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    builder.setSpan(errorColor, index, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if ((data1[7] < StringUtils.string2Int(sbpUpper) && data1[7] > StringUtils.string2Int(sbpFloor))
                        && (data1[6] < StringUtils.string2Int(dbpUpper) && data1[6] > StringUtils.string2Int(dbpFloor))) {
                    builder.setSpan(normalColor, 0, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else {
                    builder.setSpan(errorColor, 0, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
                mTextViewmmHg.setText(builder);
            }

        }

        dataCount++;

        if (isGetSampledData) {
            //血氧
            for (int i = 1; i <= sampledData[1][0]; i++) {
                bcgWaveView1.drawWave((int) sampledData[1][i]);
            }
            //心电
            for (int i = 1; i <= sampledData[3][0]; i++) {
                bcgWaveView2.drawWave((int) sampledData[3][i]);
            }
            //呼吸
            for (int i = 1; i <= sampledData[9][0]; i++) {
                bcgWaveViewResp.drawWave((int) sampledData[9][i]);
            }
        }

        saveData(data);
    }
    */

    private String getDateSavePath() {
        //return getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis();
        return getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis() + "_liuyue.txt";
    }

    @Override
    protected void NoDeviceDetached() {
        bUSB_Sta = false;
        mTextViewEcgCtl.setText("ECG OFF");
        mTextViewSpO2Ctl.setText("SpO2 OFF");
        mTextViewTempCtl.setText("Temp OFF");
        if (Constant.NIBP_FUNC_ENABLE) {
            mTextViewNibpCtl.setText("NIBP OFF");
        }
        mTitleTextView.setText("No serial device");
        clearDrawBuff();
        Log.e("NoDeviceDetached", "NoDeviceDetached is running ... ...");
    }

    @Override
    protected int getcontentLayoutId() {
        return R.layout.activity_main2;
    }

    private int lastSpo2Con = 0;
    private int lastECGCon = 0;
    private int lastTempCon = 0;
    private int lastNIBPCon = 0;

    private void DealDevConSta() {
        byte[] transfer = new byte[256];
        //Algorithm4Library.GetCmdResult(Constant.TYPE_DETECTION, transfer);
        Algorithm4SensorLib.GetCmdResult(Constant.TYPE_DETECTION, transfer);

        String print = "";
        if (transfer.length >= 0) {
            for (int i = 0; i < transfer[0] + 1; i++) {
                String strTmp = Integer.toHexString(transfer[i] & 0xFF).toUpperCase();
                print += strTmp + " ";
            }
        }
        if (transfer[0] > 0) {
            //spo2View.setText(print);
            Log.e("DeamonProcess", "MainProject : " + print);
        }
        if (transfer[0] == 9 * 4) {
            //血氧
            if (transfer[1 + 9 * 1] == 0) {
                //连接失败
                mTextViewSpO2Ctl.setText("SpO2 OFF");
                lastSpo2Con = 0;
                clearSpO2DrawBuff();
            } else if (transfer[1 + 9 * 1] == 1) {
                //连接成功
                if (lastSpo2Con == 0) {
                    funAudio(true);
                }
                lastSpo2Con = 1;
                mTextViewSpO2Ctl.setText("SpO2 ON");
            }

            //心电
            if (transfer[1 + 9 * 2] == 0) {
                //连接失败
                lastECGCon = 0;
                mTextViewEcgCtl.setText("ECG OFF");
                clearEcgDrawBuff();
                clearRespDrawBuff();
            } else if (transfer[1 + 9 * 2] == 1) {
                //连接成功
                if (lastECGCon == 0) {
                    funAudio(true);
                }
                lastECGCon = 1;
                mTextViewEcgCtl.setText("ECG ON");
                if (((transfer[1 + 9 * 2 + 7] & 0xFF) | ((transfer[1 + 9 * 2 + 8] & 0xFF) << 8)) == 0x180F) {
                    bcgWaveViewECG.setSampleRe(21000);
                    Log.e("DeamonProcess", "MainProject " + 20000);
                } else if (((transfer[1 + 9 * 2 + 7] & 0xFF) | ((transfer[1 + 9 * 2 + 8] & 0xFF) << 8)) == 0x180E) {
                    bcgWaveViewECG.setSampleRe(8000);
                    Log.e("DeamonProcess", "MainProject " + 8000);
                }
            }

            //温度
            if (transfer[1 + 9 * 0] == 0) {
                //连接失败
                lastTempCon = 0;
                mTextViewTempCtl.setText("Temp OFF");
            } else if (transfer[1 + 9 * 0] == 1) {
                //连接成功
                if (lastTempCon == 0) {
                    funAudio(true);
                }
                lastTempCon = 1;
                mTextViewTempCtl.setText("Temp ON");
            }

            //血压
            if (Constant.NIBP_FUNC_ENABLE) {
                if (transfer[1 + 9 * 3] == 0) {
                    //连接失败
                    lastNIBPCon = 0;
                    mTextViewNibpCtl.setText("NIBP OFF");
                } else if (transfer[1 + 9 * 3] == 1) {
                    //连接成功
                    if (lastNIBPCon == 0) {
                        funAudio(true);
                    }
                    lastNIBPCon = 1;
                    mTextViewNibpCtl.setText("NIBP ON");
                }
            }
        }
    }

    private void DealDevConStaExt() {
        byte[] transfer = new byte[256];
        //Algorithm4Library.GetCmdResult(Constant.TYPE_NET_HEART, transfer);
        Algorithm4SensorLib.GetCmdResult(Constant.TYPE_NET_HEART, transfer);

        int nDevMsgBaseLen = 10;
        boolean bEcg = false, bSpO2 = false, bTemp = false, bNIBP = false, bHPMS = false;
        int nUseUUID4ECG = CSingleInstance.UUID_ECG_3001E_180F;

        for (int i = 0; i < transfer[0] / nDevMsgBaseLen; i++) {
            byte[] MsgBase = new byte[nDevMsgBaseLen];
            System.arraycopy(transfer, 1 + 1 + i * 10, MsgBase, 0, nDevMsgBaseLen);

            //获得设备类型
            int nDevType = MsgBase[0];

            //获得信号强度
            int nSigStrength = MsgBase[1];

            //获得UUID
            int nUUID = MsgBase[2] & 0xFF | (MsgBase[3] & 0xFF) << 8;

            //获得Mac地址
            byte[] DevMac = new byte[6];
            System.arraycopy(MsgBase, 4, DevMac, 0, 6);

            switch (nUUID) {
                case CSingleInstance.UUID_TEMP_FFF0:
                case CSingleInstance.UUID_TEMP_FFF1:
                    if (nSigStrength != 0xFF && nSigStrength > 0) {
                        bTemp = true;
                    }
                    break;
                case CSingleInstance.UUID_ECG_3001E_180F:
                case CSingleInstance.UUID_ECG_3001C_180E:
                    if (nSigStrength != 0xFF && nSigStrength > 0) {
                        bEcg = true;
                        nUseUUID4ECG = nUUID;
                    }
                    break;
                case CSingleInstance.UUID_SpO2S_1910:
                case CSingleInstance.UUID_SpO2L_1911:
                    if (nSigStrength != 0xFF && nSigStrength > 0) {
                        bSpO2 = true;
                    }
                    break;
                case CSingleInstance.UUID_Medxing_FFB0:
                case CSingleInstance.UUID_TKBP_FFE0:
                    if (nSigStrength != 0xFF && nSigStrength > 0) {
                        bNIBP = true;
                    }
                    break;
                case CSingleInstance.UUID_HPMS_1D10:
                    if (nSigStrength != 0xFF && nSigStrength > 0) {
                        bHPMS = true;
                    }
                    break;
                default:
                    break;
            }

//            System.out.printf("Dev Type %d, Strength %d, UUID %04X, DevMac %02X%02X%02X%02X%02X%02X\n", nDevType, nSigStrength, nUUID, DevMac[0], DevMac[1], DevMac[2], DevMac[3], DevMac[4], DevMac[5]);
        }

        if (bHPMS) {
            bEcg = true;
            bSpO2 = true;
            bTemp = true;
            bNIBP = true;
        }
//        bHPMS = false;
        bHpmsIsConnected = bHPMS;

        //血氧
        if (bSpO2 == false && bHPMS == false) {
            //连接失败
            mTextViewSpO2Ctl.setText("SpO2 OFF");
            lastSpo2Con = 0;
            clearSpO2DrawBuff();
        } else if (bSpO2 == true) {
            //连接成功
            if (lastSpo2Con == 0) {
                funAudio(true);
            }
            lastSpo2Con = 1;
            mTextViewSpO2Ctl.setText("SpO2 ON");
        }

        //心电
        if (bEcg == false && bHPMS == false) {
            //连接失败
            lastECGCon = 0;
            mTextViewEcgCtl.setText("ECG OFF");
            clearEcgDrawBuff();
            clearRespDrawBuff();
        } else if (bEcg == true) {
            //连接成功
            if (lastECGCon == 0) {
                funAudio(true);
            }
            lastECGCon = 1;
            mTextViewEcgCtl.setText("ECG ON");
            if (nUseUUID4ECG == CSingleInstance.UUID_ECG_3001E_180F) {
                bcgWaveViewECG.setSampleRe(21000);
//                    Log.e("DeamonProcess", "MainProject " + 20000);
            } else if (nUseUUID4ECG == CSingleInstance.UUID_ECG_3001C_180E) {
                bcgWaveViewECG.setSampleRe(21000);
//                    Log.e("DeamonProcess", "MainProject " + 8000);
            }
        }

        //温度
        if (bTemp == false && bHPMS == false) {
            //连接失败
            lastTempCon = 0;
            mTextViewTempCtl.setText("Temp OFF");
        } else if (bTemp == true) {
            //连接成功
            if (lastTempCon == 0) {
                funAudio(true);
            }
            lastTempCon = 1;
            mTextViewTempCtl.setText("Temp ON");
        }

        //血压
        if (Constant.NIBP_FUNC_ENABLE) {
            if (bNIBP == false) {
                //连接失败
                lastNIBPCon = 0;
                mTextViewNibpCtl.setText("NIBP OFF");
            } else if (bNIBP == true) {
                //连接成功
                if (lastNIBPCon == 0) {
                    funAudio(true);
                }
                lastNIBPCon = 1;
                mTextViewNibpCtl.setText("NIBP ON");
            }
        }
    }

    @Override
    protected void initWidget() {
        super.initWidget();

        //记录handle到全局变量
        CSingleInstance.getInstance().mainActiveHandler = handler;

        mTitleTextView = (TextView) findViewById(R.id.text_usb);
        mTextViewRate = (TextView) findViewById(R.id.graph_father1_data_text_left);
        mTextViewBPM = (TextView) findViewById(R.id.graph_father1_data_text_right);
        mTextViewHR = (TextView) findViewById(R.id.graph_father2_data_text);
        mTextViewmmHg = (TextView) findViewById(R.id.graph_father3_data_text);
        mTextViewNibpMBP = (TextView) findViewById(R.id.graph_father3_data2_text);
        mTextViewRR = (TextView) findViewById(R.id.graph_father4_data_text_left);
        mTemperature = (TextView) findViewById(R.id.graph_father4_data_text_right);
        mTextViewPI = (TextView) findViewById(R.id.graph_father1_data_text_right1);

        mTextViewSpO2Ctl = (TextView) findViewById(R.id.SpO2_Ctl);
        mTextViewEcgCtl = (TextView) findViewById(R.id.ECG_Ctl);
        mTextViewTempCtl = (TextView) findViewById(R.id.Temp_Ctl);
        if (Constant.NIBP_FUNC_ENABLE) {
            mTextViewNibpCtl = (TextView) findViewById(R.id.NIBP_Ctl);
            mSpinnerNibpCtl = (Spinner) findViewById(R.id.graph_father3_data_ctl);
            mItems = getResources().getStringArray(R.array.NibpCtlSpingarr);
            ArrayAdapter<String> _Adapter=new ArrayAdapter<String>(this,R.layout.item_spinner_layout_nibp, mItems);
            //绑定 Adapter到控件
            mSpinnerNibpCtl.setAdapter(_Adapter);
            mSpinnerNibpCtl.setSelection(1, true);
            mSpinnerNibpCtl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //String[] NIBPCtlItem = getResources().getStringArray(R.array.NibpCtlSpingarr);
                    //Toast.makeText(MainActivity.this, "你点击的是:" + NIBPCtlItem[position], 2000).show();

                    // TODO Auto-generated method stub
//                    try {
//                        //以下三行代码是解决问题所在
//                        Field field = AdapterView.class.getDeclaredField("mOldSelectedPosition");
//                        field.setAccessible(true);	//设置mOldSelectedPosition可访问
//                        field.setInt(mSpinnerNibpCtl, AdapterView.INVALID_POSITION); //设置mOldSelectedPosition的值
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                    String curText = mSpinnerNibpCtl.getSelectedItem().toString();
                    //tring curText = mItems[position];
                    Log.i("liuyue", curText);
                    //记录下血压测量按钮的状态，在Activity启动时恢复状态
                    gTransferData.m_nNibpCtlPosition = position;
                    byte[] nibpSensorMac = gTransferData.m_MyDeamonProcess.getNibpSensorMac();
                    byte[] hpmsSensorMac = gTransferData.m_MyDeamonProcess.getHpmsSensorMac();
                    switch (position) {
                        case 0: {
                            //开始测量
                            //构建指令的预备数据
                            byte[] transfer = new byte[256];
                            //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            if (bHpmsIsConnected) {
                                System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                            } else {
                                System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                            }
                            Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            if (transfer[0] > 0 && transfer[0] < 256) {
                                byte[] Send = new byte[transfer[0]];
                                System.arraycopy(transfer, 1, Send, 0, transfer[0]);
                                Log.i("liuyue", "NIBP start , running !!!!");

                                if (writeIoManage(Send) == true) {
                                    System.out.printf("发送开始测量数据 ... %d bytes\n", transfer[0]);
                                } else {
                                    System.out.printf("发送开始测量数据 ... 失败\n");
                                }
                            } else {
                                //show error
                                System.out.printf("构建指令错误 ... 失败\n");
                            }
                        }
                        break;
                        case 1: {
                            //停止测量
                            //构建指令的预备数据
                            if (mNibpCtlStopJump != 0) {
                                mNibpCtlStopJump = 0;
                                Log.i("liuyue", "Internal Stop , So Jump !!!!");
                            } else {
                                Log.i("liuyue", "NIBP Stop , running !!!!");

                                byte[] transfer = new byte[256];
//                                Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Stop, transfer);
                                System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                                if (bHpmsIsConnected) {
                                    System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                                } else {
                                    System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                                }
                                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Stop, transfer);
                                if (transfer[0] > 0 && transfer[0] < 256) {
                                    byte[] Send = new byte[transfer[0]];
                                    System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                                    if (writeIoManage(Send) == true) {
                                        System.out.printf("发送停止测量数据 ... %d bytes\n", transfer[0]);
                                    } else {
                                        System.out.printf("发送停止测量数据 ... 失败\n");
                                    }
                                } else {
                                    //show error
                                    System.out.printf("构建指令错误 ... 失败\n");
                                }
                            }
                        }
                        break;
                        case 2: {
                            //5Min
                            lLastTime = System.currentTimeMillis();
                            //构建指令的预备数据
                            byte[] transfer = new byte[256];
                            //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                            if (bHpmsIsConnected) {
                                System.arraycopy(hpmsSensorMac, 0, transfer, 0, 6);
                            } else {
                                System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                            }
                            Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            if (transfer[0] > 0 && transfer[0] < 256) {
                                byte[] Send = new byte[transfer[0]];
                                System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                                if (writeIoManage(Send) == true) {
                                    System.out.printf("发送开始测量数据 5 ... %d bytes\n", transfer[0]);
                                } else {
                                    System.out.printf("发送开始测量数据 5Min... 失败\n");
                                }
                            } else {
                                //show error
                                System.out.printf("构建指令错误 ... 失败\n");
                            }
                        }
                        break;
                        case 3: {
                            //10Min
                            lLastTime = System.currentTimeMillis();
                            //构建指令的预备数据
                            byte[] transfer = new byte[256];
                            //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                            if (bHpmsIsConnected) {
                                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_HPMS_NIBP_Start, transfer);
                            } else {
                                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            }
                            if (transfer[0] > 0 && transfer[0] < 256) {
                                byte[] Send = new byte[transfer[0]];
                                System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                                if (writeIoManage(Send) == true) {
                                    System.out.printf("发送开始测量数据 10Min... %d bytes\n", transfer[0]);
                                } else {
                                    System.out.printf("发送开始测量数据 10Min... 失败\n");
                                }
                            } else {
                                //show error
                                System.out.printf("构建指令错误 ... 失败\n");
                            }
                        }
                        break;
                        case 4: {
                            //15Min
                            lLastTime = System.currentTimeMillis();
                            //构建指令的预备数据
                            byte[] transfer = new byte[256];
                            //Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            System.arraycopy(nibpSensorMac, 0, transfer, 0, 6);
                            if (bHpmsIsConnected) {
                                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_HPMS_NIBP_Start, transfer);
                            } else {
                                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_NIBP_Start, transfer);
                            }
                            if (transfer[0] > 0 && transfer[0] < 256) {
                                byte[] Send = new byte[transfer[0]];
                                System.arraycopy(transfer, 1, Send, 0, transfer[0]);

                                if (writeIoManage(Send) == true) {
                                    System.out.printf("发送开始测量数据 15Min... %d bytes\n", transfer[0]);
                                } else {
                                    System.out.printf("发送开始测量数据 15Min... 失败\n");
                                }
                            } else {
                                //show error
                                System.out.printf("构建指令错误 ... 失败\n");
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }

        mEcgChannalList = getResources().getStringArray(R.array.EcgChanneSpingarr);
        mSpinnerEcgChannel = findViewById(R.id.graph_father2_Ecg_Channel_Spinner);
        ArrayAdapter<String> ecgChannelAdapter=new ArrayAdapter<String>(this,R.layout.item_spinner_layout, mEcgChannalList);
        //绑定 Adapter到控件
        mSpinnerEcgChannel.setAdapter(ecgChannelAdapter);
        mSpinnerEcgChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:{
                        gTransferData.ecgViewDataItem = gTransferData.SampledDataECGI;
                    } break;
                    case 1: {
                        gTransferData.ecgViewDataItem = gTransferData.SampledDataECGII;
                    } break;
                    case 2: {
                        gTransferData.ecgViewDataItem = gTransferData.SampledDataECGV;
                    } break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGI) {
            mSpinnerEcgChannel.setSelection(0, true);
        } else if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGII) {
            mSpinnerEcgChannel.setSelection(1, true);
        } else if (gTransferData.ecgViewDataItem == gTransferData.SampledDataECGV) {
            mSpinnerEcgChannel.setSelection(2, true);
        }

        spo2View = (TextView) findViewById(R.id.graph_father1_left);
        mTextVersionShow = (TextView) findViewById(R.id.tv_version_name);

        mUsbLog = (TextView) findViewById(R.id.text_log);
        linearLayout = (LinearLayout) findViewById(R.id.root);
        view = getWindow().getDecorView();
        linearLayout.setOnTouchListener(this);

        //ljfth:
        initView();

        //获得屏幕信息
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float xDpi = displayMetrics.xdpi;
        int widthPixels = displayMetrics.widthPixels;
        //公式含义为：1毫米对应的像素数 = 水平像素宽度/((水平像素宽度/水平像素密度) * 1英寸对应的毫米数)
        int pixelPerMM = (int) (widthPixels / ((widthPixels / xDpi) * 25.4) + 0.5);

        if (pixelPerMM * 25 < Constant.TagetSampled4Draw) {
            Constant.TagetSampled4Draw = pixelPerMM * 25;
        }

        //设置采样率方法调整至addData方法后
//        int[] SetTargetSampled = new int[7];
//        for (int i = 0; i < SetTargetSampled.length; i++) {
//            if (i != 2) {
//                SetTargetSampled[i] = Constant.TagetSampled4Draw;
//            } else {
//                SetTargetSampled[i] = Constant.TagetSampled4Draw / 2;
//            }
//        }
//        Algorithm4Library.SetTargetSampling(SetTargetSampled);
//        Log.e("liuyue", ""+SetTargetSampled);

        //init Timer
        //new Timer().schedule(m_TimerLoop, 100, (int)(1000.0f / ((25.0 * pixelPerMM) / 2)));
        DrawTimer = new Timer();
        int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (1.0));
        DrawTimer.schedule(m_TimerLoop, 100, loopTimer);


        //设置timer定时查询设备信息
        new Timer().schedule(new TimerTask() {
            int LoopState = 0;  //0:Search 1:Deal

            //定时任务查询设备连接状态
            @Override
            public void run() {
                byte[] transfer = new byte[256];
//                Log.e("SerialCheck", "running .. ...");
                if (LoopState == 0) {
                    //发送查询命令
//                    Log.e("SerialCheck", "Send .. ...");
                    LoopState = 1;
//                    Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_DETECTION, transfer);
                    Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPE_DETECTION, transfer);
                    if (transfer[0] > 0) {
                        byte[] send = new byte[transfer[0]];
                        System.arraycopy(transfer, 1, send, 0, transfer[0]);
                        try {
                            writeIoManage(send);
//                            Log.e("SerialCheck", "Sending len  .. ... " + transfer[0]);
                        } catch (Exception e) {
//                            Log.e("SerialCheck", "Sending len  .. ... " + transfer[0] + " " + e.toString());
                            throw e;
                        }
//                        Log.e("SerialCheck", "Sending len  .. ... " + transfer[0]);
                    }
                } else if (LoopState == 1) {
                    //处理命令返回结果
//                    Log.e("SerialCheck", "Deal .. ...");
                    Message msg = new Message();
                    msg.what = SetDevSta;
                    handler.sendMessage(msg);
                    LoopState = 0;
                }

            }
        }, 500, 500);
    }


    @Override
    protected void initData() {
        super.initData();
//        mServiceIntent = new Intent(MainActivity.this, UsbService.class);
//        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initView() {
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        for (int i = 0; i < 4; i++) {
                            if (i == 2) {
                                //去掉连续血压波形
                                continue;
                            }
                            int sampleRe = 20000;
                            double change_nV = 1D;
                            double baseline_voltage = 1D;
                            int waveColor = Color.RED;
                            int id1 = 0;
                            int id2 = 0;
                            TextView leftTitle = mTitleTextView;
                            switch (i) {
                                //血氧
                                case 0:
                                    graph_father = (RelativeLayout) view.findViewById(R.id.graph_father1);
                                    leftTitle = (TextView) findViewById(R.id.graph_father1_left);
                                    sampleRe = 4500000;
                                    change_nV = 0.125;
                                    baseline_voltage = 0.585;
                                    waveColor = Color.argb(255, 43, 172, 246);
                                    id1 = R.id.graph_father1_data;
                                    id2 = R.id.graph_father1_left;
                                    break;
                                //心电
                                case 1:
                                    graph_father = (RelativeLayout) view.findViewById(R.id.graph_father2);
                                    leftTitle = (TextView) findViewById(R.id.graph_father2_left);
                                    sampleRe = 21000;
                                    change_nV = 1;
                                    baseline_voltage = -2.2;
                                    waveColor = Color.GREEN;
                                    id1 = R.id.graph_father2_data;
                                    id2 = R.id.graph_father2_left;
                                    break;
                                //血压
                                case 2:
                                    graph_father = (RelativeLayout) view.findViewById(R.id.graph_father3);
                                    leftTitle = (TextView) findViewById(R.id.graph_father3_left);
                                    sampleRe = 4000000;
                                    waveColor = Color.WHITE;
                                    id1 = R.id.graph_father3_data;
                                    id2 = R.id.graph_father3_left;
                                    break;
                                //呼吸
                                case 3:
                                    graph_father = (RelativeLayout) view.findViewById(R.id.graph_father4);
                                    leftTitle = (TextView) findViewById(R.id.graph_father4_left);
                                    sampleRe = 4000000;
                                    change_nV = 4;
                                    baseline_voltage = -24.64;
                                    waveColor = Color.argb(255, 255, 228, 71);
                                    id1 = R.id.graph_father4_data;
                                    id2 = R.id.graph_father4_left;
                                    break;
                            }
                            int width = graph_father.getWidth() - view.findViewById(id1).getWidth();
                            int height = graph_father.getHeight();
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, height);
                            layoutParams.addRule(RelativeLayout.LEFT_OF, id1);
                            EcgWaveView ecgWaveView = new EcgWaveView(getBaseContext(), width, height);
                            ecgWaveView.getDensity();
                            ecgWaveView.setF_step(ecgWaveView.getPixelPerMM() * 25.0f / Constant.TagetSampled4Draw);
                            ecgWaveView.setN_step(1);

                            ecgWaveView.setTag(leftTitle.getText().toString());
                            ecgWaveView.setN_frequency(100);
                            ecgWaveView.setSampleRe(sampleRe);
                            ecgWaveView.initGridVoltage(change_nV);
                            if (i == 1) {
                                //心电
                                baseline_voltage = height * (2 / 3.0);
                                ecgWaveView.initBaseLineVoltage(baseline_voltage);
                                ecgWaveView.setSampleV(1);
                                ecgWaveView.setWaveAdapter(false);
                                ecgWaveView.setEcgy((int) baseline_voltage + ecgWaveView.getEmptyMargin());

                                Rect tmpRecg = new Rect();
                                view.findViewById(id1).getDrawingRect(tmpRecg);
                                Log.e("DrawSize", "ECG  Width : " + width + ",Heigh : " + height + ",Rect is " + tmpRecg.toString());
                            } else if (i == 0) {
                                //血氧
                                baseline_voltage = 0;
                                ecgWaveView.setEmptyMargin(height / 6);
                                ecgWaveView.setY_max(ecgWaveView.getY_max());
                                ecgWaveView.initBaseLineVoltage(baseline_voltage);
                                ecgWaveView.setWaveAdapter(true);
                                ecgWaveView.setP_V_Adapter(true);
                                ecgWaveView.setEcgy(ecgWaveView.GetDefaultOut(ecgWaveView.getY_max(), false));
                                Rect tmpRecg = new Rect();
                                view.findViewById(id1).getDrawingRect(tmpRecg);
                                Log.e("DrawSize", "SpO2  Width : " + width + ",Heigh : " + height + ",Rect is " + tmpRecg.toString());
                            } else if (i == 2) {
                                //血压
                                baseline_voltage = 0;
                                ecgWaveView.initBaseLineVoltage(baseline_voltage);
                                ecgWaveView.setWaveAdapter(true);
                                ecgWaveView.setP_V_Adapter(true);
                                ecgWaveView.setEcgy(ecgWaveView.GetDefaultOut(ecgWaveView.getY_max(), false));
                            } else if (i == 3) {
                                //呼吸
                                baseline_voltage = 0;
                                ecgWaveView.setEmptyMargin(height / 6);
                                ecgWaveView.setY_max(ecgWaveView.getY_max());
                                ecgWaveView.initBaseLineVoltage(baseline_voltage);
                                ecgWaveView.setWaveAdapter(true);
//                                ecgWaveView.setP_V_Adapter(true);
                                ecgWaveView.setP_V_Adapter(false);
                                ecgWaveView.setY_maxReal(127);
                                ecgWaveView.setY_minReal(-128);
                                ecgWaveView.setEcgy(ecgWaveView.GetDefaultOut(ecgWaveView.getY_max(), false));
                            }

                            ecgWaveView.setWaveColor(waveColor);
                            ecgWaveView.setGridColor(Color.argb(255, 26, 40, 59));
                            ecgWaveView.setGridFullFill(true);
                            ecgWaveView.init(ecgViewListener);

                            graph_father.addView(ecgWaveView, layoutParams);
                            leftTitle.bringToFront();
                            view.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                            switch (i) {
                                case 0:
                                    bcgWaveViewSpO2 = ecgWaveView;
                                    break;
                                case 1:
                                    bcgWaveViewECG = ecgWaveView;
                                    break;
                                case 2:
                                    bcgWaveViewCNIBP = ecgWaveView;
                                    break;
                                case 3:
                                    bcgWaveViewResp = ecgWaveView;
                                    break;
                            }
                        }

                    }
                });
    }

    private EcgViewInterface ecgViewListener = new EcgViewInterface() {
        @Override
        public void onError(EcgWaveView view, Exception e) {

        }

        @Override
        public void onShowMessage(EcgWaveView view, String t, int i) {
            Log.i("tag", "心电接口回调--》" + t);
            if (i == 0) {
                //  Toast.makeText(getApplication(),"时间：" + t + "ms/格",Toast.LENGTH_SHORT).show();
            } else if (i == 1) {
                mUsbLog.setText(view.getTag() + " : change_nV = " + t);
                //   Toast.makeText(getApplication(),"电压："+t+"mv/格",Toast.LENGTH_SHORT).show();

            } else if (i == 2) {
                mUsbLog.setText(view.getTag() + " : baseline_voltage = " + t);
            }
        }
    };

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i("tag", "linear监听");
        return false;
    }

    private void funAudio(boolean Con) {
        if (Con == true) {
            AssetManager am = getAssets();
            AssetFileDescriptor asFd = null;
            FileDescriptor aFd = null;
            try {
                asFd = am.openFd("taobao.mp3");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (asFd != null) {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                    }
                    mediaPlayer.setDataSource(asFd.getFileDescriptor(), asFd.getStartOffset(), asFd.getLength()); // 指定音频文件的路径
                    mediaPlayer.prepare(); // 让MediaPlayer进入到准备状态
                    mediaPlayer.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //DisConnect
            AssetManager am = getAssets();
            AssetFileDescriptor asFd = null;
            FileDescriptor aFd = null;
            try {
                asFd = am.openFd("taobao.mp3");
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (asFd != null) {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                    }
                    mediaPlayer.setDataSource(asFd.getFileDescriptor(), asFd.getStartOffset(), asFd.getLength()); // 指定音频文件的路径
                    mediaPlayer.setVolume(100, 100);
                    mediaPlayer.prepare(); // 让MediaPlayer进入到准备状态
                    mediaPlayer.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        boolean bRet = PermissionUtils.isGrantExternalRW(this,1);
        Log.i("bodystm", "Permission is " + bRet);

        // 监测一次应用是否更新
        Beta.checkAppUpgrade();
        // crash test
//        CrashReport.testJavaCrash();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //检验是否获取权限，如果获取权限，外部存储会处于开放状态，会弹出一个toast提示获得授权
                    String sdCard = Environment.getExternalStorageState();
                    if (sdCard.equals(Environment.MEDIA_MOUNTED)){
                        Toast.makeText(this,"获得授权",Toast.LENGTH_LONG).show();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "buxing", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        startIoManager();

        if (gTransferData.mPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            UsbDeviceConnection connection = gTransferData.mUsbManager.openDevice(gTransferData.mPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                gTransferData.mPort.open(connection);
                gTransferData.mPort.setParameters(230400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Error setting up device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    gTransferData.mPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                gTransferData.mPort = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + gTransferData.mPort.getClass().getSimpleName());

            // 血氧
            writeIoManage(GeneralSpO2Command(true));
            // 心电、呼吸
            writeIoManage(GeneralECGCommand(true));
            // 血压
            writeIoManage(GeneralNIBPCommand(true));
        }
        onDeviceStateChange();
*/
//        DrawTimer = new Timer();
//        int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (1.0));
//        DrawTimer.schedule(m_TimerLoop, 100, loopTimer);
        Log.e("test", "onResume" + this.toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDrawerLayout.closeDrawer(Gravity.START);
        /*
        stopIoManager();
        if (gTransferData.mPort != null) {
            try {
                gTransferData.mPort.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
        */
        //DrawTimer.cancel();
        Log.e("test", "onPause" + this.toString());
    }


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //动态获取内存存储权限
    public static void verifyStoragePermissions(MainActivity context) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

//        verifyStoragePermissions(this);

        //清除数据解析线程的所有数据
        if (gTransferData.m_MyDeamonProcess != null) {
            gTransferData.m_MyDeamonProcess.clearAlgInternalValue();
        }

        //恢复上次的USB状态
        bUSB_Sta = CSingleInstance.getInstance().m_bUSB_Sta;

        if (bUSB_Sta == false) {
            //当发现当前USB设备未加载时手动启动一次USB设备扫描加载
            initPort();
            //配置串口参数
            PortConfigure();
            //配置串口监听
            onDeviceStateChange();
        }

        //恢复血压测量的上次状态
        mSpinnerNibpCtl.setSelection(gTransferData.m_nNibpCtlPosition);
        Log.e("test", "onStart" + this.toString());

        //清除绘图缓冲信息
        clearDrawBuff();

        m_TimerLoop.run();
        Log.e("test", "m_TimerLoop Run(), " + m_TimerLoop.toString());

//        int loopTimer = (int) (1000 / Constant.TagetSampled4Draw * (1.0));
//        DrawTimer.schedule(m_TimerLoop, 100, loopTimer);


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //onStop();
        Log.e("test", "onRestart" + this.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        //记录当前的USB状态
        CSingleInstance.getInstance().m_bUSB_Sta = bUSB_Sta;

        //关闭定时任务线程
//        DrawTimer.cancel();
//        m_TimerLoop.cancel();

        Log.e("test", "onStop, " + this.toString());
        Log.e("test", "m_TimerLoop cancel, " + this.toString());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mConnection);
//        stopService(mServiceIntent);
        //记录当前的USB状态
        CSingleInstance.getInstance().m_bUSB_Sta = bUSB_Sta;

        //关闭定时任务线程
        DrawTimer.cancel();
        m_TimerLoop.cancel();

        EventBus.getDefault().unregister(this);
        Log.e("test", "onDestroy");
    }

    /*
    //*************************一些命令*************************

    // 打开或关闭血氧
    private byte[] GeneralSpO2Command(boolean isOpen) {

        byte[] array = new byte[10];

        array[0] = (byte) (0xAA);
        array[1] = (byte) (0xAA);
        array[2] = (byte) (0x00);
        array[3] = (byte) (0x01);
        if (isOpen) {
            array[4] = (byte) (0x42);
        } else {
            array[4] = (byte) (0x43);
        }
        array[5] = (byte) (0x00);
        array[6] = (byte) (0x00);
        array[7] = (byte) (0x00);
        array[8] = (byte) (0x55);
        array[9] = (byte) (0x55);

        return array;
    }

    // 打开或关闭心电和呼吸
    private byte[] GeneralECGCommand(boolean isOpen) {

        byte[] array = new byte[10];
        //byte array[] = {0xAA, 0xAA, 0x00, 0x01, 0x42, 0x00, 0x00, 0x00, 0x55, 0x55};

        array[0] = (byte) (0xAA);
        array[1] = (byte) (0xAA);
        array[2] = (byte) (0x00);
        array[3] = (byte) (0x01);
        if (isOpen) {text_title
            array[4] = (byte) (0x42);
        } else {
            array[4] = (byte) (0x43);
        }
        array[5] = (byte) (0x01);
        array[6] = (byte) (0x00);
        array[7] = (byte) (0x00);
        array[8] = (byte) (0x55);
        array[9] = (byte) (0x55);

        return array;
    }

    // 打开或关闭温度
    private byte[] GeneralTempCommand(boolean isOpen) {

        byte[] array = new byte[10];
        //byte array[] = {0xAA, 0xAA, 0x00, 0x01, 0x42, 0x00, 0x00, 0x00, 0x55, 0x55};

        array[0] = (byte) (0xAA);
        array[1] = (byte) (0xAA);
        array[2] = (byte) (0x00);
        array[3] = (byte) (0x01);
        if (isOpen) {
            array[4] = (byte) (0x42);
        } else {
            array[4] = (byte) (0x43);
        }
        array[5] = (byte) (0x04);
        array[6] = (byte) (0x00);
        array[7] = (byte) (0x00);
        array[8] = (byte) (0x55);
        array[9] = (byte) (0x55);

        return array;
    }

    // 打开或关闭血压
    private byte[] GeneralNIBPCommand(boolean isOpen) {

        byte[] array = new byte[10];
        //byte array[] = {0xAA, 0xAA, 0x00, 0x01, 0x42, 0x00, 0x00, 0x00, 0x55, 0x55};

        array[0] = (byte) (0xAA);
        array[1] = (byte) (0xAA);
        array[2] = (byte) (0x00);
        array[3] = (byte) (0x01);
        if (isOpen) {
            array[4] = (byte) (0x42);
        } else {
            array[4] = (byte) (0x43);
        }
        array[5] = (byte) (0x05);
        array[6] = (byte) (0x00);
        array[7] = (byte) (0x00);
        array[8] = (byte) (0x55);
        array[9] = (byte) (0x55);

        return array;
    }

    //*************************一些命令*************************
*/

    //断开所有设备连接

    private void OffLineAllDevices() {
        //Off Line SpO2
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                final byte SpO2Type = 7;

                byte[] transfer = new byte[256];
                transfer[0] = 1;

                //Off Line SpO2
                transfer[1] = SpO2Type;
                //Algorithm4Library.GeneralCmd4Dev(Constant.TYPW_DISC_ALL_DEV, transfer);
                Algorithm4SensorLib.GeneralCmd4Dev(Constant.TYPW_DISC_ALL_DEV, transfer);
                if (transfer[0] > 0) {
                    byte[] send = new byte[transfer[0]];
                    System.arraycopy(transfer, 1, send, 0, transfer[0]);

                    writeIoManage(send);
                    Log.e("DeamonProcess", "MainProject SpO2 Off");
                }
            }
        }, 100);
        mTextViewSpO2Ctl.setText("SpO2 OFF");

        //Off Line ECG
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                final byte ECGType = 1;
//
//                byte[] transfer = new byte[256];
//                transfer[0] = 1;
//                transfer[1] = ECGType;
//                Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_BREAK, transfer);
//                if (transfer[0] > 0) {
//                    byte[] send = new byte[transfer[0]];
//                    System.arraycopy(transfer, 1, send, 0, transfer[0]);
//                    writeIoManage(send);
//                    Log.e("DeamonProcess", "MainProject ECG Off");
//                }
//            }
//        }, 2000);
        mTextViewEcgCtl.setText("ECG OFF");


        //Off Line Temp
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                final byte TempType = 4;
//
//                byte[] transfer = new byte[256];
//                transfer[0] = 1;transfer[1] = TempType;
//                Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_BREAK, transfer);
//                if (transfer[0] > 0) {
//                    byte[] send = new byte[transfer[0]];
//                    System.arraycopy(transfer, 1, send, 0, transfer[0]);
//                    writeIoManage(send);
//                    Log.e("DeamonProcess", "MainProject Temp Off");
//                }            }
//        }, 3000);
        mTextViewSpO2Ctl.setText("Temp OFF");

        //Off Line NIBP
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                final byte NIBPType = 5;
//
//                byte[] transfer = new byte[256];
//                transfer[0] = 1;
//                transfer[1] = NIBPType;
//                Algorithm4Library.GeneralCmd4Dev(Constant.TYPE_BREAK, transfer);
//                if (transfer[0] > 0) {
//                    byte[] send = new byte[transfer[0]];
//                    System.arraycopy(transfer, 1, send, 0, transfer[0]);
//                    writeIoManage(send);
//                    Log.e("DeamonProcess", "MainProject NIBP Off");
//                }
//            }
//        }, 4000);
        if (Constant.NIBP_FUNC_ENABLE) {
            mTextViewSpO2Ctl.setText("NIBP OFF");
        }

        /*
         断开连接后初始化，供CMyDeamonProcess类判断ECG波形使用
         */
        gTransferData.needAutoSelectEcgChannel = true;
        gTransferData.disconnectTime = System.currentTimeMillis();
    }

    //清空绘图缓冲去
    private void clearEcgDrawBuff() {
        gTransferData.m_atomECGWIndex.getAndSet(0);
        gTransferData.m_atomECGRIndex.getAndSet(0);
        gTransferData.m_atomECGLen.getAndSet(0);
    }

    private void clearRespDrawBuff() {
        gTransferData.m_atomRespWIndex.getAndSet(0);
        gTransferData.m_atomRespRIndex.getAndSet(0);
        gTransferData.m_atomRespLen.getAndSet(0);
    }

    private void clearSpO2DrawBuff() {
        gTransferData.m_atomSpO2WIndex.getAndSet(0);
        gTransferData.m_atomSpO2RIndex.getAndSet(0);
        gTransferData.m_atomSpO2Len.getAndSet(0);
    }

    private void clearDrawBuff() {
        clearEcgDrawBuff();
        clearRespDrawBuff();
        clearSpO2DrawBuff();

        //绘图状态恢复默认
        nDrawFlag4SpO2 = 0;
        nDrawFlag4ECG = 0;
        nDrawFlag4Resp = 0;
        nSpO2NormalStatus = 0;
        nEcgNormalStatus = 0;
        nRespNormalStatus = 0;
    }


    @OnClick({R.id.stv_patient_info, R.id.stv_device_attachment, R.id.stv_param_setting,
            R.id.stv_save, R.id.stv_reset, R.id.stv_about, R.id.btn_test, R.id.btn_ipset})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.stv_patient_info:
                //startActivity(new Intent(MainActivity.this, PatientInfoActivity.class));
                NoDeviceDetached();
                break;
            case R.id.stv_device_attachment:
                startActivity(new Intent(MainActivity.this, DevicesActivity.class));
                break;
            case R.id.stv_param_setting:
                startActivity(new Intent(MainActivity.this, ParamSettingActivity.class));
                break;
            case R.id.stv_save:
                startActivity(new Intent(MainActivity.this, SaveActivity.class));
                break;
            case R.id.stv_reset:
                startActivity(new Intent(MainActivity.this, ResetActivity.class));
                break;
            case R.id.stv_about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
            case R.id.btn_test:
                OffLineAllDevices();
                mDrawerLayout.closeDrawer(Gravity.START);
                funAudio(false);
                clearDrawBuff();
                break;
            case R.id.btn_spo2config:
                //startActivity(new Intent(MainActivity.this, AboutActivity.class));
                startActivity(new Intent(MainActivity.this, CSpO2Config.class));
                break;
            case R.id.btn_ipset:
                startActivity(new Intent(MainActivity.this, IpsetActivity.class));
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(HashMap<String, String> params) {
        if (params != null) {
            String action = params.get("action");
            if (TextUtils.equals(action, PatientInfoActivity.ACTION_SAVE)) {
                String name = EcgSharedPrefrence.getName(MainActivity.this);
                String bedNum = EcgSharedPrefrence.getBedNum(MainActivity.this);
                textTitle.setText(name + "  -  " + bedNum + "床");
                textTitle.setText("铂元智能科技");
                Constant.SAVE_PATH = getExternalCacheDir().getAbsolutePath() + "/" + name;
            } else if (TextUtils.equals(action, PatientInfoActivity.ACTION_CLEAR)) {
                textTitle.setText("");
            } else if (TextUtils.equals(action, ParamSettingActivity.ACTION_PARAM_SETTING)) {
                // 参数设置完成

            }

            String DrawViewBuff = params.get("DrawViewBuff");
            if (DrawViewBuff != null) {
                if (!DrawViewBuff.isEmpty()) {
                    mUsbLog.setText(DrawViewBuff);
                }
            }

            String AdapterSpO2 = params.get("AdapterSpO2");
            if (AdapterSpO2 != null) {
                if (!AdapterSpO2.isEmpty()) {
                    spo2View.setText(AdapterSpO2);
                }
            }


//            String RecvDemonListen = params.get("RecvDemonListen");
//            if (!RecvDemonListen.isEmpty()) {
//                spo2View.setText(RecvDemonListen);
//            }
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMessageEvent() {
//
//    }


    private class UsbConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("warner", "===========onServiceConnected===========");
            mBinder = (UsbService.UsbBinder) service;
            mBinder.writeManage(GeneralSpO2Command(true));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder = null;
        }
    }

}
