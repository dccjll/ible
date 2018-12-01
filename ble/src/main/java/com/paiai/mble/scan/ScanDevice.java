package com.paiai.mble.scan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.dcc.ibase.log.LogManager;
import com.paiai.mble.BLEManage;
import com.paiai.mble.BLEMsgCode;
import com.paiai.mble.core.BLEScan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:34<br>
 * 功能描述：扫描设备管理器<br>
 */
public class ScanDevice {

    private static final String TAG = "ScanDevice";
    private static boolean foundDevice = false;
    private BLEManage bleManage;
    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

    private boolean longScanFlag;
    private int type;
    private String deviceNameStartWithString;
    private BLEScan.OnBLEScanListener onBLEScanListener;

    /**
     * 配置长扫描标记<br/>
     * 可不配置，不配置时将默认扫描5s
     */
    public ScanDevice setLongScanFlag(boolean longScanFlag) {
        this.longScanFlag = longScanFlag;
        return this;
    }

    /**
     * 配置扫描设备的类型<br/>
     * 可配置为BluetoothDevice.DEVICE_TYPE_LE，则只扫描低功耗的蓝牙设备<br/>
     * 可不配置
     */
    public ScanDevice setType(int type) {
        this.type = type;
        return this;
    }

    /**
     * 配置扫描设备的名称开始匹配信息<br/>
     * 可不配置
     */
    public ScanDevice setDeviceNameStartWithString(String deviceNameStartWithString) {
        this.deviceNameStartWithString = deviceNameStartWithString;
        return this;
    }

    /**
     * 配置执行状态报告<br/>
     * 可不配置，不配置时将无法接收到执行状态报告
     */
    public ScanDevice setOnBLEScanListener(BLEScan.OnBLEScanListener onBLEScanListener) {
        this.onBLEScanListener = onBLEScanListener;
        return this;
    }

    @Override
    public String toString() {
        return "ScanDevice{" + "longScanFlag=" + longScanFlag + ", type=" + type + ", deviceNameStartWithString='" + deviceNameStartWithString + '\'' + ", onBLEScanListener=" + onBLEScanListener + '}';
    }

    /**
     * 任务开始——开始扫描
     */
    public void startScan() {
        LogManager.Companion.i(TAG, "接收到请求，开始扫描，" + this);
        scanDevice();
    }

    /**
     * 任务开始——停止扫描
     */
    public void stopScan() {
        LogManager.Companion.i(TAG, "接收到请求，停止扫描，" + this);
        if (bleManage == null) {
            LogManager.Companion.e(TAG, "bleManage == null");
            return;
        }
        bleManage.stopScan();
    }

    /**
     * 扫描设备
     */
    private void scanDevice() {
        bleManage = new BLEManage();
        if (longScanFlag) {
            bleManage.setLongScanFlag();
        }
        foundDevice = false;
        bluetoothDeviceList.clear();
        bleManage.setListenterObject(new BLEScan.OnBLEScanListener() {
            @Override
            public void onFoundDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                if (type > 0 && type != bluetoothDevice.getType()) {//有设备类型过滤
                    return;
                }
                if (!TextUtils.isEmpty(deviceNameStartWithString) && (TextUtils.isEmpty(bluetoothDevice.getName())  || !bluetoothDevice.getName().startsWith(deviceNameStartWithString))) {
                    return;
                }
                synchronized (TAG) {
                    foundDevice = true;
                }
                if (onBLEScanListener != null && !bluetoothDeviceList.contains(bluetoothDevice)) {
                    LogManager.Companion.i(TAG, "发现目标蓝牙设备," + "" + bluetoothDevice.getName() + ",mac=" + bluetoothDevice);
                    bluetoothDeviceList.add(bluetoothDevice);
                    onBLEScanListener.onFoundDevice(bluetoothDevice, rssi, scanRecord);
                }
            }

            @Override
            public void onScanFinish(List<Map<String, Object>> bluetoothDeviceList) {
                LogManager.Companion.i(TAG, "扫描结束");
                synchronized (TAG) {
                    if (!foundDevice) {
                        onScanFail(-10009);
                    }
                }
            }

            @Override
            public void onScanFail(int errorCode) {
                LogManager.Companion.i(TAG, BLEMsgCode.parseMessageCode(errorCode));
                if (onBLEScanListener != null) {
                    onBLEScanListener.onScanFail(errorCode);
                }
            }
        });
        bleManage.startScan();
    }
}
