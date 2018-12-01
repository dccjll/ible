package com.paiai.mble.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.AppUtils;
import com.dcc.ibase.utils.RegexUtils;
import com.paiai.mble.BLEConfig;
import com.paiai.mble.BLEGattCallback;
import com.paiai.mble.BLEManage;
import com.paiai.mble.response.OnBLEResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:27<br>
 * 功能描述：连接设备<br>
 */
public class BLEConnect {

    private final static String TAG = BLEConnect.class.getSimpleName();
    private int currentConnectCount = 0;//当前连接次数
    private BluetoothDevice bluetoothDevice;//需要连接的蓝牙设备
    private String targetMacAddress;//远程蓝牙设备的mac地址
    private BLEManage bleManage;

    /**
     * 连接蓝牙服务器回调接口
     */
    public interface OnGattBLEConnectListener {
        void onConnectSuccss(BluetoothGatt bluetoothGatt, int status, int newState, BLEGattCallback bleGattCallback);
        void onConnectFail(String errorMsg, int loglevel, BluetoothGatt bluetoothGatt);
    }

    /**
     * 指定设备对象连接设备
     */
    public BLEConnect(BluetoothDevice bluetoothDevice, BLEManage bleManage) {
        this(bleManage);
        this.bluetoothDevice = bluetoothDevice;
    }

    /**
     * 指定设备MAC地址连接设备
     */
    public BLEConnect(String targetMacAddress, BLEManage bleManage) {
        this(bleManage);
        this.targetMacAddress = targetMacAddress;
    }

    /**
     * 私有构造器
     */
    private BLEConnect(BLEManage bleManage) {
        currentConnectCount = 0;
        this.bleManage = bleManage;
        if (bleManage.getBleGattCallback() == null) {
            bleManage.setBleGattCallback(new BLEGattCallback());
            bleManage.getBleGattCallback().setBLEResponseManager(bleManage.getBleResponseManager());
            if (bleManage.getListenterObject() instanceof OnBLEResponse) {
                bleManage.getBleGattCallback().registerOnBLEResponse((OnBLEResponse) bleManage.getListenterObject());
            }
        }
    }

