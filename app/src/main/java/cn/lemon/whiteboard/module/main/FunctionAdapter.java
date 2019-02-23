package cn.lemon.whiteboard.module.main;

import android.content.Context;
import android.widget.TextView;

import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.app.App;
import cn.lemon.whiteboard.widget.BoardView;
import cn.lemon.whiteboard.widget.FloatAdapter;
import cn.lemon.whiteboard.widget.shape.MultiLineShape;
import cn.lemon.whiteboard.widget.shape.Type;

class FunctionAdapter extends FloatAdapter {

    private TextView tvIndicate;
    private String[] mHints = {"删除", "多边形", "圆形", "矩形", "曲线", "直线"};
    private int[] mDrawables = {
            R.drawable.delete_page,
            R.drawable.new_multi_line,
            R.drawable.new_oval,
            R.drawable.new_rectangle,
            R.drawable.new_curve,
            R.drawable.new_line};

    private BoardView mBoardView;

    public FunctionAdapter(Context context, BoardView boardView, TextView tvIndicate) {
        super(context);
        mBoardView = boardView;
        this.tvIndicate = tvIndicate;
    }

    @Override
    public int getCount() {
        return mHints.length;
    }

    @Override
    public String getItemHint(int position) {
        return mHints[position];
    }

    @Override
    public int getItemResource(int position) {
        return mDrawables[position];
    }

    @Override
    public int getMainResource() {
        return R.drawable.ic_float_switch;
    }

    @Override
    public void onItemClick(int position) {
        switch (position) {
            case 0:// 删除页面
//                mBoardView.clearScreen();  清屏
//                if (getContext() instanceof MainActivity) {
//                    ((MainActivity) getContext()).getPresenter().setLocalNoteNull();
//                }
                break;
            case 1:// 多边形
                mBoardView.setDrawType(Type.MULTI_LINE); // 多边形
                MultiLineShape.clear();
                tvIndicate.setText("多边形");
                break;
            case 2:// 圆形
                mBoardView.setDrawType(Type.OVAL);// 圆形
                tvIndicate.setText("圆形");
                break;
            case 3:// 矩形
                mBoardView.setDrawType(Type.RECTANGLE);// 矩形
                tvIndicate.setText("矩形");
                break;
            case 4:// 曲线
                mBoardView.setDrawType(Type.CURVE);// 曲线
                App.getInstance().getPointFactory().clearScreen();
                tvIndicate.setText("曲线");
                break;
            case 5:// 直线
                mBoardView.setDrawType(Type.LINE);// 直线
                tvIndicate.setText("直线");
                break;
            case 6:
                break;
        }
    }

    /**
     * 刷新控制的画板
     *
     * @param boardView
     */
    public void refreshBoardView(BoardView boardView) {
        this.mBoardView = boardView;
    }
}