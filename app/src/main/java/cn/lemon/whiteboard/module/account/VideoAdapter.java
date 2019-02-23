package cn.lemon.whiteboard.module.account;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import cn.lemon.view.adapter.BaseViewHolder;
import cn.lemon.view.adapter.RecyclerAdapter;
import cn.lemon.whiteboard.R;

public class VideoAdapter extends RecyclerAdapter<File> {

    private Context mContext;

    public VideoAdapter(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public BaseViewHolder<File> onCreateBaseViewHolder(ViewGroup parent, int viewType) {
        return new VideoViewHolder(parent);
    }

    private class VideoViewHolder extends BaseViewHolder<File> {

        private TextView mTitle;

        public VideoViewHolder(ViewGroup parent) {
            super(parent, R.layout.account_holder_video);
        }

        @Override
        public void onInitializeView() {
            super.onInitializeView();
            mTitle = findViewById(R.id.tv_title);
        }

        @Override
        public void onItemViewClick(File object) {
            Intent intent = new Intent(mContext, VideoPlayActivity.class);
            intent.putExtra("videoPath", object.getPath());
            mContext.startActivity(intent);
        }

        @Override
        public void setData(final File video) {
            super.setData(video);
            if (video != null) {
                mTitle.setText(video.getName());
            }
        }
    }
}
