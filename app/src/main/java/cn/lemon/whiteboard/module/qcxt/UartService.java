package cn.lemon.whiteboard.module.qcxt;

/**
 * Created by lizheng on 2018/11/2.
 */

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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class UartService extends Service {
    private final static String TAG = UartService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.jimmy.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.jimmy.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.jimmy.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.jimmy.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.jimmy.ble.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.jimmy.ble.DEVICE_DOES_NOT_SUPPORT_UART";

    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public class LocalBinder extends Binder {
        public UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                System.out.println("broadcastUpdate:Connected to GATT server.");
                boolean isdiscovered = mBluetoothGatt.discoverServices();
                System.out.println("broadcastUpdate:Attempting to start service discovery:" + isdiscovered);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                System.out.println("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("onCharacteristicRead");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            System.out.println("onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {

            // Log.d(TAG, String.format("Received TX:
            // %d",characteristic.getValue() ));
//            byte[] byte1 = characteristic.getValue();
//            String str = new String(byte1);
//            System.out.println("���յ�������Ϊ��" + str);
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            System.out.println("�����豸δ��ʼ�����ߵ�ַδָ��");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            System.out.println("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            System.out.println("δ�����豸���޷�����");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        // mBluetoothGatt.close();
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */
    /*
     * public void setCharacteristicNotification(BluetoothGattCharacteristic
     * characteristic, boolean enabled) { if (mBluetoothAdapter == null ||
     * mBluetoothGatt == null) { Log.w(TAG, "BluetoothAdapter not initialized");
     * return; } mBluetoothGatt.setCharacteristicNotification(characteristic,
     * enabled);
     *
     *
     * if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
     * BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
     * UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
     * descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
     * mBluetoothGatt.writeDescriptor(descriptor); } }
     */

    /**
     * Enable TXNotification
     */
    // DA14580��TX_SERVICE_UUID=ffe5
    // PW0316��TX_SERVICE_UUID=ffe5
    // PW-01��TX_SERVICE_UUID=fff0
    public static final UUID TX_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    // DA14580��RX_SERVICE_UUID=fff0
    // PW0316��RX_SERVICE_UUID=ffe0
    // PW0517��RX_SERVICE_UUID=fff0
    public static final UUID RX_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    // DA14580��Rx_CHARACTERISTIC_UUID=fff1
    // PW0316�� Rx_CHARACTERISTIC_UUID=ffe9
    // PW-01�� Rx_CHARACTERISTIC_UUID=fff6
    public static final UUID RX_CHAR_UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    // DA14580��Tx_CHARACTERISTIC_UUID=fff4��notify��ʽ��
    // PW0316�� Tx_CHARACTERISTIC_UUID=ffe4
    // PW-01��Tx_CHARACTERISTIC_UUID=fff6
    public static final UUID TX_CHAR_UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");

    public void enableTXNotification() {

        if (mBluetoothGatt == null) {
            // showMessage("mBluetoothGatt null" + mBluetoothGatt);
            toastMessage("enableTXNotification:mBluetoothGatt null" + mBluetoothGatt);
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            // showMessage("Rx service not found!");
//			toastMessage("enableTXNotification:Rx service not found!");
            toastMessage("RX Service UUID ����");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            toastMessage("TX Charateristic UUID ����");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void writeRXCharacteristic(byte[] value) {
        if (mBluetoothGatt != null) {
            BluetoothGattService RxService = mBluetoothGatt.getService(TX_SERVICE_UUID);
            if (RxService == null) {
                toastMessage("RX Service UUID 错误！");
                broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
                return;
            }
            BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
            if (RxChar == null) {
                toastMessage("RX Charateristic UUID 错误！");
                broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
                return;
            }
            RxChar.setValue(value);
            boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
        }
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    private void toastMessage(String msg) {
        try {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            //解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Looper.loop();
        }
    }
}

