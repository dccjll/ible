package com.paiai.mble.response;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.dcc.ibase.log.LogManager;
import com.paiai.mble.BLEConfig;
import com.paiai.mble.BLEGattCallback;
import com.paiai.mble.BLEManage;
import com.paiai.mble.BLEMsgCode;
import com.paiai.mble.core.BLEConnect;
import com.paiai.mble.core.BLEFindService;
import com.paiai.mble.core.BLEOpenNotification;
import com.paiai.mble.core.BLEScan;
import com.paiai.mble.core.BLEWriteData;

import java.util.List;
import java.util.Map;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:29<br>
 * 功能描述： <br>
 * 蓝牙核心响应管理器,统一处理底层蓝牙响应消息,并转发到上层<br>
 * 1.对于底层蓝牙断开的消息,判断蓝牙任务是否执行完毕,执行完毕,则直接显示断开日志,不进行后续处理;没有执行完毕,则通知上层任务执行失败<br>
 * 2.根据不同需求,对于蓝牙任务执行完毕需要立即关闭当前连接的,直接关闭<br>
 */
public class BLEResponseManager {

    private final static String TAG = BLEResponseManager.class.getSimpleName();
    private BLEManage bleManage;//连接管理对象

    public BLEManage getBleManage() {
        return bleManage;
    }

    public BLEResponseManager(BLEManage bleManage) {
        this.bleManage = bleManage;
    }

