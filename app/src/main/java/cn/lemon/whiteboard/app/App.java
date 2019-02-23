package cn.lemon.whiteboard.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.WindowManager;

import cn.alien95.util.Utils;
import cn.lemon.common.base.model.SuperModel;
import cn.lemon.whiteboard.BuildConfig;
import cn.lemon.whiteboard.module.qcxt.PointFactory;
import cn.lemon.whiteboard.module.qcxt.Refreshd;

/**
 * Created by linlongxin on 2016/10/24.
 */

public class App extends Application {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Utils.initialize(this);
        SuperModel.initialize(this);
        if(BuildConfig.DEBUG){
            Utils.setDebug(true,"Whiteboard");
        }

    }

    /**
     * 单例，返回一个实例
     * @return
     */
    public static synchronized App getInstance() {
        if (instance == null) {
            Log.w("ECApplication","[ECApplication] instance is null.");
        }
        return instance;
    }
    private static App instance;

    public Refreshd sPoint;
    private PointFactory pointFactory;
    public Refreshd getsPoint() {
        return sPoint;
    }

    public void setsPoint(Refreshd sPoint) {
        if (sPoint != null)
            this.sPoint = sPoint;

    }
    public int getScreenWidth(){
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();

//        return DensityUtil.px2dip(this,width);
        return width;
    }

    public int  getScreenHeight(){
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
//        return DensityUtil.px2dip(this,height);
        return height;

    }

    public float getQCXTScale(){
        float x = 10500/(float)getScreenWidth();
        float y = 14850/(float)(getScreenHeight() - cn.lemon.whiteboard.module.qcxt.Utils.dp2Px(50));
        if (x > y) return x;
        else return y;
    }


    public PointFactory getPointFactory() {
        return pointFactory;
    }

    public void setPointFactory(PointFactory pointFactory) {
        this.pointFactory = pointFactory;
    }
}
