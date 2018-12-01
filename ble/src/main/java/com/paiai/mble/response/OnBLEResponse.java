package com.paiai.mble.response;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 11:31<br>
 * 功能描述：接收数据处理器<br>
 */
public abstract class OnBLEResponse {
    private BLEResponseManager bleResponseManager;
    public abstract  void receiveData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    public abstract  void onError(int errorCode);

    public BLEResponseManager getBleResponseManager() {
        return bleResponseManager;
    }

    public void setBleResponseManager(BLEResponseManager bleResponseManager) {
        this.bleResponseManager = bleResponseManager;
    }
}
