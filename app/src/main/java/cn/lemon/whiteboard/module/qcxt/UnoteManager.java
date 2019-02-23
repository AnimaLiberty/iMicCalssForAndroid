package cn.lemon.whiteboard.module.qcxt;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.lemon.whiteboard.app.App;

public class UnoteManager {
	private static PointFactory pointFactory;
	private static Refreshd mSetPoint;
	private static int delete = 0;
	private static List<String> dataList = new ArrayList<String>();
	private static boolean key = false;
	private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 11) {
				if (mSetPoint != null) {
					mSetPoint.refresh();
				}else{
					Log.w("qcxt","mSetPoint is null");
				}
			} else {
				Log.w("qcxt",msg.obj.toString());
				if(!key){
					Log.w("qcxt","key is false");
					if(dataList.size()==4){
						checkString(dataList);
						dataList.clear();
					}
				}else{
					Log.w("qcxt","key is true");
				}
			}
		};
	};
	public static void init(Application context) {
		pointFactory = new PointFactory();
		App.getInstance().setPointFactory(pointFactory);
		mSetPoint = App.getInstance().getsPoint();
	}

	public static void onDestroy() {
	}

	private static int upFingerCount = 0;
	private static boolean isDownFinger = true;

	// TODO read
	public static void readDataFromSerial(byte[] rbuf) {

		if(mSetPoint == null){
			mSetPoint = App.getInstance().getsPoint();
		}
		if (mSetPoint != null) {
			//笔迹
			for (int i = 0; i < rbuf.length; i++) {
				if (delete > 0) {
					delete--;
					continue;
				}
				if (rbuf[i] == -4) { // FC
					pointFactory.refreshQueue();
					delete = 4;
					continue;
				}
				if (rbuf[i] == -1) { // FF
					pointFactory.refreshQueue();
					delete = 0;
					continue;
				}

				/*if (rbuf[i] < 0 ) {
					delete = 3;
//					pointFactory.refreshQueue();
//					pointFactory.cut();
					pointFactory.putPoint(rbuf[i],2);
					continue;
				}*/
				{
					pointFactory.putPoint(rbuf[i],1);
				}
			}
		} else {
			mSetPoint = App.getInstance().getsPoint();
		}
		Message msg = handler.obtainMessage();
		msg.what = 11;
		handler.sendMessage(msg);
		msg = handler.obtainMessage();
		msg.obj = printHexString(rbuf);
		Log.e("data", Arrays.toString(rbuf));
		handler.sendMessage(msg);
	}// readDataFromSerial

	public void getHandler(){

	}

	public static String printHexString(byte[] b) {
		String tempString = "";
		for (int i = 0; i < 33; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			if("FA".equals(hex.toUpperCase())){
				key = true;
			}
			if("FF".equals(hex.toUpperCase())){
				key = false;
			}
			if(key){
				if(!"00".equals(hex)){
					dataList.add(hex.toUpperCase());
				}
			}
			if (!"00".equals(hex)) {
				tempString += hex.toUpperCase();
			}
		}
		return tempString;
	}

	public static void checkString(List<String> data) {
		String deviceNum = data.get(1);
		String buttonNum = data.get(3);
		/*if (buttonNum.contains("05")) {
			ToastUtil.showMessage(deviceNum+"号按了OK键");
		}else if (buttonNum.contains("04")) {
			ToastUtil.showMessage(deviceNum+"号按了4D键");
		}else if (buttonNum.contains("03")) {
			ToastUtil.showMessage(deviceNum+"号按了3C键");
		}else if (buttonNum.contains("02")) {
			ToastUtil.showMessage(deviceNum+"号按了2B键");
		}else if(buttonNum.contains("01")){
			ToastUtil.showMessage(deviceNum+"号按了1A键");
		}else{
			ToastUtil.showMessage("错误数据");
		}*/
	}

	public static interface onConnectListener {
		void onConnect();
	}

	public static onConnectListener mListener;

	public static void setOnConnectListener(onConnectListener listener) {
		mListener = listener;
	}
}
