/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.lemon.whiteboard.module.qcxt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.lemon.whiteboard.R;

/*
 * DeviceListActivity��һ����Ϊ���������Ի������
 */
public class DeviceListActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;

    // private BluetoothAdapter mBtAdapter;
    private TextView mEmptyList;
    public static final String TAG = "DeviceListActivity";

    List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 10000; // 10 seconds
    private Handler mHandler;
    private boolean mScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.device_list);
//        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
//        layoutParams.gravity = Gravity.TOP;
//        layoutParams.y = 200;

        mHandler = new Handler();
        /**
         * hasSystemFeature(����):�жϱ����Ƿ�֧�ֲ�������ָ��������
         * PackageManager.FEATURE_BLUETOOTH_LE������֧����Զ��ble�豸�������ݴ���
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "������֧��BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ����һ����������������>=18�汾��api�У�����һ��������������Ҫͨ��BluetoothManager������
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // ͨ��������������������鱾���Ƿ�֧��BLE����
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "������֧��BLE", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateList();

        mEmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * ֻ����10����֮���������߳����潲mScanning��ֵΪfalse�ˣ��������߳��а�ť�����ֱ��ó���scan��
                 * ���Կ���ͨ���˱�־���ó���ʱ��ť�ϵ�����Ϊscan����Ҫ�����ť����ô�ͻ�ִ��scan����
                 */
                if (mScanning == false)
                    scanLeDevice(true);
                /**
                 * ���mScanning��=false����ô�����жϳ���ǰ��ť�ϵ�����Ϊcancle��
                 * ��������ͱ���ִ��cancle��Ӧ�Ĳ���
                 */
                else
                    finish();
            }
        });

    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceList);
        devRssiValues = new HashMap<String, Integer>();

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        // ����б����Ӧ�������豸��mac��ַͨ��intent�����͵�MainActivityҳ��
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        scanLeDevice(true);
    }

    /**
     * 扫描Ble设备
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        if (enable) {
            // 10s��֮��ʼִ��Runnable����ĺ�����
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.out.println("ֹͣ����ble");
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    cancelButton.setText(android.R.string.cancel);
                }
            }, 10000);
            // ����if������֮�����Ȼ�ִ����������д��룬10�����Ժ�Ὺ��һ�����̣߳������߳�����ִ������Runnable���������
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            cancelButton.setText(android.R.string.cancel);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            cancelButton.setText(android.R.string.cancel);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(device, rssi);
                        }
                    });
                }
            });
        }
    };

    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;
        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
            deviceList.add(device);
            mEmptyList.setVisibility(View.GONE);

            deviceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {
        super.onStop();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = deviceList.get(position);
            // �����������������������ô�ͽ���ֹͣ����
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            Bundle b = new Bundle();
            /**
             * key��BluetoothDevice.EXTRA_DEVICE������һ���ַ���
             * value��deviceList.get(position).getAddress()����
             * ��ѡ���б�������Ӧ��ble�����豸��mac��ַ
             */
            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());
            // ͨ��intent��key��valueֵ���͵�MainActivityҳ��
            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    };

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("Rssi = " + String.valueOf(rssival));
            }

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::" + device.getName());
                tvname.setTextColor(context.getResources().getColor(R.color.dark_gray));
                tvadd.setTextColor(context.getResources().getColor(R.color.dark_gray));
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText("repair");
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(context.getResources().getColor(R.color.dark_gray));

            } else {
                tvname.setTextColor(context.getResources().getColor(R.color.dark_gray));
                tvadd.setTextColor(context.getResources().getColor(R.color.dark_gray));
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(context.getResources().getColor(R.color.dark_gray));
            }
            return vg;
        }
    }

}
