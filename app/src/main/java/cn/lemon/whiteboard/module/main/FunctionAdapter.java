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


    public FunctionAdapter(Context context, TextView tvIndicate) {
        super(context);
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
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).deletePage();
                }
                break;
            case 1:// 多边形
                setPenType(Type.MULTI_LINE); // 多边形
                MultiLineShape.clear();
                tvIndicate.setText("多边形");
                break;
            case 2:// 圆形
                setPenType(Type.OVAL);// 圆形
                tvIndicate.setText("圆形");
                break;
            case 3:// 矩形
                setPenType(Type.RECTANGLE);// 矩形
                tvIndicate.setText("矩形");
                break;
            case 4:// 曲线
                setPenType(Type.CURVE);// 曲线
                App.getInstance().getPointFactory().clearScreen();
                tvIndicate.setText("曲线");
                break;
            case 5:// 直线
                setPenType(Type.LINE);// 直线
                tvIndicate.setText("直线");
                break;
            case 6:
                break;
        }
    }

    private void setPenType(int type) {
        for (BoardView boardView : ((MainActivity) getContext()).getAllBoardView()) {
            boardView.setDrawType(type);
        }
    }

}