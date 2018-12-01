package com.paiai.mble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.ByteUtils;
import com.paiai.mble.core.BLEConnect;
import com.paiai.mble.core.BLEFindService;
import com.paiai.mble.core.BLEOpenNotification;
import com.paiai.mble.core.BLEWriteData;
import com.paiai.mble.response.BLEResponseManager;
import com.paiai.mble.response.OnBLEResponse;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:26<br>
 * 功能描述：底层蓝牙回调状态管理器<br>
 */
public class BLEGattCallback extends BluetoothGattCallback {

    private final static String TAG = BLEGattCallback.class.getSimpleName();
    public static BluetoothGatt lastBluetoothGatt;
    private BLEConnect.OnGattBLEConnectListener onGattBLEConnectListener;
    private BLEFindService.OnGattBLEFindServiceListener onGattBLEFindServiceListener;
    private BLEOpenNotification.OnGattBLEOpenNotificationListener onGattBLEOpenNotificationListener;
    private BLEWriteData.OnGattBLEWriteDataListener onGattBLEWriteDataListener;
    private OnBLEResponse onBLEResponse;
    private BLEResponseManager bleResponseManager;
    private UUID uuidCharacteristicWrite;
    private UUID uuidCharacteristicChange;
    private UUID uuidDescriptorWrite;

    public void registerOnGattConnectListener(BLEConnect.OnGattBLEConnectListener onGattBLEConnectListener) {
        this.onGattBLEConnectListener = onGattBLEConnectListener;
        this.onGattBLEFindServiceListener = null;
        this.onGattBLEOpenNotificationListener = null;
        this.onGattBLEWriteDataListener = null;
    }

    public void registerOnGattBLEFindServiceListener(BLEFindService.OnGattBLEFindServiceListener onGattBLEFindServiceListener) {
        this.onGattBLEConnectListener = null;
        this.onGattBLEFindServiceListener = onGattBLEFindServiceListener;
        this.onGattBLEOpenNotificationListener = null;
        this.onGattBLEWriteDataListener = null;
    }

    public void registerOnGattBLEOpenNotificationListener(BLEOpenNotification.OnGattBLEOpenNotificationListener onGattBLEOpenNotificationListener) {
        this.onGattBLEConnectListener = null;
        this.onGattBLEFindServiceListener = null;
        this.onGattBLEOpenNotificationListener = onGattBLEOpenNotificationListener;
        this.onGattBLEWriteDataListener = null;
    }

    public void registerOnGattBLEWriteDataListener(BLEWriteData.OnGattBLEWriteDataListener onGattBLEWriteDataListener) {
        this.onGattBLEConnectListener = null;
        this.onGattBLEFindServiceListener = null;
        this.onGattBLEOpenNotificationListener = null;
        this.onGattBLEWriteDataListener = onGattBLEWriteDataListener;
    }

    public void registerOnBLEResponse(OnBLEResponse onBLEResponse) {
        this.onBLEResponse = onBLEResponse;
    }

    public void setBLEResponseManager(BLEResponseManager bleResponseManager) {
        this.bleResponseManager = bleResponseManager;
    }

    public void setUuidCharacteristicWrite(UUID uuidCharacteristicWrite) {
        this.uuidCharacteristicWrite = uuidCharacteristicWrite;
    }

    public void setUuidCharacteristicChange(UUID uuidCharacteristicChange) {
        this.uuidCharacteristicChange = uuidCharacteristicChange;
    }

