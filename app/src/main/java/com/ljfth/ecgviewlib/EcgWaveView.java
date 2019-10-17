package com.ljfth.ecgviewlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ljfth.ecgviewlib.BackUsing.CSingleInstance;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;

public class EcgWaveView extends View {
    private int mode;
    private EcgViewInterface listener;
    private Bitmap machineBitmap;
    private Bitmap cacheBitmap;
    private Bitmap backBitmap;
    private Canvas machineCanvas = null;
    private Paint paint;
    private Paint bmpPaint = new Paint();
    private int width;
    private int height;
    private boolean waveAdapter = true;     //波形自适应

    public int getEmptyMargin() {
        return emptyMargin;
    }

    public void setEmptyMargin(int emptyMargin) {
        this.emptyMargin = emptyMargin;
    }

    private int emptyMargin = 20;           //绘制区域上下各预留一定空间做留白

    public boolean isP_V_Adapter() {
        return P_V_Adapter;
    }

    public void setP_V_Adapter(boolean p_V_Adapter) {
        P_V_Adapter = p_V_Adapter;
    }

    private boolean P_V_Adapter = false;    //峰谷值自适应调整
    private int SampleV;
    private int SampleR;
    private double n_frequency;
    private int bx;
    private int by;
    //基线电压
    private double baseline_voltage = 1D; //baseline_voltage
    private int change_50n;             //基准像素数
    private double change_nV = 1D;      //该值配合变量change_50n使用，change_50n/change_nV表示mV电压值用多少像素点表示

    public int getN_step() {
        return n_step;
    }

    public void setN_step(int n_step) {
        this.n_step = n_step;
    }

    private int n_step = 2;                 //数据步长（相邻两个数据的实际距离）
    private float f_stepRemaind = 0.0f;     //由于步长不一定是整数，所以将余数保留

    public float getF_step() {
        return f_step;
    }

    public void setF_step(float f_step) {
        this.f_step = f_step;
    }

    private float f_step = 2;
    private int n_ecgx;                 //上一次所绘制点的x坐标

    //设置第一个点y值
    public void setEcgy(int ecgy) {
        this.ecgy = ecgy;
    }

    public int ecgy;                   //上一次所绘制点的y坐标
    private int n_Btime;
    private double n_Ptime;
    private double[] t = new double[3];
    private float oldDistance;
    private float newDistance;
    private static int DISTANCE = 100;

    public int getXCoordinate() {
        return n_ecgx;
    }

    public boolean bCheckIs0Coordinate() {
        if (n_ecgx <= 0 || n_ecgx >= this.width) {
            return true;
        } else {
            return false;
        }
    }

    public int getY_max() {
        return y_max;
    }

    public void setY_max(int y_max) {
        if (y_max > emptyMargin * 2) {
            this.y_max = (y_max - emptyMargin * 2);
        } else {
            this.y_max = y_max;
        }
    }

    public int getY_min() {
        return y_min;
    }

    public void setY_min(int y_min) {
        this.y_min = y_min;
    }

    public int y_max = -1;       //要绘制的目标坐标系Y最大值
    public int y_min = -1;       //要绘制的目标坐标系Y最小值
    private int y_maxReal;  //实时数据的峰值
    private int y_minReal;  //实时数据的谷值


    //实时数据的峰谷值是否需要根据根据数据更新
    //  true:不需要依赖其他变量赋值修正，自己统计（一般是第一次绘图或者绘图时用）
    //  false:需要依赖其他变量赋值修正
    private int yRealInitFlag = 0;

    public int isyRealInitFlag() {
        return yRealInitFlag;
    }

    public void setyRealInitFlag(int yRealInitFlag) {
        this.yRealInitFlag = yRealInitFlag;
    }

    //用来记录绘图从x = 0到x=end这一个区间的最大值和最小值，通过该值对实时的峰值进行矫正
    //防止因为出现一些极端值而导致峰谷值的差过大，最终影响波形的绘制（波形被压缩成直线）
    private int y_maxRealLast;
    private int y_minRealLast;

