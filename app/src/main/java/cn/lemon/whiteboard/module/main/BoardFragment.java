package cn.lemon.whiteboard.module.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import cn.lemon.common.base.fragment.SuperFragment;
import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.widget.BoardView;

/**
 * 画板页
 */
public class BoardFragment extends SuperFragment {

    public BoardView mBoardView;
    private BoardView.OnDownAction mOnDownAction;
    private BoardViewCreateListener mBoardViewCreateListener;

    public BoardFragment() {
        super(R.layout.board_fragment);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBoardView = (BoardView) findViewById(R.id.board_view);
        if (mOnDownAction != null) {
            mBoardView.setOnDownAction(mOnDownAction);
        }
        if (mBoardViewCreateListener != null) {
            mBoardViewCreateListener.onBoardViewCreate(mBoardView);
        }
    }

    public void setOnDownAction(BoardView.OnDownAction onDownActio) {
        this.mOnDownAction = onDownActio;
    }

    public void setBoardViewCreateListener(BoardViewCreateListener boardViewCreateListener) {
        this.mBoardViewCreateListener = boardViewCreateListener;
    }

    interface BoardViewCreateListener {
        void onBoardViewCreate(BoardView boardView);
    }
}
