package cn.lemon.whiteboard.widget.shape;

import android.graphics.Color;

/**
 * Created by user on 2016/10/26.
 */

public class WipeShape extends CurveShape {

    public WipeShape(int width) {

        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(width);
    }

    public WritablePath getPath(){
        mPaint.mColor = Color.WHITE;
        mPaint.mWidth = 100;
        mPath.mPaint = mPaint;
        return mPath;
    }
}