    public int getY_maxRealLast() {
        return y_maxRealLast;
    }

    public void setY_maxRealLast(int y_maxRealLast) {
        this.y_maxRealLast = y_maxRealLast;
    }

    public int getY_minRealLast() {
        return y_minRealLast;
    }

    public void setY_minRealLast(int y_minRealLast) {
        this.y_minRealLast = y_minRealLast;
    }

    public int getY_maxReal() {
        return y_maxReal;
    }

    public void setY_maxReal(int y_maxReal) {
        this.y_maxReal = y_maxReal;
    }

    public int getY_minReal() {
        return y_minReal;
    }

    public void setY_minReal(int y_minReal) {
        this.y_minReal = y_minReal;
    }

    //基线调整参考值
    private int BaseLineRef = 0;
    //缩放系数
    private float scalVal = 1.0f;

    private int change_type;
    private int waveColor = Color.RED;
    private int gridColor = Color.argb(128, 220, 190, 50);
    private boolean b_autoResize = true;
    private boolean gridFullFill = false;
    //屏幕信息
    private float xDpi;   //水平方向像素密度
    private float yDpi;   //垂直方向像素密度
    private int widthPixels;    //屏幕宽度，单位像素
    private int heighPixels;    //屏幕高度，单位像素
    private final float mmPerInch = 25.4f; //1Inch = 2.54cm=25.4mm

    public int getPixelPerMM() {
        return pixelPerMM;
    }

    private int pixelPerMM; //1mm对应像素数
    private int mmPerMV = 10;    //默认情况下1mv对应10毫米
    private int EcgGain = 1;     //ECG增益值默认是1

    public EcgViewInterface getListener() {
        return this.listener;
    }

    public void setListener(EcgViewInterface var1) {
        this.listener = var1;
    }

    public boolean isWaveAdapter() {
        return this.waveAdapter;
    }

    public void setWaveAdapter(boolean var1) {
        this.waveAdapter = var1;
    }

    public void setWaveColor(int var1) {
        this.waveColor = var1;
    }

    public void setGridColor(int var1) {
        this.gridColor = var1;
    }

    public void setGridFullFill(boolean var1) {
        this.gridFullFill = var1;
    }

    public int getSampleRe() {
        return this.SampleR;
    }

    public void setSampleRe(int var1) {
        this.SampleR = var1;
    }

    public int getSampleV() {
        return this.SampleV;
    }

    public void setSampleV(int var1) {
        this.SampleV = var1;
        //初始化基线电压
    }

    //初始化每格电压
    public void initGridVoltage(double var1) {
        this.change_nV = var1;
    }

    //初始化下基线电压
    //20180829, liuyue, 将该变量修改为基线偏移，单位为像素(pixel)
    public void initBaseLineVoltage(double var1) {
        this.baseline_voltage = var1 + emptyMargin;
    }

    public double getN_frequency() {
        return this.n_frequency;
    }

    public void setN_frequency(double var1) {
        this.n_frequency = var1;
    }

    /*
        Desc: 清除所有和初始化数据相关变量信息
     */
    public void initAllVariable() {
        //绘图极值恢复到默认状态
        setY_maxReal(-0x7FFFFFFF);
        setY_minReal(0x7FFFFFFF);
        setY_maxRealLast(-1);    //-0x7FFFFFFF
        setY_minRealLast(-1); //0x7FFFFFFF

        //血氧动态调整相关
        nExtremeRefIndex = 0;
        nExtremeIndexMax = 0;
        nExtremeIndexMin = 0;
        nExtremeTmpYMax = -0x7FFFFFFF;
        nExtremeTmpYMin = 0x7FFFFFFF;
    }
    public EcgWaveView(Context var1, int var2, int var3) {
        super(var1);
        this.width = var2;
        this.height = var3;

        //初始化峰谷值边界
        setY_max(var3);
        setY_min(0);
        setY_maxReal(-0x7FFFFFFF);
        setY_minReal(0x7FFFFFFF);
        setY_maxRealLast(-1);    //-0x7FFFFFFF
        setY_minRealLast(-1); //0x7FFFFFFF
    }

