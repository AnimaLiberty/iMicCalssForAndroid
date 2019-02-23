package cn.lemon.whiteboard.module.qcxt;

import java.util.LinkedList;
import java.util.Queue;

import cn.lemon.whiteboard.module.main.MainActivity;

public class PointFactory {

	Queue<MyPoint> queue = new LinkedList<MyPoint>();
	Float[] po = new Float[4];
	MyPoint temppPoint;

	public PointFactory() {
		// TODO Auto-generated constructor stub
		refreshQueue();
		queue.add(new MyPoint(-1, -1,0));
	}

	public void clearScreen(){
		if (queue != null){
			queue.clear();
			MainActivity.isClearScreen = true;
		}
	}

	public Queue<MyPoint> getPoints() {
		Queue<MyPoint> queue1 = queue;

		queue = new LinkedList<MyPoint>();
		if (temppPoint != null) {
			queue.add(temppPoint);
		}

		return queue1;
	}

	public void putPoint(int i,int flag) {

		for (int j = 0; j <= 3; j++) {
			if (po[j] == -1) {
				po[j] = (float) i;
				break;
			}
		}
		if (po[3] != -1) {
			temppPoint = new MyPoint(po[0] * 128 + po[1], po[2] * 128 + po[3],flag);
			queue.add(temppPoint);
			refreshQueue();
		}
	}

	public void refreshQueue() {
		po[0] = (float)-1;
		po[1] = (float)-1;
		po[2] = (float)-1;
		po[3] = (float)-1;
	}



	public void cut() {
		temppPoint = new MyPoint(-1, -1,2);
		queue.add(temppPoint);
		refreshQueue();
	}
}
