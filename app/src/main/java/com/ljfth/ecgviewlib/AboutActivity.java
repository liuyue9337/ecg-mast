package com.ljfth.ecgviewlib;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.TextView;

import com.ljfth.ecgviewlib.base.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 */
public class AboutActivity extends BaseActivity {


    @BindView(R.id.title)
    TextView mTitle;

    @Override
    protected int getcontentLayoutId() {
        return R.layout.activity_about;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        mTitle.setText("关于");
    }

    @OnClick(R.id.btn_back)
    public void onViewClicked() {
        finish();
    }
}