    /*
        Desc: 获得屏幕尺寸信息
     */
    public void getDensity() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        xDpi = displayMetrics.xdpi;
        yDpi = displayMetrics.ydpi;
        widthPixels = displayMetrics.widthPixels;
        heighPixels = displayMetrics.heightPixels;
        //公式含义为：1毫米对应的像素数 = 水平像素宽度/((水平像素宽度/水平像素密度) * 1英寸对应的毫米数)
        pixelPerMM = (int) (widthPixels / ((widthPixels / xDpi) * mmPerInch) + 0.5);
        Log.i("Display", "Density is " + displayMetrics.density + " densityDpi is " + displayMetrics.densityDpi + " height: " + displayMetrics.heightPixels +
                " width: " + displayMetrics.widthPixels);

    }

    public void init(EcgViewInterface var1) {
        listener = var1;
        this.bx = this.width;
        this.by = this.height;// / 50 * 50 + 5;
        this.drawBackGrid();
        this.machineBitmap = Bitmap.createBitmap(this.backBitmap, 0, 0, this.width, this.height);
        this.cacheBitmap = Bitmap.createBitmap(this.backBitmap, 0, 0, this.width, this.height);
        this.machineCanvas = new Canvas();
        this.machineCanvas.setBitmap(this.machineBitmap);
        this.paint = new Paint(4);
        this.paint.setColor(waveColor);
        this.paint.setStyle(Style.STROKE);
        this.paint.setStrokeWidth(4.0F);
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setStrokeJoin(Join.ROUND);
        //this.ecgy = 21;
        this.n_ecgx = 1;
        checkRange();
        this.change_type = 1;
        this.change_50n = 50;
        //this.n_step = 2;
        this.UnitDraw();
        //getDensity();
    }

    public void UnitDraw() {
        this.n_Ptime = 1000.0D / this.n_frequency;
        this.n_Btime = (int) (this.n_Ptime * (double) this.change_50n / (double) this.n_step);
        if (listener != null) {
            this.listener.onShowMessage(this, this.n_Btime + "", 0);
        }
    }

    private void drawBackGrid() {
        //绘制网格
        int m, n;
        backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvasBackground = new Canvas();
        canvasBackground.setBitmap(backBitmap);

        Paint paint1 = new Paint();

        //画背景
        paint1.setColor(gridFullFill ? gridColor : Color.WHITE);
        paint1.setStyle(Paint.Style.FILL);
        paint1.setStrokeWidth(2);
        paint1.setAntiAlias(true);
        paint1.setDither(true);
        paint1.setStrokeJoin(Paint.Join.ROUND);
        canvasBackground.drawRect(0, 0, bx, by, paint1);
        if (!gridFullFill) {
            setBackgroundColor(gridColor);
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setColor(gridColor);
            //画点
            for (m = 0; m < bx; m = m + 10)
                for (n = 0; n < by; n = n + 10)
                    canvasBackground.drawPoint(m, n, paint1);
            //画网格
            for (m = 0; m < bx; m = m + 50)
                canvasBackground.drawLine(m, 0, m, by - 5, paint1);
            for (n = 0; n < by; n = n + 50)
                canvasBackground.drawLine(0, n, bx, n, paint1);
        }

    }


    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(machineBitmap, 0, 0, bmpPaint);
    }

    public void drawWave(int y, boolean defVal) {
        //转换y坐标值
        int ecgy_new = changeOut(y, defVal);

        //更新实际步长
        n_step = (int) (f_step + f_stepRemaind);
        f_stepRemaind = f_step + f_stepRemaind - n_step;

        this.paint.setColor(waveColor);

        //画线
        machineCanvas.drawLine(n_ecgx, ecgy, n_ecgx + n_step, ecgy_new, this.paint);

        //更新x坐标
        n_ecgx += n_step;

        //刷新绘制区域
        ScreenResh();

        //告诉UI线程执行刷新操作
        // invalidate(); //该绘图操作放在了Timer线程中，所以必须将刷新的操作告知UI线程，所以此处去掉了刷新操作

        //更新y坐标
        ecgy = ecgy_new;
    }

    public int GetDefaultOut(int temp, boolean defVal) {
        return changeOut(temp, defVal);
    }

    /*
        Desc: 为血氧动态调整上下限提供一种极值参考方法
        Params：
            nRealY: 实际Y坐标点
        Returns:
            返回调整状态
     */
    private boolean bRefPauseRate = false;  //是否参考脉率值
    private static int nExtremeBufLen = 6;
    private int nExtremeRefLen = Constant.TagetSampled4Draw / 2;   //取一个极值的参考长度，即125个点中取出极大值、极小值
    private int nExtremeRefIndex = 0;
    private int nExtremeIndexMax = 0;
    private int nExtremeIndexMin = 0;
    private int nExtremeValueMin[] = new int[nExtremeBufLen];
    private int nExtremeValueMax[] = new int[nExtremeBufLen];
    private int nExtremeTmpYMax = -0x7FFFFFFF;
    private int nExtremeTmpYMin = 0x7FFFFFFF;

    public int getnRefPauseRageVal() {
        return nRefPauseRageVal;
    }

    public void setnRefPauseRageVal(int nRefPauseRageVal) {
        this.nRefPauseRageVal = nRefPauseRageVal;
    }

    private int nRefPauseRageVal = -1;

    private int extremePointDynamic(int nRealY) {
        int nPauseRateVal = getnRefPauseRageVal();
        if (bRefPauseRate == true) {
            if (nPauseRateVal > 0 && nPauseRateVal < 250) {
                try {
                    nExtremeRefLen = Constant.TagetSampled4Draw * 60 / nPauseRateVal;
                } catch (Exception e) {
                    nExtremeRefLen = Constant.TagetSampled4Draw / 2;
                }
            } else {
                nExtremeRefLen = Constant.TagetSampled4Draw / 2;
            }
        } else {
            nExtremeRefLen = Constant.TagetSampled4Draw / 2;
        }

        nExtremeRefIndex++;
        if (nExtremeRefIndex < nExtremeRefLen) {
            //更新参考极值
            if (nRealY < nExtremeTmpYMin) {
                nExtremeTmpYMin = nRealY;
            }
            if (nRealY > nExtremeTmpYMax) {
                nExtremeTmpYMax = nRealY;
            }
        } else {
            //判断极大值极小值是否小于当前正在使用的参数
            if (nExtremeTmpYMin <= y_minReal * 0.7) {
                //最小值不修正
                nExtremeIndexMin = 0;
                setY_minRealLast(-1);
            } else {
                nExtremeValueMin[nExtremeIndexMin] = nExtremeTmpYMin;
                nExtremeIndexMin++;
                if (nExtremeIndexMin == nExtremeBufLen) {
                    //更新绘图参考极值，会在绘图在原点刷新时生效。
                    int nTmpSumMin = 0;
                    for (int i = 0; i < nExtremeBufLen; i++) {
                        nTmpSumMin += nExtremeValueMin[i];
                    }
                    setY_minRealLast(nTmpSumMin / nExtremeBufLen);
                    setY_minReal(getY_minRealLast());
                    Log.e("AdapterSpO2Min", "RealMin " + getY_minReal());
                    nExtremeIndexMin = 0;
                } else {
                    setY_minRealLast(-1);
                }
            }


            if (nExtremeTmpYMax >= y_maxReal * 0.7) {
                //最大值不更新
                nExtremeIndexMax = 0;
                setY_maxRealLast(-1);
            } else {
                nExtremeValueMax[nExtremeIndexMax] = nExtremeTmpYMax;
                nExtremeIndexMax++;
                if (nExtremeIndexMax == nExtremeBufLen) {
                    //更新绘图参考极值，会在绘图在原点刷新时生效。
                    int nTmpSumMax = 0;
                    for (int i = 0; i < nExtremeBufLen; i++) {
                        nTmpSumMax += nExtremeValueMax[i];
                    }
                    setY_maxRealLast(nTmpSumMax / nExtremeBufLen);
                    setY_maxReal(getY_maxRealLast());
                    Log.e("AdapterSpO2Max", "RealMax " + getY_maxReal());
                    nExtremeIndexMax = 0;
                } else {
                    setY_maxRealLast(-1);
                }
            }

            nExtremeTmpYMax = -0x7FFFFFFF;
            nExtremeTmpYMin = 0x7FFFFFFF;
            nExtremeRefIndex = 0;

        }
        return 0;
    }

    /*
        Desc:将数据采集的数字量对应为绘图坐标系的坐标值
        Params:
            temp: 数据采集数字量
            defVal: 判断是真实数据还是默认填充数据
                    true：真实数据
                    false：默认填充
     */
    private int changeOut(int temp, boolean defVal) {
        if (defVal == true) {
            if (waveAdapter == false) {
                float a;    //将电压数字量转换为对应的电压值
                int b;
                //temp-欲转换的数值
                //a表示放大之前电压的真实范围。SampleV 是输入的电压范围：比如[0,10]mv,则SampleV 就是10，而[0,10]所对应的Y值是[0,4096],则SampleR就是4096
                a = (float) SampleV * temp / SampleR / 1.0f;
                //这个公式的意思是（真实电压-基线电压）* (每格所拥有的50个像素/每格代表的电压)
                //b = (short) (by - (a - baseline_voltage) * (change_50n / change_nV));
                //b = (short) ((a -  baseline_voltage) * (change_50n / change_nV));
                b = (int) (a * (pixelPerMM * mmPerMV * EcgGain) + baseline_voltage);


//                // y_max 和y_min 用于记录像素最大值和最小值，当前的像素点超过了最大值，表明曲线将要超出屏幕，需要进行波形的自适应调整。
//                if (b < y_min)
//                    y_min = b;
//                if (b > y_max)
//                    y_max = b;

                if (b > by) {
//            b = (short) by - 1;
                    b = (short) by;
                    b_autoResize = true;
                    change_type = 1;
                }

                if (b <= 0) {
                    b = 0;
                    b_autoResize = true;
                    change_type = 1;
                }
                return b;
            } else {
                int b = temp;
                if (isP_V_Adapter() == true) {
                    //最大值和最小值实时更新
//                    if (isyRealInitFlag() < width / 2) {
//                        if (b < y_minReal) {
//                            y_minReal = b;
//                        }
//                        if (b > y_maxReal) {
//                            y_maxReal = b;
//                        }
//                        setyRealInitFlag(isyRealInitFlag()+1);
//                    }

                    //极大值，极小值跟随实时数据更新防止数据绘制越界
                    if (b < y_minReal) {
                        y_minReal = b;
                    }
                    if (b > y_maxReal) {
                        y_maxReal = b;
                    }

                    //记录后续的数据极大值极小值是否一直低于实时更新的值，
                    //Log.e("AdapterSpO2_2", "Real Max " + y_maxReal + ", Real Min" + y_minReal + ", RealLastMax " + y_maxRealLast + ", RealLastMin " + y_minRealLast);
                    extremePointDynamic(b);
                    //Log.e("AdapterSpO2_3", "Real Max " + y_maxReal + ", Real Min" + y_minReal + ", RealLastMax " + y_maxRealLast + ", RealLastMin " + y_minRealLast);

//                    if (b < y_minRealLast) {
//                        y_minRealLast = b;
//                    }
//                    if (b > y_maxRealLast) {
//                        y_maxRealLast = b;
//                    }
                } else {
                    //最大值和最小值固定

                }
                //判断区间范围，调整基线到y_min
                BaseLineRef = y_minReal - y_min;
                b = b - BaseLineRef;
                //更新缩放系数
                if (y_maxReal == y_minReal || y_max == y_min) {

                } else {
                    scalVal = ((float) (Math.abs(y_maxReal - y_minReal))) / ((float) Math.abs(y_max - y_min));
                    b = (int) (b / scalVal);
//                    Log.e("DrawSize", "SpO2 Real y_max" + y_max + ", b " + b);
//                    Log.e("DrawSize2", " P_V " + isP_V_Adapter() + "baseline_voltage " + baseline_voltage + "y_min " + y_min + " y_minReal " + y_minReal + " y_max " + y_max + " y_maxReal " + y_maxReal + " BaseLineRef " + BaseLineRef + ", Temp " + temp + " scalVal " + scalVal + " b " + b);
                }

                //强制调整基线
                b += baseline_voltage;

                return b;
            }
        } else {
            return (int) (temp + baseline_voltage);
        }
    }

    /*
        Desc: 绘图（刷新）制定区域区域
     */

    //    private int nFreshCount = 0;
    private void ScreenResh() {

        if (n_ecgx > bx - 5)    //如果曲线到头
        {
            //判断（ymax-ymin）的值是不是小于高度一半，如果小于，就要自动调整
//            if ((y_max - y_min) * 2 < by) {
//                b_autoResize = true;
//                change_type = 1;
//            }
            n_ecgx = 0;
            Rect rect = new Rect(0, 0, 20, height);    //表示更新的区域,从0到20的X轴
            machineCanvas.drawBitmap(cacheBitmap, rect, rect, bmpPaint);
//            if (b_autoResize && waveAdapter) {
//                AutoResize();
//            }
//            checkRange();

//            if (waveAdapter == true && P_V_Adapter == true) {
//                //矫正实时最大最小值
//                if (getY_maxRealLast() != -1) {
//                    setY_maxReal(getY_maxRealLast());
//                }
//                if (getY_minRealLast() != -1) {
//                    setY_minReal(getY_minRealLast());
//                }
//                //Log.e("Adapter4Draw", "Real : " + getY_maxReal() + " " + getY_minReal() + ", RealLast : " + getY_maxRealLast() + " " + getY_minRealLast());
//                //Log.e("DrawSize", "SpO2 Real" + rect.toString());
//                //初始化参数，用来重新记录一屏的峰值
//                //setY_minRealLast(0x7FFFFFFF);
//                //setY_maxRealLast(-0x7FFFFFFF);
//            }
        }

//        nFreshCount++;
//        if (nFreshCount % 250 == 0) {
//            nFreshCount = 0;
//            if (waveAdapter == true && P_V_Adapter == true) {
//                //矫正实时最大最小值
//                setY_maxReal(getY_maxRealLast());
//                setY_minReal(getY_minRealLast());
//                //Log.e("Adapter4Draw", "Real : " + getY_maxReal() + " " + getY_minReal() + ", RealLast : " + getY_maxRealLast() + " " + getY_minRealLast());
//                //Log.e("DrawSize", "SpO2 Real" + rect.toString());
//                //初始化参数，用来重新记录一屏的峰值
//                setY_minRealLast(0x7FFFFFFF);
//                setY_maxRealLast(-0x7FFFFFFF);
//            }
//        }

        if (waveAdapter == true && P_V_Adapter == true) {
            //矫正实时最大最小值
            //Log.e("AdapterSpO2_before", "Real Max " + y_maxReal + ", Real Min" + y_minReal);
//            if (getY_maxRealLast() != -1) {
//                setY_maxReal(getY_maxRealLast());
//            }
//            if (getY_minRealLast() != -1) {
//                setY_minReal(getY_minRealLast());
//            }
//            HashMap<String, String> params = new HashMap<>();
//            //params.put("DrawViewBuff", "SpO2 " + Integer.toString(nSpO2BufferLen) + ", ECG " + Integer.toString(nECGBufferLen) + ", Resp " + Integer.toString(nRespBufferLen));
//            String writeData = String.format("RealMax %d, RealMin %d, RealLastMax %d, RealLastMin %d", getY_maxReal(), getY_minReal(), getY_maxRealLast(), getY_minRealLast());
//            //Log.e("AdapterSpO2", writeData);
//            params.put("AdapterSpO2", writeData);
//            EventBus.getDefault().post(params);
            //Log.e("Adapter4Draw", "Real : " + getY_maxReal() + " " + getY_minReal() + ", RealLast : " + getY_maxRealLast() + " " + getY_minRealLast());
            //Log.e("DrawSize", "SpO2 Real" + rect.toString());
            //初始化参数，用来重新记录一屏的峰值
            //setY_minRealLast(0x7FFFFFFF);
            //setY_maxRealLast(-0x7FFFFFFF);
        }
        Rect rect = new Rect(n_ecgx + n_step + 10, 0, n_ecgx + n_step + 20, height);
//        Log.e("DrawSize", "SpO2 Real" + rect.toString());
//        Rect rect = new Rect(n_ecgx + 2, 0, n_ecgx + n_step + 2, height);
        machineCanvas.drawBitmap(cacheBitmap, rect, rect, bmpPaint);
    }


    //自适应调整
    public void AutoResize() {
        if (change_type == 1) {
            if ((y_max - y_min) * 2 >= by && (y_max - y_min) <= by) {
                //波形的幅度小于画布高度，并且波形幅度的2倍大于画布高度，说明波形幅度合适，此时只要调整基线
                change_type = 2;
            } else {
                //表示波形范围超过画布高度
                if (y_max - y_min >= by) {
                    change_nV = change_nV * 2;
                    if (listener != null) {
                        listener.onShowMessage(this, change_nV + "", 1);
                    }
                } else if ((y_max - y_min) * 2 <= by) {
                    //如果波形幅度的两倍都小于画布高度，说明波形幅度过小，需要波形像素调整放大
                    change_nV = change_nV / 2;
                    if (listener != null) {
                        listener.onShowMessage(this, change_nV + "", 1);
                    }
                }

                checkRange();
                return;
            }
        }

        if (change_type == 2) {
            int n_top = (by - (y_max + y_min)) / 2;
            this.baseline_voltage += n_top * change_nV / change_50n;
            if (listener != null) {
                listener.onShowMessage(this, baseline_voltage + "", 2);
            }
        }
        b_autoResize = false;
    }


    public void checkRange() {
//        this.y_max = -3 * this.by;
//        this.y_min = 3 * this.by;
//        this.y_max = this.by;
//        this.y_min = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.i("tag", "手势放下");
                mode = 1;
                break;
            case MotionEvent.ACTION_UP:
                mode = 0;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.i("tag", "手势拿起" + spacing(event));
                newDistance = spacing(event);
                mode -= 1;
                if (newDistance > oldDistance + DISTANCE) {
                    n_step = n_step * 2;
                    n_Btime = (int) (n_Ptime * change_50n / n_step);
                    if (listener != null) {
                        listener.onShowMessage(this, n_Btime + "", 0);
                    }
                    Log.i("tag", "n_step-->" + n_step + ";n_Btime-->" + n_Btime);
                } else if (newDistance < oldDistance - DISTANCE) {
                    if (n_step >= 2) {
                        n_step = n_step / 2;
                        n_Btime = (int) (n_Ptime * change_50n / n_step);
                        if (listener != null) {
                            listener.onShowMessage(this, n_Btime + "", 0);
                        }
                        Log.i("tag", "n_step-->" + n_step + ";n_Btime-->" + n_Btime);
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.i("tag", "手势放下" + spacing(event));
                oldDistance = spacing(event);
                mode += 1;
                break;

            case MotionEvent.ACTION_MOVE:
                break;
        }
        return true;
    }


    private float spacing(MotionEvent var1) {
        float var2 = var1.getX(0) - var1.getX(1);
        float var3 = var1.getY(0) - var1.getY(1);
        return (float) Math.sqrt((double) (var2 * var2 + var3 * var3));
    }
}
