package cn.lemon.whiteboard.module.account;

import java.io.File;
import java.util.Collections;
import java.util.List;

import cn.lemon.common.base.presenter.SuperPresenter;
import cn.lemon.whiteboard.data.AccountModel;

public class VideoPresenter extends SuperPresenter<VideoActivity> {

    @Override
    public void onCreate() {
        super.onCreate();
        getData();
    }

    /**
     * 获取视频数据
     */
    public void getData() {
        AccountModel.getInstance().getVideoList(new AccountModel.VideoCallback() {
            @Override
            public void onCallback(List<File> data) {
                if (data.size() > 0) {
                    getView().showContent();
                    Collections.reverse(data);
                    getView().setData(data);
                } else {
                    getView().showEmpty();
                }
            }
        });
    }

}