    /**
     * 连接设备
     */
    public void connect(){
        LogManager.Companion.i(TAG, "准备开始连接");
        LogManager.Companion.i(TAG, "bluetoothDevice=" + bluetoothDevice + "\ntargetMacAddress=" + targetMacAddress);
        BluetoothManager bluetoothManager = bleManage.getBluetoothManager();
        if (bluetoothManager == null) {
            bleManage.handleError(-10001);
            return;
        }
        BluetoothAdapter bluetoothAdapter = bleManage.getBluetoothAdapter();
        if(bluetoothAdapter == null){
            bleManage.handleError(-10002);
            return;
        }
        if (!AppUtils.Companion.getApp().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleManage.handleError(-10050);
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            bleManage.handleError(-10015);
            return;
        }
        if(bluetoothDevice == null && !RegexUtils.INSTANCE.checkAddress(targetMacAddress)){
            bleManage.handleError(-10019);
            return;
        }
        if(++currentConnectCount > BLEConfig.MAX_CONNECT_COUNT){
            bleManage.handleError(-10018);
            return;
        }
        List<BluetoothDevice> bluetoothDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
            LogManager.Companion.i(TAG, "有连接的设备列表=" + bluetoothDevices);
            if (bluetoothDevices.size() >= BLEConfig.MaxConnectDeviceNum) {
                bleManage.handleError(-10020);
                return;
            }
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                if (BLEConnect.this.bluetoothDevice == bluetoothDevice) {
                    LogManager.Companion.i(TAG, "根据设备对象匹配到已连接的设备,device=" + bluetoothDevice);
                    onConnectDeivceSuccss(BLEManage.getBluetoothGatt(bluetoothDevice.getAddress()), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
                    return;
                }
                if (bluetoothDevice.getAddress().equalsIgnoreCase(bleManage.getTargetDeviceAddress())) {
                    LogManager.Companion.i(TAG, "根据mac地址匹配到已连接的设备,mac=" + bluetoothDevice.getAddress());
                    onConnectDeivceSuccss(BLEManage.getBluetoothGatt(bleManage.getTargetDeviceAddress()), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
                    return;
                }
            }
        }
        bleManage.getBleGattCallback().registerOnGattConnectListener(
                new OnGattBLEConnectListener() {
                    @Override
                    public void onConnectSuccss(BluetoothGatt bluetoothGatt, int status, int newState, BLEGattCallback bleGattCallback) {
                        onConnectDeivceSuccss(bluetoothGatt, status, newState);
                    }

                    @Override
                    public void onConnectFail(String errorMsg, int loglevel, BluetoothGatt bluetoothGatt) {
                        LogManager.Companion.i(TAG, "第" + currentConnectCount + "次连接失败\nerrorMsg=" + errorMsg);
                        connect();
                    }
                }
        );
        if (!bleManage.getRunning()) {
            LogManager.Companion.i(TAG, "任务已停止");
            return;
        }
        LogManager.Companion.i(TAG, "第" + currentConnectCount + "次开始连接");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bluetoothDevice == null) {
                    try {
                        //如果木雕地址不是一个可用的mac地址，底层会报is not a valid Bluetooth address
                        bluetoothDevice = bleManage.getBluetoothAdapter().getRemoteDevice(targetMacAddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                        bleManage.handleError(-10022);
                        return;
                    }
                }
                BluetoothGatt bluetoothGatt = bluetoothDevice.connectGatt(AppUtils.Companion.getApp(), false, bleManage.getBleGattCallback());
                if (bluetoothGatt == null) {
                    LogManager.Companion.e(TAG, "连接失败");
                    connect();
                    return;
                }
                LogManager.Companion.i(TAG, "请求连接成功,gatt=" + bluetoothGatt);
            }
        });
    }

    /**
     * 连接成功
     */
    private void onConnectDeivceSuccss(BluetoothGatt bluetoothGatt, int status, int newState) {
        if (bluetoothGatt == null) {
            LogManager.Companion.e(TAG, "在连接成功的回调里，bluetoothGatt对象验证失败，bluetoothGatt=null");
            bleManage.handleError(-10051);
            return;
        }
        LogManager.Companion.i(TAG, "已连接成功");
        bleManage.setBluetoothGatt(bluetoothGatt);
        if (BLEManage.getBluetoothGatt(bluetoothGatt.getDevice().getAddress()) == null && BLEManage.getBluetoothGatt(bluetoothGatt.getDevice().getName()) == null) {
            Map<String, Object> bluetoothGattMap = new HashMap<>();
            bluetoothGattMap.put("bluetoothGatt", bluetoothGatt);
            bluetoothGattMap.put("connectedTime", System.currentTimeMillis());
            bluetoothGattMap.put("bluetoothGattCallback", bleManage.getBleGattCallback());
            BLEManage.connectedBluetoothGattList.add(bluetoothGattMap);
        }
        if (bleManage.getListenterObject() instanceof OnBLEConnectListener) {
            ((OnBLEConnectListener)bleManage.getListenterObject()).onConnectSuccess(bluetoothGatt, status, newState, bleManage.getBleGattCallback());
            return;
        }
        if (BLEConfig.START_FIND_SERVICE_INTERVAL > 0) {
            LogManager.Companion.i(TAG, "休眠" + BLEConfig.START_FIND_SERVICE_INTERVAL + "ms开始找服务，手机型号:" + Build.MODEL);
            SystemClock.sleep(BLEConfig.START_FIND_SERVICE_INTERVAL);
        }
        if (!bleManage.getRunning()) {
            LogManager.Companion.e(TAG, "准备找服务时任务已停止");
            return;
        }
        bleManage.setBleConnect(this);
        BLEFindService bleFindService = new BLEFindService(bleManage);
        bleFindService.findService();
    }

    /**
     * 作者：dccjll<br>
     * 创建时间：2017/11/6 11:30<br>
     * 功能描述：连接监听器<br>
     */
    public interface OnBLEConnectListener {
        void onConnectSuccess(BluetoothGatt bluetoothGatt, int status, int newState, BLEGattCallback bleGattCallback);
        void onConnectFail(int errorCode);
    }
}
