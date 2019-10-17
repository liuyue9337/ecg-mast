package com.ljfth.ecgviewlib.base;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.algorithm4.library.algorithm4library.Algorithm4Library;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.ButterKnife;

import com.ljfth.ecgviewlib.BackUsing.CMyDeamonProcess;
import com.ljfth.ecgviewlib.BackUsing.CMyTcpTransfer;
import com.ljfth.ecgviewlib.BackUsing.CSingleInstance;
import com.ljfth.ecgviewlib.BackUsing.LifeSignParamRecord;
import com.ljfth.ecgviewlib.MainActivity;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by warner on 2017/9/29.
 */

public abstract class BaseActivity extends AppCompatActivity {
	protected CSingleInstance gTransferData = CSingleInstance.getInstance();
	//protected UsbManager mUsbManager;
	//protected UsbSerialPort mPort;
	private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	//private SerialInputOutputManager mSerialIoManager;
	private BroadcastReceiver mReceiver;



	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 在界面未初始化之前调用的初始化窗口
		if (gTransferData.m_atomDeamonProcess.compareAndSet(0, 1)) {
			initDeamonProcess();
		}

		initWindows();

		if (initArgs(getIntent().getExtras())) {
			setContentView(getcontentLayoutId());
			initWidget();
			initData();
		} else {
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String action = intent.getAction();
		if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
			initPort();
			//配置串口参数
			PortConfigure();
			//配置串口监听
			onDeviceStateChange();

			//初始化模块
			// 血氧
//			writeIoManage(GeneralSpO2Command(true));
			// 心电
//			writeIoManage(GeneralECGCommand(true));
			 // 呼吸
//			writeIoManage(GeneralRespCommand(true));
			// 血压
//			writeIoManage(GeneralNIBPCommand(true));
			//温度
//			writeIoManage(GeneralTempCommand(true));


			//Toast.makeText(this, "onNewIntent Attached", Toast.LENGTH_SHORT).show();
		}
		if (action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
			NoDeviceDetached();
			//Toast.makeText(this, "onNewIntent Detached in active", Toast.LENGTH_SHORT).show();
		}
//        Toast.makeText(this, "onNewIntent", Toast.LENGTH_SHORT).show();
	}

	protected void PortConfigure() {
		if (gTransferData.mPort == null) {
			//Toast.makeText(this, "No serial device", Toast.LENGTH_SHORT).show();
		} else {
			UsbDeviceConnection connection = gTransferData.mUsbManager.openDevice(gTransferData.mPort.getDriver().getDevice());
			if (connection == null) {
				//Toast.makeText(this, "Opening device failed", Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				gTransferData.mPort.open(connection);
				gTransferData.mPort.setParameters(230400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
				gTransferData.m_bUSB_Is_Ready = true;
			} catch (IOException e) {
				//Toast.makeText(this, "Error setting up device: " + e.getMessage(), Toast.LENGTH_SHORT).show();

				try {
					gTransferData.mPort.close();
				} catch (IOException e2) {
					// Ignore.
				}
				gTransferData.mPort = null;
				return;
			}
			//Toast.makeText(this, "Serial device : " + gTransferData.mPort.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
		}
	}
    private void initDeamonProcess() {
		//开启后台守护进程，准备好Tcp连接服务
		if (gTransferData.m_MyTcpTransfer == null) {
			gTransferData.m_MyTcpTransfer = new CMyTcpTransfer();
			Log.i("TcpTransfer", "m_MyTcpTransfer is running ... ..." + gTransferData.m_MyTcpTransfer.toString());
			new Thread(gTransferData.m_MyTcpTransfer).start();
			try {
				Thread.currentThread().sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//开启后台守护线程，准备好数据数据接收处理
		if (gTransferData.m_MyDeamonProcess == null) {
			gTransferData.m_MyDeamonProcess = new CMyDeamonProcess();
			Log.i("DeamonProcess", "m_MyDeamonProcess is running .. ..." + gTransferData.m_MyDeamonProcess.toString());
			new Thread(gTransferData.m_MyDeamonProcess).start();
			try {
				Thread.currentThread().sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		LifeSignParamRecord.sendUpdateBroadcast(getBaseContext());
    }
	/**初始化窗口*/
	protected void initWindows() {

	}

	/**
	 * 初始化相关参数
	 * 参数正确返回true，错误返回false
	 */
	protected boolean initArgs(Bundle bundle) {
		return true;
	}

	/**获取界面资源ID*/
	protected abstract int getcontentLayoutId();

	/**初始化控件*/
	protected void initWidget() {
		ButterKnife.bind(this);

		if (gTransferData.mUsbManager == null) {
            gTransferData.mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            //initPort();
        }

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String curItentActionName = intent.getAction();
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(curItentActionName)) {
					NoDeviceDetached();
				}
			}
		};
		// ACTION_USB_DEVICE_DETACHED 这个事件监听需要通过广播，activity监听不到
		IntentFilter filter = new IntentFilter();
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        filter.addAction(ACTION_USB_DEVICE_PERMISSION);
		registerReceiver(mReceiver, filter);

		Log.e("test", "onCreate");
		//Algorithm4Library.InitSingleInstance();
	}

	/**初始化数据*/
	protected void initData() {

	}

	@Override
	public boolean onSupportNavigateUp() {
		// 当点击界面导航返回时，finish当前界面
		finish();
		return super.onSupportNavigateUp();
	}

	@Override
	public void onBackPressed() {
		// 获取当前activity下的所有的Fragment
		@SuppressLint("RestrictedApi")
		List<Fragment> fragments = getSupportFragmentManager().getFragments();
		// 判断是否为空
		if (fragments != null && fragments.size() > 0) {
			for (Fragment fragment :fragments) {
				// 判断是否是我们能够处理的fragment的类型
				if (fragment instanceof BaseFragment) {
					// 判断是否拦截了返回按钮
					if (((BaseFragment) fragment).onBackPressed()) {
						// 拦截返回按钮，直接return
						return;
					}
				}
			}
		}
		super.onBackPressed();
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	public boolean bUSB_Sta = false;

	/**
	 * 该方法默认设定是在驱动第一次识别时调用
	 * 现在需要将USB句柄存储在CSinggleInstance中，在UI初始化的时候恢复状态
	 */
	protected void initPort() {
		List<UsbSerialDriver> drivers =
				UsbSerialProber.getDefaultProber().findAllDrivers(gTransferData.mUsbManager);
		if (drivers != null) {
			for (UsbSerialDriver driver : drivers) {
				List<UsbSerialPort> ports = driver.getPorts();
				if (ports != null && ports.size() == 1) {
					//synchronized (BaseActivity.class){
						gTransferData.mPort = ports.get(0);
						//这个位置的USB_Sta只表示驱动识别到了USB设备，并没有打开
						bUSB_Sta = true;
					//}
                    //Toast.makeText(this, "发现一个USB设备,Port = " + gTransferData.mPort, Toast.LENGTH_SHORT).show();
				} else {
					//Toast.makeText(this, "没有发现USB,或者USB设备超过1个", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	protected void stopIoManager() {
		if (gTransferData.mSerialIoManager != null) {
//            Toast.makeText(MainActivity.this, "Stopping io manager ..", Toast.LENGTH_SHORT).show();
            gTransferData.mSerialIoManager.stop();
            gTransferData.mSerialIoManager = null;
		}
	}

	protected void startIoManager() {
		if (gTransferData.mPort != null) {
//            Toast.makeText(MainActivity.this, "Starting io manager ..", Toast.LENGTH_SHORT).show();
            gTransferData.mSerialIoManager = new SerialInputOutputManager(gTransferData.mPort, mListener);
			mExecutor.submit(gTransferData.mSerialIoManager);
		}
	}

	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {

				@Override
				public void onRunError(Exception e) {
					//Toast.makeText(BaseActivity.this, "Runner stopped.", Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onNewData(final byte[] data) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updateReceivedData(data);
//							CSingleInstance mSingIn = CSingleInstance.getInstance();
//							int nWriteIndex = mSingIn.m_atomWriteIndex.getAndAdd(data.length);
//							System.arraycopy(data, 0, mSingIn.m_cRecvBuffer, nWriteIndex, nWriteIndex);
//							mSingIn.m_atomBuffDataLen.addAndGet(data.length);
						}
					});
				}
			};

	protected void updateReceivedData(byte[] data){
		gTransferData.updateReceivedDataExt(data);
		//Toast.makeText(BaseActivity.this, "Data Len " + data.length, Toast.LENGTH_SHORT).show();
	}

	protected void NoDeviceDetached(){
		//Toast.makeText(this, "NoDeviceDetached", Toast.LENGTH_SHORT).show();
	}

	protected void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	protected boolean writeIoManage(byte[] array) {
		String str = String.format("array len %d", array.length);
		//Log.i("test", "recv len " + str);
		if (gTransferData.mPort != null && gTransferData.m_bUSB_Is_Ready == true) {
			if (array.length > 0) {
				try {
					int nRet = gTransferData.mPort.write(array, 100);
					str = String.format("w succ %d", nRet);
					//Toast.makeText(BaseActivity.this, str, Toast.LENGTH_SHORT).show();
					//Log.e("test", "Serial testwrite success" + str);
					return true;
				} catch (IOException e2) {
					//ignore
					str = String.format("write error ");
					//Toast.makeText(BaseActivity.this, str, Toast.LENGTH_SHORT).show();
					Log.e("test", "recv len " + str);
					Log.e("test", "Serial testwrite error" + str);
					//mTextViewHR.setText("write Error");
				}
			}
		}
		return false;
	}

	//*************************一些命令*************************

	// 打开或关闭血氧
	protected byte[] GeneralSpO2Command(boolean isOpen) {

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

	// 打开或关闭心电
	protected byte[] GeneralECGCommand(boolean isOpen) {

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
		array[5] = (byte) (0x01);
		array[6] = (byte) (0x00);
		array[7] = (byte) (0x00);
		array[8] = (byte) (0x55);
		array[9] = (byte) (0x55);

		return array;
	}

	//打开或关闭呼吸
	protected byte[] GeneralRespCommand(boolean isOpen) {

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
		array[5] = (byte) (0x03);
		array[6] = (byte) (0x00);
		array[7] = (byte) (0x00);
		array[8] = (byte) (0x55);
		array[9] = (byte) (0x55);

		return array;
	}

	// 打开或关闭温度
	protected byte[] GeneralTempCommand(boolean isOpen) {

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
	protected byte[] GeneralNIBPCommand(boolean isOpen) {

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

}
