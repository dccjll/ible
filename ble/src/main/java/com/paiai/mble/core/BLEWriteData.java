package com.paiai.mble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.SystemClock;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.ByteUtils;
import com.paiai.mble.BLEConfig;
import com.paiai.mble.BLEGattCallback;
import com.paiai.mble.BLEManage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:33<br>
 * 功能描述：写数据<br>
 */
public class BLEWriteData {

    private final static String TAG = BLEWriteData.class.getSimpleName();
    private boolean dataWrittenStart = false;//是否已开始写数据标记(发现锁在有的时候还没有开始写数据就回了上一条写数据失败的数据，加此标记以便于在接收数据时过滤)
    private boolean dataWrittenFinish = false;
    private boolean dataWrittenState = true;
    public UUID[] uuids;
    public byte[] data;
    private List<byte[]> dataList = new ArrayList<>();
    private Integer writtenDataLength;
    private BLEManage bleManage;
    private Integer index = 0;//当前发送的第几个数据包
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private boolean dividePackage = true;//是否考虑分包，如果分，按20字节或约定的分，如果不分，当前数据一次发送
    private int maxBytesCount = BLEConfig.MAX_BYTES;
    private boolean enableBleLog = true;


    public void setMaxBytesCount(int maxBytesCount) {
        if (maxBytesCount > 0) {
            this.maxBytesCount = maxBytesCount;
        }
        enableBleLog = bleManage.getEnableLogFlag();
    }

    public int getMaxBytesCount() {
        return maxBytesCount;
    }

    public BLEWriteData setDividePackage(boolean dividePackage) {
        this.dividePackage = dividePackage;
        return this;
    }

    public boolean getDividePackage() {
        return dividePackage;
    }

    public boolean getDataWrittenStart() {
        return dataWrittenStart;
    }

    public boolean isDataWrittenFinish() {
        return dataWrittenFinish;
    }

    public boolean isDataWrittenState() {
        return dataWrittenState;
    }

    public interface OnGattBLEWriteDataListener {
        void onWriteDataFinish();

        void onWriteDataSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

        void onWriteDataFail(String errorMsg, int loglevel);
    }

    public BLEWriteData(){}

    /**
     * 写数据构造器
     */
    public BLEWriteData(BLEManage bleManage) {
        this.uuids = bleManage.getWriteuuids();
        this.data = bleManage.getData();
        this.bleManage = bleManage;
        this.bleManage.setBleWriteData(this);
    }

