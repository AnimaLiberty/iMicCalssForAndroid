package cn.lemon.whiteboard.module.main;

import android.os.Bundle;
import android.support.annotation.Nullable;

import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.R;

public class ServiceActivity extends ToolbarActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_service);
    }
}
