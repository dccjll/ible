package com.paiai.mble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.ByteUtils;
import com.paiai.mble.BLEGattCallback;
import com.paiai.mble.BLEManage;

import java.util.List;
import java.util.UUID;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:28<br>
 * 功能描述：打开通知<br>
 */
public class BLEOpenNotification {

    private final static String TAG = BLEOpenNotification.class.getSimpleName();
    private List<BluetoothGattService> bluetoothGattServices;
    private UUID[] uuids;
    private BLEManage bleManage;

    /**
     * gatt服务器打开通知监听器
     */
    public interface OnGattBLEOpenNotificationListener {
        void onOpenNotificationSuccess(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);
        void onOpenNotificationFail(String errorMsg, int loglevel);
    }

    /**
     * 打开通知
     */
    public BLEOpenNotification(BLEManage bleManage) {
        this.bluetoothGattServices = bleManage.getBluetoothGatt().getServices();
        this.uuids = bleManage.getNotificationuuids();
        this.bleManage = bleManage;
    }

    /**
     * 开始打开通知
     */
    public void openNotification(){
        LogManager.Companion.i(TAG, "准备开始打开通知");
        if(bleManage.getBluetoothGatt() == null){
            bleManage.handleError(-10021);
            return;
        }
        if(bluetoothGattServices == null || bluetoothGattServices.size() == 0){
            bleManage.handleError(-10023);
            return;
        }
        if(uuids == null || uuids.length != 3){
            bleManage.handleError(-10024);
            return;
        }
        if(bleManage.getBleGattCallback() == null){
            bleManage.handleError(-10022);
            return;
        }
        bleManage.getBleGattCallback().setUuidCharacteristicChange(uuids[1]);
        bleManage.getBleGattCallback().setUuidDescriptorWrite(uuids[2]);
        bleManage.getBleGattCallback().registerOnGattBLEOpenNotificationListener(
                new OnGattBLEOpenNotificationListener() {
                    @Override
                    public void onOpenNotificationSuccess(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        afterOpenNotificationSuccess(gatt, descriptor, status);
                    }

                    @Override
                    public void onOpenNotificationFail(String errorMsg, int loglevel) {
                        LogManager.Companion.i(TAG, "打开通知失败\nerrorMsg=" + errorMsg);
                        bleManage.getBleConnect().connect();
                    }
                }
        );
        LogManager.Companion.i(TAG, "开始打开通知");
        BluetoothGattService bluetoothGattService = null;
        for(BluetoothGattService bluetoothGattService_ : bluetoothGattServices){
            if(bluetoothGattService_.getUuid().toString().equalsIgnoreCase(uuids[0].toString())){
                bluetoothGattService = bluetoothGattService_;
                break;
            }
        }
        if(bluetoothGattService == null){
            bleManage.handleError(-10025);
            return;
        }
        final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(uuids[1]);
        if(bluetoothGattCharacteristic == null){
            bleManage.handleError(-10026);
            return;
        }
        LogManager.Companion.i(TAG, "通知的特征属性：" + bluetoothGattCharacteristic.getProperties() + ",need：" + BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            bleManage.handleError(-10027);
            return;
        }
        final BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(uuids[2]);
        if(bluetoothGattDescriptor == null){
            bleManage.handleError(-10028);
            return;
        }
        if (!bleManage.getRunning()) {
            LogManager.Companion.i(TAG, "任务已停止");
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    bleManage.handleError(-10029);
                    return;
                }
                if(!bleManage.getBluetoothGatt().setCharacteristicNotification(bluetoothGattCharacteristic, true)){
                    bleManage.handleError(-10030);
                    return;
                }
                if(!bleManage.getBluetoothGatt().writeDescriptor(bluetoothGattDescriptor)){
                    bleManage.handleError(-10031);
                }
            }
        });
    }

    private void afterOpenNotificationSuccess(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        LogManager.Companion.i(TAG, "打开通知成功");
        if(bleManage.getListenterObject() instanceof OnBLEOpenNotificationListener){
            ((OnBLEOpenNotificationListener)bleManage.getListenterObject()).onOpenNotificationSuccess(gatt, descriptor, status, bleManage.getBleGattCallback());
            return;
        }
        LogManager.Companion.i(TAG, "要写入的数据：" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(bleManage.getData()));
        if (!bleManage.getRunning()) {
            LogManager.Companion.e(TAG, "准备写入数据时任务已停止");
            return;
        }
        BLEWriteData bleWriteData = new BLEWriteData(bleManage);
        bleWriteData.writeData();
    }

    /**
     * 作者：dccjll<br>
     * 创建时间：2017/11/6 11:31<br>
     * 功能描述：打开通知监听器<br>
     */
    public interface OnBLEOpenNotificationListener {
        void onOpenNotificationSuccess(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, BLEGattCallback bleGattCallback);
        void onOpenNotificationFail(int errorCode);
    }
}
