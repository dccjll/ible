package com.paiai.mble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.AppUtils;
import com.dcc.ibase.utils.RegexUtils;
import com.paiai.mble.core.BLEConnect;
import com.paiai.mble.core.BLEFindService;
import com.paiai.mble.core.BLEOpenNotification;
import com.paiai.mble.core.BLEScan;
import com.paiai.mble.core.BLEWriteData;
import com.paiai.mble.response.BLEResponseManager;
import com.paiai.mble.response.OnBLEResponse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:27<br>
 * 功能描述：蓝牙低功耗管理器<br>
 */
public class BLEManage {

    private final static String TAG = BLEManage.class.getSimpleName();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    public static List<Map<String, Object>> connectedBluetoothGattList = new CopyOnWriteArrayList<>();//当前已连接的设备服务器连接池  第一次参数表示某一个连接对象，第二个参数表示该连接上一次通讯(读过或接收过数据)的时间戳
    public static List<Map<String, Object>> bluetoothDeviceList = new CopyOnWriteArrayList<>();//蓝牙设备对象池，缓存所有扫描过的设备

    private BLEResponseManager bleResponseManager;//蓝牙核心响应管理器
    private Boolean running = true;//当前蓝牙任务是否正在执行
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());//超时管理对象
    private UUID[] writeuuids;//从serviceUUIDs分离出来的写数据UUID,取serviceUUIDs的0,1个UUID,分别为服务与特征uuid
    private UUID[] notificationuuids;//从serviceUUIDs分离出来的通知UUID,取serviceUUIDs的2,3,4个UUID
    private Boolean needOpenNotification = false;//是否接收设备返回的数据
    private BLEScan bleScan;//蓝牙扫描管理器
    private BLEConnect bleConnect;//蓝牙连接管理器
    private BLEWriteData bleWriteData;//写数据管理器
    private BluetoothDevice bluetoothDevice;//扫描到的目标蓝牙设备
    private BluetoothGatt bluetoothGatt;//蓝牙连接服务器
    private BLEGattCallback bleGattCallback;//连接状态回调管理器
    private boolean hasBLEFeature;//是否支持蓝牙ble功能

    private boolean withoutNoResponse = false;
    private BLEScan.BLEFilter bleFilter;//扫描过滤器
    private String targetDeviceAddress;//目前设备地址
    private String targetDeviceName;//目标设备名称
    private List<String> targetDeviceAddressList;//目标设备地址列表
    private byte[] data;//发送的总数据包字节数组
    private List<byte[]> dataList;//发送的总数据包字节数组列表
    private int timeoutScanBLE = BLEConfig.SCAN_TIMEOUT_INTERVAL;//扫描蓝牙默认超时时间
    private UUID[] serviceUUIDs;//设备的UUID,uuids=2 则不接受设备返回的数据, uuids=5 则接收设备返回的数据
    private Boolean disconnectOnFinish = false;//任务完成后是否关闭蓝牙
    private Object listenterObject;//响应对象
    private boolean multiScan = true;//是否多次扫描

    private boolean enableLogFlag = true;//是否启用蓝牙log

    public boolean getEnableLogFlag() {
        return enableLogFlag;
    }

    public BLEManage setEnableLogFlag(boolean enableLogFlag) {
        this.enableLogFlag = enableLogFlag;
        return this;
    }

    public BLEManage() {
        bleResponseManager = new BLEResponseManager(this);
        running = true;
        bluetoothManager = (BluetoothManager) AppUtils.Companion.getApp().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        hasBLEFeature = AppUtils.Companion.getApp().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 开始扫描
     */
    public void startScan(){
        if (!(listenterObject instanceof BLEScan.OnBLEScanListener)) {//非单纯的扫描任务才启用全局超时，扫描任务有自己的超时
            timeoutHandler.postDelayed(timeoutRunnable, BLEConfig.WHOLE_TASK_TIMEOUT_INTERVAL);
        }
        bleScan = new BLEScan(this);
        bleScan.setMultiScan(multiScan);
        bleScan.startScan();
    }

    /**
     * 停止扫描
     */
    public void stopScan() {
        if (bleScan == null) {
            LogManager.Companion.i(TAG, "stopScan, bleScan==null");
            return;
        }
        bleScan.stopScan();
    }

    /**
     * 开始连接
     */
    public void connect(){
        timeoutHandler.postDelayed(timeoutRunnable, BLEConfig.WHOLE_TASK_TIMEOUT_INTERVAL);
        if (reuseGatt()) {
            LogManager.Companion.i(TAG, "已复用连接");
            return;
        }
        if (reuseDevice()) {
            LogManager.Companion.i(TAG, "已复用设备");
            return;
        }
        startScan();
    }

    /**
     * 寻找服务
     */
    public void findService(){
        timeoutHandler.postDelayed(timeoutRunnable, BLEConfig.WHOLE_TASK_TIMEOUT_INTERVAL);
        if (reuseGatt()) {
            LogManager.Companion.i(TAG, "已复用连接");
            return;
        }
        if (reuseDevice()) {
            LogManager.Companion.i(TAG, "已复用设备");
            return;
        }
        startScan();
    }

    /**
     * 打开通知
     */
    public void openNotification(){
        timeoutHandler.postDelayed(timeoutRunnable, BLEConfig.WHOLE_TASK_TIMEOUT_INTERVAL);
        if (reuseGatt()) {
            LogManager.Companion.i(TAG, "已复用连接");
            return;
        }
        if (reuseDevice()) {
            LogManager.Companion.i(TAG, "已复用设备");
            return;
        }
        startScan();
    }

    /**
     * 写数据
     */
    public void write(){
        timeoutHandler.postDelayed(timeoutRunnable, BLEConfig.WHOLE_TASK_TIMEOUT_INTERVAL);
        if (reuseGatt()) {
            LogManager.Companion.i(TAG, "已复用连接");
            return;
        }
        LogManager.Companion.i(TAG, "无可复用的连接");
        if (reuseDevice()) {
            LogManager.Companion.i(TAG, "已复用设备");
            return;
        }
        LogManager.Companion.i(TAG, "无可复用的设备");
        bleScan = new BLEScan(this);
        bleScan.startScan();
    }

    /**
     * gatt连接复用
     */
    private boolean reuseGatt() {
        BluetoothGatt bluetoothGatt = getBluetoothGatt(getDeviceAttr());
        if(bluetoothGatt != null){
            this.bluetoothGatt = bluetoothGatt;
            Map<String, Object> bluetoothGattMap = getBluetoothGattMap(bluetoothGatt);
            if (bluetoothGattMap == null) {
                return false;
            }
            this.bleGattCallback = (BLEGattCallback) bluetoothGattMap.get("bluetoothGattCallback");
            if (bleGattCallback == null) {
                return false;
            }
            this.bleGattCallback.setBLEResponseManager(bleResponseManager);
            LogManager.Companion.i(TAG, "从连接池中获取到连接\nbluetoothGatt=" + bluetoothGatt + "\naddress=" + bluetoothGatt.getDevice().getAddress());
            if (listenterObject instanceof BLEWriteData.OnBLEWriteDataListener || listenterObject instanceof OnBLEResponse) {//需要写数据或接收数据
                if (listenterObject instanceof OnBLEResponse) {
                    this.bleGattCallback.registerOnBLEResponse((OnBLEResponse) listenterObject);
                }
                bleWriteData = new BLEWriteData(this);
                if (bluetoothGattMap.get("mtuSize") == null) {
                    LogManager.Companion.i(TAG, "bluetoothGattMap.get(\"mtuSize\") == null");
                    return false;
                }
                int mtuSize = (int) bluetoothGattMap.get("mtuSize");
                setBleMaxBytesCount(mtuSize);
                bleWriteData.writeData();
                return true;
            } else if (listenterObject instanceof BLEOpenNotification.OnBLEOpenNotificationListener) {
                bleResponseManager.onOpenNotificationSuccess(bluetoothGatt, null, BluetoothGatt.GATT_SUCCESS, bleGattCallback);
                return true;
            } else if (listenterObject instanceof BLEFindService.OnBLEFindServiceListener) {
                bleResponseManager.onFindServiceSuccess(bluetoothGatt, BluetoothGatt.GATT_SUCCESS, bluetoothGatt.getServices(), bleGattCallback);
                return true;
            } else if (listenterObject instanceof BLEConnect.OnBLEConnectListener) {
                bleResponseManager.onConnectSuccess(bluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED, bleGattCallback);
                return true;
            } else {
                LogManager.Companion.i(TAG, "gatt连接复用方法中未找到合适的监听器");
                return false;
            }
        }
        return false;
    }

    /**
     * 设备复用
     */
    private boolean reuseDevice() {
        LogManager.Companion.i(TAG, "设备对象池遍历\nbluetoothDeviceList=" + bluetoothDeviceList);
        BluetoothDevice cacheBluetoothDevice = null;
        for (Map<String, Object> bluetoothDeviceMap : bluetoothDeviceList) {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) bluetoothDeviceMap.get("bluetoothDevice");
            if ((!TextUtils.isEmpty(targetDeviceAddress) && targetDeviceAddress.equalsIgnoreCase(bluetoothDevice.getAddress()))
                    || !TextUtils.isEmpty(targetDeviceName) && targetDeviceName.equalsIgnoreCase(bluetoothDevice.getName())) {
                cacheBluetoothDevice = bluetoothDevice;
                break;
            }
        }
        LogManager.Companion.i(TAG, "目标设备对象\ncacheBluetoothDevice=" + cacheBluetoothDevice);
        if (cacheBluetoothDevice != null){
            LogManager.Companion.i(TAG, "从设备对象池中获取到设备\ncacheBluetoothDevice=" + cacheBluetoothDevice);
            bleConnect = new BLEConnect(cacheBluetoothDevice, this);
            bleConnect.connect();
            return true;
        }
        return false;
    }

    /**
     * 移除超时任务
     */
    public void removeTimeoutCallback(){
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    /**
     * 获取设备识别标志
     */
    public String getDeviceAttr() {
        String attr = null;
        if (RegexUtils.INSTANCE.checkAddress(targetDeviceAddress)) {
            attr = targetDeviceAddress;
        } else if (!TextUtils.isEmpty(targetDeviceName)) {
            attr = targetDeviceName;
        } else if (bluetoothDevice != null) {
            attr = bluetoothDevice.getAddress();
        }
        return attr;
    }

    /**
     * 验证当前属性的设备是否已连接
     */
    public synchronized static Boolean checkConnectStatus(String deviceAttr){
        LogManager.Companion.i(TAG, "验证当前属性的设备是否已连接, deviceAttr=" + deviceAttr);
        if (TextUtils.isEmpty(deviceAttr)) {
            return false;
        }
        for (Map<String,Object> map: BLEManage.connectedBluetoothGattList) {
            if(deviceAttr.equalsIgnoreCase(((BluetoothGatt)map.get("bluetoothGatt")).getDevice().getAddress())
                    || deviceAttr.equalsIgnoreCase(((BluetoothGatt)map.get("bluetoothGatt")).getDevice().getName())){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取连接的服务列表
     */
    public List<BluetoothGattService> getBluetoothGattServerList() {
       if (bluetoothGatt == null) {
           return null;
       }
       return bluetoothGatt.getServices();
    }

    /**
     * 获取一个BluetoothGatt连接对象
     */
    public synchronized static BluetoothGatt getBluetoothGatt(String deviceAttr){
        if (TextUtils.isEmpty(deviceAttr)) {
            return null;
        }
        for (Map<String,Object> map: BLEManage.connectedBluetoothGattList) {
            if(deviceAttr.equalsIgnoreCase(((BluetoothGatt)map.get("bluetoothGatt")).getDevice().getAddress())
                    || deviceAttr.equalsIgnoreCase(((BluetoothGatt)map.get("bluetoothGatt")).getDevice().getName())){
                return (BluetoothGatt)map.get("bluetoothGatt");
            }
        }
        return null;
    }

    /**
     * 获取一个Map连接对象，对象是一个Map复合对象，包含回调、连接、上一次通讯时间等
     */
    public synchronized static Map<String, Object> getBluetoothGattMap(BluetoothGatt bluetoothGatt){
        for (Map<String,Object> map: BLEManage.connectedBluetoothGattList) {
            if(map.get("bluetoothGatt") == bluetoothGatt){
                return map;
            }
        }
        return null;
    }

    /**
     * 移除一个复合连接对象
     */
    public synchronized static void removeConnect(String deviceAttr){
        LogManager.Companion.i(TAG, "移除一个复合连接(BluetoothGatt)对象, deviceAttr=" + deviceAttr);
        if (TextUtils.isEmpty(deviceAttr)) {
            return;
        }
        for (Map<String, Object> bluetoothLongMap : BLEManage.connectedBluetoothGattList) {
            BluetoothGatt bluetoothGatt = (BluetoothGatt) bluetoothLongMap.get("bluetoothGatt");
            if (deviceAttr.equalsIgnoreCase(bluetoothGatt.getDevice().getName()) || deviceAttr.equalsIgnoreCase(bluetoothGatt.getDevice().getAddress())) {
                BLEManage.connectedBluetoothGattList.remove(bluetoothLongMap);
                bluetoothGatt = null;
            }
        }
    }

    /**
     * 移除一个复合设备对象
     */
    public synchronized static void removeDevice(String deviceAttr){
        LogManager.Companion.i(TAG, "移除一个复合设备(BluetothDevice)对象, deviceAttr=" + deviceAttr);
        if (TextUtils.isEmpty(deviceAttr)) {
            return;
        }
        for (Map<String, Object> bluetoothLongMap : BLEManage.bluetoothDeviceList) {
            if (deviceAttr.equalsIgnoreCase(((BluetoothDevice) bluetoothLongMap.get("bluetoothDevice")).getAddress())) {
                BLEManage.bluetoothDeviceList.remove(bluetoothLongMap);
            }
        }
    }

    /**
     * 更新某一个连接对象最近一次通讯的时间戳
     */
    public synchronized static void updateBluetoothGattLastCommunicationTime(BluetoothGatt bluetoothGatt, Long lastCommunicationTime){
        for (Map<String,Object> map: BLEManage.connectedBluetoothGattList) {
            if(map.get("bluetoothGatt")  == bluetoothGatt){
                map.put("connectedTime", lastCommunicationTime);
            }
        }
    }

    /**
     * 反射对用BluetoothGatt对象隐藏的方法refresh强制刷新连接对象状态
     */
    private static void refreshBluetoothGatt(BluetoothGatt bluetoothGatt) {
        try {
            LogManager.Companion.i(TAG, "反射对用BluetoothGatt对象隐藏的方法refresh强制刷新连接对象状态");
            if (bluetoothGatt == null) {
                LogManager.Companion.e(TAG, "连接已不存在");
                return;
            }
            Method refresh = BluetoothGatt.class.getDeclaredMethod("refresh");
            boolean refreshState = (boolean) refresh.invoke(bluetoothGatt);
            LogManager.Companion.i(TAG, refreshState ? "已刷新" : "刷新方法调用失败");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开当前连接
     */
    public void disconnect() {
        disconnect(bluetoothGatt);
    }

    /**
     * 断开指定连接
     */
    public static void disconnect(String deviceAttr){
        BluetoothGatt bluetoothGatt = getBluetoothGatt(deviceAttr);
        disconnect(bluetoothGatt);
    }

    /**
     * 断开指定连接
     */
    public static void disconnect(final BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt == null) {
            LogManager.Companion.i(TAG, "请求断开指定连接, 连接不存在");
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    LogManager.Companion.i(TAG, "close线程=" + Thread.currentThread());
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                    BLEManage.refreshBluetoothGatt(bluetoothGatt);
                    BLEManage.removeConnect(bluetoothGatt.getDevice().getAddress());
                    BLEManage.removeDevice(bluetoothGatt.getDevice().getAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 断开所有连接
     */
    public static void disconnectAllBluetoothGatt() {
        for (Map<String, Object> map : connectedBluetoothGattList) {
            disconnect((BluetoothGatt) map.get("bluetoothGatt"));
        }
    }

    /**
     * 处理错误消息
     */
    public void handleError(int errorCode){
        bleResponseManager.onResponseError(listenterObject, errorCode);
    }

    public BLEScan.BLEFilter getBleFilter() {
        return bleFilter;
    }

    public void setBleFilter(BLEScan.BLEFilter bleFilter) {
        this.bleFilter = bleFilter;
    }

    public Object getListenterObject() {
        return listenterObject;
    }

    private final Runnable timeoutRunnable = new Runnable() {//超时任务
        @Override
        public void run() {
            bleResponseManager.onResponseError(listenterObject, -10041);
        }
    };

    public BLEGattCallback getBleGattCallback() {
        return bleGattCallback;
    }

    public BLEManage setBleGattCallback(BLEGattCallback bleGattCallback) {
        this.bleGattCallback = bleGattCallback;
        return this;
    }

    public BLEResponseManager getBleResponseManager() {
        return bleResponseManager;
    }

    public Boolean getRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public BLEManage setListenterObject(Object listenterObject) {
        this.listenterObject = listenterObject;
        if (listenterObject instanceof OnBLEResponse) {
            ((OnBLEResponse)listenterObject).setBleResponseManager(bleResponseManager);
        }
        return this;
    }

    public Boolean getDisconnectOnFinish() {
        return disconnectOnFinish;
    }

    public void setDisconnectOnFinish(boolean disconnectOnFinish){
        this.disconnectOnFinish = disconnectOnFinish;
    }

    public void setTargetDeviceAddress(String targetDeviceAddress) {
        this.targetDeviceAddress = targetDeviceAddress;
    }

    public void setTargetDeviceName(String targetDeviceName) {
        this.targetDeviceName = targetDeviceName;
    }

    public void setTargetDeviceAddressList(List<String> targetDeviceAddressList) {
        this.targetDeviceAddressList = targetDeviceAddressList;
    }

    public void setServiceUUIDs(UUID[] serviceUUIDs) {
        if(serviceUUIDs == null || (serviceUUIDs.length != 2 && serviceUUIDs.length != 5)){
            LogManager.Companion.i(TAG, "setServiceUUIDs,设置的扫描uuids过滤为空,已忽略");
            return;
        }
        this.serviceUUIDs = serviceUUIDs;
        UUID[] writeDataUUIDS = new UUID[2];
        writeDataUUIDS[0] = serviceUUIDs[0];
        writeDataUUIDS[1] = serviceUUIDs[1];
        setWriteuuids(writeDataUUIDS);
        if (serviceUUIDs.length == 5) {
            UUID[] notificationuuids = new UUID[3];
            notificationuuids[0] = serviceUUIDs[2];
            notificationuuids[1] = serviceUUIDs[3];
            notificationuuids[2] = serviceUUIDs[4];
            setNotificationuuids(notificationuuids);
        }
        LogManager.Companion.i(TAG, "setServiceUUIDs,设置的扫描uuids过滤条件已更新为" + Arrays.toString(serviceUUIDs));
    }

    public void setTimeoutScanBLE(int timeoutScanBLE) {
        if(timeoutScanBLE <= this.timeoutScanBLE){
            LogManager.Companion.i(TAG, "setTimeoutScanBLE,设置的扫描超时不能小于或等于系统默认的扫描超时时间(" + this.timeoutScanBLE + "),已忽略");
            return;
        }
        this.timeoutScanBLE = timeoutScanBLE;
        LogManager.Companion.i(TAG, "setTimeoutScanBLE, 扫描超时时间已被更新为" + this.timeoutScanBLE + "ms");
    }

    public BLEManage setNotificationuuids(UUID[] notificationuuids) {
        this.notificationuuids = notificationuuids;
        needOpenNotification = true;
        return this;
    }

    public BLEManage setWriteuuids(UUID[] writeuuids) {
        this.writeuuids = writeuuids;
        return this;
    }

    public void setLongScanFlag() {
        BLEConfig.LONG_SCAN_FALG = true;
    }

    public BLEManage setBleConnect(BLEConnect bleConnect) {
        this.bleConnect = bleConnect;
        return this;
    }

    public BLEConnect getBleConnect() {
        return bleConnect;
    }

    public int getTimeoutScanBLE() {
        return timeoutScanBLE;
    }

    public String getTargetDeviceAddress() {
        return targetDeviceAddress;
    }

    public String getTargetDeviceName() {
        return targetDeviceName;
    }

    public List<String> getTargetDeviceAddressList() {
        return targetDeviceAddressList;
    }

    public UUID[] getServiceUUIDs() {
        return serviceUUIDs;
    }

    public UUID[] getNotificationuuids() {
        return notificationuuids;
    }

    public UUID[] getWriteuuids() {
        return writeuuids;
    }

    public Boolean getNeedOpenNotification() {
        return needOpenNotification;
    }

    public BLEManage setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        targetDeviceAddress = bluetoothDevice.getAddress();
        targetDeviceName = bluetoothDevice.getName();
        return this;
    }

    public BLEManage setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
        return this;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public boolean getWithoutNoResponse() {
        return withoutNoResponse;
    }

    public BLEManage setWithoutNoResponse(boolean withoutNoResponse) {
        this.withoutNoResponse = withoutNoResponse;
        return this;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public List<byte[]> getDataList() {
        return dataList;
    }

    public BLEManage setDataList(List<byte[]> dataList) {
        this.dataList = dataList;
        return this;
    }

    public BLEManage setBleWriteData(BLEWriteData bleWriteData) {
        this.bleWriteData = bleWriteData;
        return this;
    }

    public BLEWriteData getBleWriteData() {
        return bleWriteData;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean hasBLEFeature() {
        return hasBLEFeature;
    }

    public BLEManage setMultiScan(boolean multiScan) {
        this.multiScan = multiScan;
        return this;
    }

    public int getBleMaxBytesCount() {
        return bleWriteData.getMaxBytesCount();
    }

    public void setBleMaxBytesCount(int mtuSize) {
        bleWriteData.setMaxBytesCount(mtuSize);
    }
}
