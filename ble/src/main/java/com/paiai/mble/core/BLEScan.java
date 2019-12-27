package com.paiai.mble.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.AppUtils;
import com.paiai.mble.BLEConfig;
import com.paiai.mble.BLEGattCallback;
import com.paiai.mble.BLEManage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:33<br>
 * 功能描述：蓝牙扫描<br>
 */
@SuppressWarnings("ALL")
public class BLEScan {

    private BLEManage bleManage;
    private BLEConnect bleConnect;

    private int currentScanCount = 0;//当前扫描次数
    private final static String TAG = BLEScan.class.getSimpleName();
    private final List<Map<String, Object>> foundDeviceList = new ArrayList<>();
    private Boolean isScaning = false;
    private Handler scanHandler;
    private Runnable scanRunnable;
    private boolean multiScan = true;
    private boolean foundDevice = false;//是否已找到设备
    private boolean enableBleLog = false;
    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!bleManage.getRunning()) {
                if (enableBleLog) {
                    LogManager.Companion.i(TAG, "扫描到设备时任务已停止");
                }
                return;
            }
            if (enableBleLog) {
                LogManager.Companion.i(TAG, "扫描到设备,deviceMac=" + device + ",deviceName=" + device.getName());
            }
            if(foundDevice){
                if (enableBleLog) {
                    LogManager.Companion.i(TAG, "已找到设备，略过停止扫描间隙扫描到的设备");
                }
                return;
            }
            boolean containsDevice = false;
            for (Map<String, Object> bluetoothDeviceMap : BLEManage.bluetoothDeviceList) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) bluetoothDeviceMap.get("bluetoothDevice");
                if (bluetoothDevice.equals(device)) {
                    containsDevice = true;
                    break;
                }
            }
            if (!containsDevice) {
                Map<String, Object> bluetoothDeviceMap = new HashMap<>();
                bluetoothDeviceMap.put("foundTime", System.currentTimeMillis());
                bluetoothDeviceMap.put("bluetoothDevice", device);
                BLEManage.bluetoothDeviceList.add(bluetoothDeviceMap);
            }
            synchronized (TAG) {
                Map<String, Object> existDevice = null;
                for(Map<String, Object> entry : foundDeviceList){
                    if(((BluetoothDevice)entry.get("device")).getAddress().equalsIgnoreCase(device.getAddress())){
                        existDevice = entry;
                        break;
                    }
                }
                if(existDevice == null){//已扫描到的列表中没有这个设备，新加
                    Map<String, Object> deviceAttrMap = new HashMap<>();
                    deviceAttrMap.put("device", device);
                    deviceAttrMap.put("rssi", rssi);
                    deviceAttrMap.put("scanRecord", scanRecord);
                    foundDeviceList.add(deviceAttrMap);
                } else {//已扫描到的列表中有这个设备，更新
                    existDevice.put("device", device);
                    existDevice.put("rssi", rssi);
                    existDevice.put("scanRecord", scanRecord);
                }
                if (bleManage.getBleFilter() == null) {//没有过滤器，认为是单纯的扫描任务
                    onFoundDevice(false, device, rssi, scanRecord);
                    return;
                }
                //有过滤器
                if (bleManage.getBleFilter().matcher(device, rssi, scanRecord)) {
                    onFoundDevice(true, device, rssi, scanRecord);
                }
            }
        }
    };

    public BLEScan(BLEManage bleManage) {
        this.bleManage = bleManage;
        enableBleLog = bleManage.getEnableLogFlag();
    }

    public BLEScan setMultiScan(boolean multiScan) {
        this.multiScan = multiScan;
        return this;
    }

    /**
     * 扫描设备
     */
    public void startScan(){
        if (!bleManage.getRunning()) {
            if (enableBleLog) {
                LogManager.Companion.i(TAG, "准备开始扫描时任务已停止");
            }
            return;
        }
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "准备开始扫描");
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
        if (++currentScanCount > BLEConfig.MAX_SCAN_COUNT) {
            if (bleManage.getBleFilter() == null) {//执行是单纯的扫描任务
                if (enableBleLog) {
                    LogManager.Companion.i(TAG, "扫描完成");
                }
                bleManage.getBleResponseManager().onScanFinish(foundDeviceList);
                return;
            }
            if (enableBleLog) {
                LogManager.Companion.i(TAG, "扫描失败,已尝试最大扫描次数");
            }
            bleManage.handleError(-10009);
            return;
        }
        if (!BLEConfig.LONG_SCAN_FALG) {//长扫描标记会一直扫描
            scanHandler = new Handler(AppUtils.Companion.getApp().getMainLooper());
            scanRunnable = new Runnable() {
                @Override
                public void run() {
                    stopScan();
                    if (!bleManage.getRunning()) {
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "准备重新扫描时任务已停止");
                        }
                        return;
                    }
                    if (multiScan) {
                        startScan();//重新开始扫描
                    } else {
                        bleManage.getBleResponseManager().onScanFinish(foundDeviceList);
                    }
                }
            };
            scanHandler.postDelayed(scanRunnable, bleManage.getTimeoutScanBLE());
        }
        foundDeviceList.clear();
        BLEManage.disconnect(BLEGattCallback.lastBluetoothGatt);
        if (!bleManage.getRunning()) {
            if (enableBleLog) {
                LogManager.Companion.i(TAG, "准备扫描时任务已停止");
            }
            return;
        }
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "第" + currentScanCount + "次开始扫描");
        }
        try {
            scanControl(true);
        } catch (Exception e) {
            e.printStackTrace();
            bleManage.handleError(-10012);
        }
    }

    /**
     * 停止扫描
     */
    public void stopScan() {
        scanControl(false);
    }

    /**
     * 扫描控制,scanFlag=true 开始扫描 scanFlag=false 停止扫描
     */
    private void scanControl(Boolean scanFlag){
        if(scanFlag){
            if(isScaning){
                bleManage.handleError(-10017);
                return;
            }
            isScaning = true;
            foundDevice = false;
            bleManage.getBluetoothAdapter().startLeScan(leScanCallback);
        }else{
            bleManage.getBluetoothAdapter().stopLeScan(leScanCallback);
            if (scanHandler != null) {
                scanHandler.removeCallbacks(scanRunnable);
            }
            isScaning = false;
        }
    }

    /**
     * 发现设备
     */
    private void onFoundDevice(boolean existFilter, BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "已找到设备,mac=" + device);
        }
        if (existFilter) {
            foundDevice = true;
            stopScan();
            bleManage.setTargetDeviceAddress(device.getAddress());
            bleManage.setTargetDeviceName(device.getName());
        }
        if (bleManage.getListenterObject() instanceof OnBLEScanListener) {
            bleManage.getBleResponseManager().onFoundDevice(device, rssi, scanRecord);
            return;
        }
        if (!bleManage.getRunning()) {
            if (enableBleLog) {
                LogManager.Companion.e(TAG, "准备连接时任务已停止");
            }
            return;
        }
        bleConnect = new BLEConnect(device, bleManage);
        bleConnect.connect();
    }

    /**
     * 作者：dccjll<br>
     * 创建时间：2017/11/6 11:31<br>
     * 功能描述：扫描监听器<br>
     */
    public interface OnBLEScanListener {
        void onFoundDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord);
        void onScanFinish(List<Map<String, Object>> bluetoothDeviceList);
        void onScanFail(int errorCode);
    }

    /**
     * BLE过滤器
     */
    public interface BLEFilter {
        /**
         * 设备匹配规则
         * @param bluetoothDevice 扫描到的蓝牙设备
         * @param rssi 设备信号
         * @param scanRecord    设备广播包
         * @return
         */
        boolean matcher(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord);
    }
}
