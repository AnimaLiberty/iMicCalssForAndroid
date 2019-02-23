package cn.lemon.whiteboard.module.account;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;

import java.io.File;
import java.util.List;

import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.common.base.presenter.RequirePresenter;
import cn.lemon.view.RefreshRecyclerView;
import cn.lemon.view.adapter.Action;
import cn.lemon.whiteboard.R;

@RequirePresenter(VideoPresenter.class)
public class VideoActivity extends ToolbarActivity<VideoPresenter> {

    private RefreshRecyclerView mRecyclerView;
    private VideoAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        useStatusPages(true);
        setContentView(R.layout.account_activity_note);

        mRecyclerView = (RefreshRecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mAdapter = new VideoAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addRefreshAction(new Action() {
            @Override
            public void onAction() {
                getPresenter().getData();
            }
        });
    }

    public void setData(List<File> data) {
        mAdapter.clear();
        mAdapter.addAll(data);
        mRecyclerView.dismissSwipeRefresh();
    }
}
