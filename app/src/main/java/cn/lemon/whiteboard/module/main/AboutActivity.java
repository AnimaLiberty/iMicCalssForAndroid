package cn.lemon.whiteboard.module.main;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.alien95.util.Utils;
import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.BuildConfig;
import cn.lemon.whiteboard.R;

public class AboutActivity extends ToolbarActivity implements View.OnClickListener {

    private LinearLayout mTellAuthor;
    private TextView mFeedBack;
    private TextView tvVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_about);

        mTellAuthor = (LinearLayout) findViewById(R.id.tell_author);
        mFeedBack = (TextView) findViewById(R.id.feedback);
        tvVersionName = (TextView) findViewById(R.id.tv_version_name);
        mTellAuthor.setOnClickListener(this);
        mFeedBack.setOnClickListener(this);

        tvVersionName.setText(BuildConfig.VERSION_NAME);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tell_author:
                Utils.jumpDialUI(this, "18983679028");
                break;
        }
    }
}
