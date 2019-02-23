package cn.lemon.whiteboard.screen.util;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2018/3/8 0008.
 */

public class CommonUtil {

    private static int mScreenWidth;
    private static int mScreenHeight;

    private static int mScreenDpi;

    public static void init(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDpi = metrics.densityDpi;
    }

    public static int getScreenWidth() {
        return mScreenWidth;
    }

    public static int getScreenHeight() {
        return mScreenHeight;
    }

    public static int getScreenDpi() {
        return mScreenDpi;
    }

    /**
     * 获取视频存放路径
     *
     * @param context
     * @return
     */
    public static String getVideoCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

}