    public void writeData() {
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "准备开始写数据");
        }
        dataWrittenFinish = false;
        dataWrittenState = true;
        if (bleManage.getBluetoothGatt() == null) {
            dataWrittenState = false;
            bleManage.handleError(-10021);
            return;
        }
        if (uuids == null || uuids.length != 2) {
            dataWrittenState = false;
            bleManage.handleError(-10032);
            return;
        }
        if ((data == null || data.length == 0) && (bleManage.getDataList() == null || bleManage.getDataList().size() == 0)) {
            dataWrittenState = false;
            bleManage.handleError(-10033);
            return;
        }
        if (dividePackage) {
            dataList = ByteUtils.INSTANCE.parseBytesToBytesListByLength(data, maxBytesCount);
        } else {
            dataList.add(data);
        }
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "要写入的总数据遍历,total data=" + ByteUtils.INSTANCE.parseBytesToHexString(data, true, " "));
            for(int i=0;dataList != null && i<dataList.size();i++) {
                LogManager.Companion.i(TAG, "i=" + i + ",data=" + ByteUtils.INSTANCE.parseBytesToHexString(dataList.get(i), true, " "));
            }
        }
        if (dataList == null) {
            dataWrittenState = false;
            bleManage.handleError(-10034);
            return;
        }
        if (bleManage.getBleGattCallback() == null) {
            dataWrittenState = false;
            bleManage.handleError(-10022);
            return;
        }
        writtenDataLength = 0;
        bleManage.getBleGattCallback().setUuidCharacteristicWrite(uuids[1]);
        bleManage.getBleGattCallback().registerOnGattBLEWriteDataListener(
                new OnGattBLEWriteDataListener() {
                    @Override
                    public void onWriteDataFinish() {
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "写入的所有数据为：" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(bleManage.getData()));
                        }
                        dataWrittenFinish = true;
                        bleManage.getBleResponseManager().onWriteDataFinish();
                    }

                    @Override
                    public void onWriteDataSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "数据写入成功，写入的数据为:\t\t\t\t" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(characteristic.getValue()) + "\n当前第" + (index + 1) + "包" + "\n总共" + dataList.size() + "包");
                        }
                        bleManage.getBleResponseManager().onWriteDataSuccess(gatt, characteristic, status, bleManage.getBleGattCallback());
                        if ((writtenDataLength += characteristic.getValue().length) == data.length) {
                            if (enableBleLog) {
                                LogManager.Companion.i(TAG, "数据写入完成");
                            }
                            onWriteDataFinish();
                            return;
                        }
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "间隔" + BLEConfig.SEND_NEXT_PACKAGE_INTERVAL + "ms再发送下一个数据包");
                        }
                        SystemClock.sleep(BLEConfig.SEND_NEXT_PACKAGE_INTERVAL);
                        int i = ++index;
                        if(i >= dataList.size()){
                            if (enableBleLog) {
                                LogManager.Companion.e(TAG,"写数据时下标越界(出现在底层写成功回调异常情况下，请忽略)");
                            }
                            return;
                        }
                        sendBLEData(dataList.get(i));
                    }

                    @Override
                    public void onWriteDataFail(String errorMsg, int loglevel) {
                        if (enableBleLog) {
                            LogManager.Companion.e(TAG, "onWriteDataFail\nerrorMsg=" + errorMsg + "\nloglevel=" + loglevel + "\nbleManage=" + bleManage);
                        }
                        dataWrittenState = false;
                        if (bleManage.getBleConnect() != null) {
                            bleManage.getBleConnect().connect();
                            return;
                        }
                        bleManage.handleError(-10039);
                    }
                }
        );
        BluetoothGattService bluetoothGattService = bleManage.getBluetoothGatt().getService(uuids[0]);
        if (bluetoothGattService == null) {
            dataWrittenState = false;
            bleManage.handleError(-10035);
            return;
        }
        bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(uuids[1]);
        if (bluetoothGattCharacteristic == null) {
            dataWrittenState = false;
            bleManage.handleError(-10036);
            return;
        }
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "写数据的特征属性：" + bluetoothGattCharacteristic.getProperties() + ",need：" + BluetoothGattCharacteristic.PROPERTY_WRITE);
        }
        if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
            dataWrittenState = false;
            bleManage.handleError(-10037);
            return;
        }
        if (!bleManage.getRunning()) {
            if (enableBleLog) {
                LogManager.Companion.i(TAG, "任务已停止");
            }
            return;
        }
        if (enableBleLog) {
            LogManager.Companion.i(TAG, "开始写数据");
        }
        dataWrittenStart = true;
        sendBLEData(dataList.get(index));
    }

    /**
     * 写数据
     *
     * @param value 写入的数据
     */
    private void sendBLEData(byte[] value) {
        if (!bluetoothGattCharacteristic.setValue(value)) {
            dataWrittenState = false;
            bleManage.handleError(-10038);
            return;
        }
        if (!bleManage.getBluetoothGatt().writeCharacteristic(bluetoothGattCharacteristic)) {
            dataWrittenState = false;
            bleManage.handleError(-10039);
        }
    }

    public void sendBLEData() {
        sendBLEData(data);
    }

    /**
     * 作者：dccjll<br>
     * 创建时间：2017/11/6 13:26<br>
     * 功能描述：写数据监听器<br>
     */
    public interface OnBLEWriteDataListener {
        void onWriteDataFinish();
        void onWriteDataSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status, BLEGattCallback bleGattCallback);
        void onWriteDataFail(int errorCode);
    }

    public BLEWriteData buildBLEWriteData(BLEManage bleManage) {
        this.uuids = bleManage.getWriteuuids();
        this.data = bleManage.getData();
        this.bleManage = bleManage;
        this.bleManage.setBleWriteData(this);
        this.enableBleLog = bleManage.getEnableLogFlag();
        bleManage.getBleGattCallback().setUuidCharacteristicWrite(uuids[1]);
        bleManage.getBleGattCallback().registerOnGattBLEWriteDataListener(
                new OnGattBLEWriteDataListener() {
                    @Override
                    public void onWriteDataFinish() {
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "写入的所有数据为：" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(bleManage.getData()));
                        }
                        bleManage.getBleResponseManager().onWriteDataFinish();
                    }

                    @Override
                    public void onWriteDataSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (enableBleLog) {
                            LogManager.Companion.i(TAG, "数据写入成功，写入的数据为:\t\t\t\t" + ByteUtils.INSTANCE.parseBytesToHexStringDefault(characteristic.getValue()));
                        }
                        bleManage.getBleResponseManager().onWriteDataSuccess(gatt, characteristic, status, bleManage.getBleGattCallback());
                    }

                    @Override
                    public void onWriteDataFail(String errorMsg, int loglevel) {
                        if (enableBleLog) {
                            LogManager.Companion.e(TAG, "onWriteDataFail\nerrorMsg=" + errorMsg + "\nloglevel=" + loglevel + "\nbleManage=" + bleManage);
                        }
                        bleManage.handleError(-10039);
                    }
                }
        );
        BluetoothGattService bluetoothGattService = bleManage.getBluetoothGatt().getService(uuids[0]);
        bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(uuids[1]);
        if (bleManage.getWithoutNoResponse()) {
            bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }
        return this;
    }
}