    /**
     * 找到设备
     */
    public void onFoundDevice(final BluetoothDevice bluetoothDevice, final int rssi, final byte[] scanRecord){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEScan.OnBLEScanListener) {
                    ((BLEScan.OnBLEScanListener)bleManage.getListenterObject()).onFoundDevice(bluetoothDevice, rssi, scanRecord);
                }
            }
        });
    }

    /**
     * 扫描完成
     */
    public void onScanFinish(final List<Map<String, Object>> bluetoothDeviceList){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEScan.OnBLEScanListener) {
                    ((BLEScan.OnBLEScanListener)bleManage.getListenterObject()).onScanFinish(bluetoothDeviceList);
                }
            }
        });
    }

    /**
     * 连接成功
     */
    public void onConnectSuccess(final BluetoothGatt bluetoothGatt, final int status, final int newState, final BLEGattCallback bleGattCallback){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEConnect.OnBLEConnectListener) {
                    setTaskFinishFlag(true);
                    ((BLEConnect.OnBLEConnectListener)bleManage.getListenterObject()).onConnectSuccess(bluetoothGatt, status, newState, bleGattCallback);
                }
            }
        });
    }

    /**
     * 找服务成功
     */
    public void onFindServiceSuccess(final BluetoothGatt bluetoothGatt, final int status, final List<BluetoothGattService> bluetoothGattServices, final BLEGattCallback bleGattCallback){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEFindService.OnBLEFindServiceListener) {
                    setTaskFinishFlag(true);
                    ((BLEFindService.OnBLEFindServiceListener)bleManage.getListenterObject()).onFindServiceSuccess(bluetoothGatt, status, bluetoothGattServices, bleGattCallback);
                }
            }
        });
    }

    /**
     * 打开通知成功
     */
    public void onOpenNotificationSuccess(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status, final BLEGattCallback bleGattCallback){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEOpenNotification.OnBLEOpenNotificationListener) {
                    setTaskFinishFlag(true);
                    ((BLEOpenNotification.OnBLEOpenNotificationListener)bleManage.getListenterObject()).onOpenNotificationSuccess(gatt, descriptor, status, bleGattCallback);
                }
            }
        });
    }

    /**
     * 写数据完成
     */
    public void onWriteDataFinish(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEWriteData.OnBLEWriteDataListener) {
                    setTaskFinishFlag(true);
                    ((BLEWriteData.OnBLEWriteDataListener)bleManage.getListenterObject()).onWriteDataFinish();
                }
            }
        });
    }

    /**
     * 单次写数据成功
     */
    public void onWriteDataSuccess(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status, final BLEGattCallback bleGattCallback){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (bleManage != null && bleManage.getListenterObject() instanceof BLEWriteData.OnBLEWriteDataListener) {
                    ((BLEWriteData.OnBLEWriteDataListener)bleManage.getListenterObject()).onWriteDataSuccess(gatt, characteristic, status, bleGattCallback);
                }
            }
        });
    }

    /**
     * 接收到数据
     */
    public void receiveData(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (bleManage != null && bleManage.getListenterObject() instanceof OnBLEResponse) {
            ((OnBLEResponse)bleManage.getListenterObject()).receiveData(gatt, characteristic);
        }
    }

    /**
     * 任务失败转发
     */
    public void onResponseError(final Object objectListener, final int errorCode){
        if (!bleManage.getRunning()) {
            LogManager.Companion.e(TAG, "onResponseError，任务已结束");
            return;
        }
        LogManager.Companion.e(TAG, "onResponseError\nobjectListener=" + objectListener + "\nerrorMsg=" + BLEMsgCode.parseMessageCode(errorCode));
        setTaskFinishFlag(false);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(objectListener instanceof OnBLEResponse){
                    ((OnBLEResponse)objectListener).onError(errorCode);
                }else if(objectListener instanceof BLEScan.OnBLEScanListener){
                    ((BLEScan.OnBLEScanListener)objectListener).onScanFail(errorCode);
                }else if(objectListener instanceof BLEConnect.OnBLEConnectListener){
                    ((BLEConnect.OnBLEConnectListener)objectListener).onConnectFail(errorCode);
                }else if(objectListener instanceof BLEFindService.OnBLEFindServiceListener){
                    ((BLEFindService.OnBLEFindServiceListener)objectListener).onFindServiceFail(errorCode);
                }else if(objectListener instanceof BLEOpenNotification.OnBLEOpenNotificationListener){
                    ((BLEOpenNotification.OnBLEOpenNotificationListener)objectListener).onOpenNotificationFail(errorCode);
                }else if(objectListener instanceof BLEWriteData.OnBLEWriteDataListener){
                    ((BLEWriteData.OnBLEWriteDataListener)objectListener).onWriteDataFail(errorCode);
                } else {
                    LogManager.Companion.e(TAG, "onResponseError,未知的监听器对象");
                }
            }
        });
    }

    /**
     * 设置任务完成标志
     * @param success   true    成功  false   失败
     */
    public void setTaskFinishFlag(boolean success){
        bleManage.setRunning(false);
        bleManage.removeTimeoutCallback();
        if (!success) {
            LogManager.Companion.i(TAG, "setTaskFinishFlag,任务失败,作一次断开连接");
            if (TextUtils.isEmpty(bleManage.getDeviceAttr())) {
                LogManager.Companion.e(TAG, "准备断开时，设备属性验证失败");
                return;
            }
            bleManage.disconnect();
            return;
        }
        LogManager.Companion.i(TAG, "setTaskFinishFlag,任务成功");
        if(bleManage.getDisconnectOnFinish()){//任务成功后立即断开蓝牙连接，可能导致对端设备接收不到数据，此处间隔2000ms
            new Thread(new Runnable() {
                @Override
                public void run() {
                    LogManager.Companion.i(TAG, "任务成功后立即断开蓝牙连接，可能导致对端设备接收不到数据，此处间隔" + BLEConfig.START_DISCONNECT_INTERVAL_WHEN_FINISH + "ms");
                    SystemClock.sleep(BLEConfig.START_DISCONNECT_INTERVAL_WHEN_FINISH);
                    if (TextUtils.isEmpty(bleManage.getDeviceAttr())) {
                        LogManager.Companion.e(TAG, "准备断开时，设备属性验证失败");
                        return;
                    }
                    bleManage.disconnect();
                }
            }).start();
        }
    }
}
