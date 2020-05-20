package com.ljfth.ecgviewlib;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ljfth.ecgviewlib.BackUsing.CSingleInstance;
import com.ljfth.ecgviewlib.base.BaseActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.OnClick;

import static com.ljfth.ecgviewlib.PatientInfoActivity.ACTION_SAVE;

public class IpsetActivity extends BaseActivity {


    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.ipAddr)
    EditText mIpAddr;

    @BindView(R.id.port)
    EditText mPort;

    @BindView(R.id.net_status)
    EditText mNetStatus;

    protected CSingleInstance gTransferData = CSingleInstance.getInstance();

   @Override
    protected int getcontentLayoutId() {
        return R.layout.activity_ipset;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        mTitle.setText("网络设置");
        mIpAddr.setText(EcgSharedPrefrence.getServerIp(this));
        mPort.setText(EcgSharedPrefrence.getServerPort(this));
        boolean isConnected = false;
        if (gTransferData.m_MyTcpTransfer != null) {
            isConnected = gTransferData.m_MyTcpTransfer.getConnectedSta();
        }
        mNetStatus.setText((isConnected ? "Connected" : "Disconnected"));
    }

    @OnClick({R.id.btn_back, R.id.btn_confirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_confirm:
                setData();
                break;
        }
    }

    /**
     * 设置数据
     */
    private void setData() {
        // 保存IP和端口
        String ip = mIpAddr.getText().toString();
        String port = mPort.getText().toString();
        String ipMsg[] = ip.split(".");
        try {
            if (ip.split("\\.").length != 4) {
                Toast.makeText(this, "Ip填写错误", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Integer.parseInt(port) <= 0) {
                Toast.makeText(this, "Port填写错误", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ip或Port填写错误", Toast.LENGTH_SHORT).show();
            return;
        }

        EcgSharedPrefrence.setServerIp(this, ip);
        EcgSharedPrefrence.setServerPort(this, port);

        if (gTransferData.m_MyTcpTransfer != null) {
            gTransferData.m_MyTcpTransfer.changeAddr(ip, Integer.parseInt(port));
        }

        HashMap<String, String> params = new HashMap<>();
        params.put("action", ACTION_SAVE);
        EventBus.getDefault().post(params);
        finish();
    }
}