    public void setUuidDescriptorWrite(UUID uuidDescriptorWrite) {
        this.uuidDescriptorWrite = uuidDescriptorWrite;
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        lastBluetoothGatt = gatt;
        LogManager.Companion.i(TAG, "onConnectionStateChange,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
        if(status == BluetoothGatt.GATT_SUCCESS){
            if(newState == BluetoothProfile.STATE_CONNECTING){
                LogManager.Companion.i(TAG, "正在连接,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
            }else if(newState == BluetoothProfile.STATE_CONNECTED){
                LogManager.Companion.i(TAG, "已连接,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
                if(onGattBLEConnectListener != null){
                    onGattBLEConnectListener.onConnectSuccss(gatt, status, newState, this);
                }
            }else if(newState == BluetoothProfile.STATE_DISCONNECTING){
                LogManager.Companion.i(TAG, "正在断开,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                LogManager.Companion.i(TAG, "已断开,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
                handleError(BLEMsgCode.parseMessageCode(-10007), Log.INFO, gatt);
            }
        }else{
            LogManager.Companion.e(TAG, "收到蓝牙底层协议栈异常消息,gatt=" + gatt + ",status=" + status + ",newState=" + newState);
            handleError(BLEMsgCode.parseMessageCode(-10008), Log.ERROR, gatt);
        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
        LogManager.Companion.i(TAG, "onServicesDiscovered,gatt=" + gatt + ",status=" + status);
        if(bleResponseManager.getBleManage().getRunning() && status == BluetoothGatt.GATT_SUCCESS){
            if(onGattBLEFindServiceListener != null){
                onGattBLEFindServiceListener.onFindServiceSuccess(gatt, status, gatt.getServices());
            }
        }
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!bleResponseManager.getBleManage().getRunning()) {
            return;
        }
        super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        LogManager.Companion.i(TAG, "onCharacteristicWrite\ngatt=" + gatt + "\ncharacteristic=" + characteristic + "\nstatus=" + status + "\nwritedData=" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(characteristic.getValue())
            + "\ncharacteristic.getUuid().toString()=" + characteristic.getUuid().toString() + "\nuuidCharacteristicWrite.toString()=" + uuidCharacteristicWrite.toString() + "\nonGattBLEWriteDataListener=" + onGattBLEWriteDataListener);
        BLEManage.updateBluetoothGattLastCommunicationTime(gatt, System.currentTimeMillis());
        if(bleResponseManager.getBleManage().getRunning() && status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().toString().equalsIgnoreCase(uuidCharacteristicWrite.toString())){
            if(onGattBLEWriteDataListener != null){
                onGattBLEWriteDataListener.onWriteDataSuccess(gatt, characteristic, status);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        /*
          反射修改gatt繁忙状态，请谨慎修改
         */
        try {
            Field mDeviceBusyField = BluetoothGatt.class.getDeclaredField("mDeviceBusy");
            mDeviceBusyField.setAccessible(true);
            mDeviceBusyField.set(gatt, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogManager.Companion.i(TAG, "onCharacteristicChanged\ngatt=" + gatt + "\ncharacteristic=" + characteristic + "\nreceivedData=" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(characteristic.getValue())
                + "\ncharacteristic.getUuid().toString()=" + characteristic.getUuid().toString() + "\nuuidCharacteristicWrite.toString()=" + uuidCharacteristicChange.toString()
                + "\ncurrentThread=" + Thread.currentThread());
        BLEManage.updateBluetoothGattLastCommunicationTime(gatt, System.currentTimeMillis());
        if(bleResponseManager.getBleManage().getRunning() && characteristic.getUuid().toString().equalsIgnoreCase(uuidCharacteristicChange.toString())){//接收到数据
            if(onBLEResponse == null){
                LogManager.Companion.e(TAG, "received data, but onBLEResponse is null");
                return;
            }
            bleResponseManager.receiveData(gatt, characteristic);
        }
    }

    @Override
    public void onDescriptorRead(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (!bleResponseManager.getBleManage().getRunning()) {
            return;
        }
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        LogManager.Companion.i(TAG, "onDescriptorWrite\ngatt=" + gatt + "\ndescriptor.getUuid().toString()=" + descriptor.getUuid().toString() + "\nuuidDescriptorWrite.toString()" + uuidDescriptorWrite.toString() + "\nstatus=" + status);
        if(bleResponseManager.getBleManage().getRunning() && status == BluetoothGatt.GATT_SUCCESS && descriptor.getUuid().toString().equalsIgnoreCase(uuidDescriptorWrite.toString())){
            if(onGattBLEOpenNotificationListener != null){
                onGattBLEOpenNotificationListener.onOpenNotificationSuccess(gatt, descriptor, status);
            }
        }
    }

    @Override
    public void onReliableWriteCompleted(final BluetoothGatt gatt, int status) {
        if (!bleResponseManager.getBleManage().getRunning()) {
            return;
        }
        super.onReliableWriteCompleted(gatt, status);
    }

    @Override
    public void onReadRemoteRssi(final BluetoothGatt gatt, int rssi, int status) {
        if (!bleResponseManager.getBleManage().getRunning()) {
            return;
        }
        super.onReadRemoteRssi(gatt, rssi, status);
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, int mtu, int status) {
        if (!bleResponseManager.getBleManage().getRunning()) {
            return;
        }
        super.onMtuChanged(gatt, mtu, status);
    }

    /**
     * 统一失败处理
     */
    private synchronized void handleError(String errorMsg, int loglevel, BluetoothGatt gatt) {
        boolean running = bleResponseManager.getBleManage().getRunning();
        LogManager.Companion.e(TAG, "handleError,running=" + running);
        BLEManage.disconnect(gatt);
        if(running){
            onResponseError(errorMsg, loglevel, gatt);
        }
    }

    /**
     * 响应失败回复
     */
    private void onResponseError(String errorMsg, int loglevel, BluetoothGatt gatt){
        if(onGattBLEWriteDataListener != null){
            onGattBLEWriteDataListener.onWriteDataFail(errorMsg, loglevel);
        } else if(onGattBLEOpenNotificationListener != null){
            onGattBLEOpenNotificationListener.onOpenNotificationFail(errorMsg, loglevel);
        } else if(onGattBLEFindServiceListener != null){
            onGattBLEFindServiceListener.onFindServiceFail(errorMsg, loglevel);
        } else if(onGattBLEConnectListener != null){
            onGattBLEConnectListener.onConnectFail(errorMsg, loglevel, gatt);
        } else {
            LogManager.Companion.e(TAG, "onResponseError, not found listener");
        }
    }
}
