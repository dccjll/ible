package com.paiai.mble.dfu;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.dcc.ibase.log.LogManager;
import com.dcc.ibase.utils.AppUtils;
import com.dcc.ibase.utils.RegexUtils;
import com.paiai.mble.BLEManage;
import com.paiai.mble.BLEMsgCode;
import com.paiai.mble.core.BLEScan;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 14:29<br>
 * 功能描述：通用的固件升级<br>
 */
public class DeviceFirewareUpdate {

    private static final String TAG = DeviceFirewareUpdate.class.getSimpleName();
    private int The_Num_To_Attempt = 0;//尝试更新次数
    private static final int The_Max_Num_To_Attempt = 3;//尝试固件更新的最大次数
    private BLEManage bleManage;
    private String newTargetMacAddress = null;
    private boolean updateFinished;//更新完成标识
    private boolean sendDFUCmd;//是否已发送进入固件更新指令
    private boolean startDFU;//是否已启动更新
    private int progress;//当前进度

    private final BLEScan.OnBLEScanListener onBLEScanListener = new BLEScan.OnBLEScanListener() {
        @Override
        public void onFoundDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            LogManager.Companion.i(TAG, "onScanResult,扫描到设备,mac=" + bluetoothDevice.getAddress() + ",name=" + bluetoothDevice.getName());
            if (bluetoothDevice.getAddress().equalsIgnoreCase(newTargetMacAddress)) {//找到短地址
                bleManage.stopScan();
                if (startDFU) {
                    LogManager.Companion.e(TAG, "找到新地址，但是更新服务已在运行，不再重新启动更新服务");
                    return;
                }
                LogManager.Companion.i(TAG, "找到新地址，表示设备已经进入了更新模式，直接启动dfu更新");
//               bleManage.stopScan();
                postDataToBLEDevice(bluetoothDevice);
                startDFU = true;
            }else if(bluetoothDevice.getAddress().equalsIgnoreCase(deviceMac)){//找到长地址
                bleManage.stopScan();
                if (sendDFUCmd) {
                    LogManager.Companion.i(TAG, "找到原始地址，但是已发送进入固件更新模式指令，不再重复发送");
                    return;
                }
                LogManager.Companion.i(TAG, "找到原始地址，发送进入固件更新模式指令");
//               bleManage.stopScan();
                if (onSimpleDfuProgressListener != null) {
                    onSimpleDfuProgressListener.inputFirewareUpdateMode(bluetoothDevice.getAddress(), onInputFirewareUpdateModeListener);
                    sendDFUCmd = true;
                }
            }
        }

        @Override
        public void onScanFinish(List<Map<String, Object>> bluetoothDeviceList) {

        }

        @Override
        public void onScanFail(final int errorCode) {
            handleError(deviceMac, errorCode);
        }
    };

    //升级过程监听器
    private final DfuProgressListener dfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu正在连接...");
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu已连接");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu正在在启动...");
            if (onSimpleDfuProgressListener != null) {
                onSimpleDfuProgressListener.onDfuProcessStarting(deviceAddress);
            }
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu已启动");
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            LogManager.Companion.e(TAG, "正在启用dfu模式...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            LogManager.Companion.i(TAG, "更新进度,pecent=" + percent);
            progress = percent;
            if (progress == 100) {
                updateFinished = true;
            }
            if (onSimpleDfuProgressListener != null) {
                onSimpleDfuProgressListener.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
            }
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu正在验证固件...");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu正在断开连接...");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu已断开连接,updateFinished=" + updateFinished);
            if (!updateFinished) {
                onError(deviceAddress, DfuBaseService.ERROR_CONNECTION_STATE_MASK, DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE, "dfuDisconnected");
            }
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            updateFinished = true;
            LogManager.Companion.i(TAG, "设备上更新固件成功");
            /*if (new File(firewareAddress).exists()) {
                FileUtils.INSTANCE.deleteFile(firewareAddress);
            }*/
            DfuServiceListenerHelper.unregisterProgressListener(AppUtils.Companion.getApp(), dfuProgressListener);
            if (onSimpleDfuProgressListener != null) {
                onSimpleDfuProgressListener.onDfuCompleted(deviceAddress);
            }
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            LogManager.Companion.e(TAG, "dfu中断...");
            if (!updateFinished) {
                onError(deviceAddress, DfuBaseService.ERROR_CONNECTION_STATE_MASK, DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE, "dfuAborted");
            }
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            if (The_Num_To_Attempt ++ < The_Max_Num_To_Attempt) {
                LogManager.Companion.i(TAG, "第" + The_Num_To_Attempt + "次固件更新失败," + message + ",尝试第" + (The_Num_To_Attempt + 1) + "次更新...");
                startDFU = false;
                sendDFUCmd = false;
                progress = 0;
                bleManage.setTargetDeviceAddress(null);
                bleManage.setBleFilter(null);
                startScan();
                return;
            }
            LogManager.Companion.i(TAG, "第" + The_Num_To_Attempt + "次固件更新失败," + message + ",已尝试最大重试次数" + The_Max_Num_To_Attempt + ",不再尝试，固件更新失败");
            DfuServiceListenerHelper.unregisterProgressListener(AppUtils.Companion.getApp(), dfuProgressListener);
            handleError(deviceAddress, -10044);
        }
    };

    //设备进入更新模式成功之后的监听器
    private final OnInputFirewareUpdateModeListener onInputFirewareUpdateModeListener = new OnInputFirewareUpdateModeListener() {
        @Override
        public void inputFirewareUpdateModeSuccess() {
            bleManage.setTargetDeviceAddress(newTargetMacAddress);
            bleManage.setBleFilter(new BLEScan.BLEFilter() {
                @Override
                public boolean matcher(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                    return newTargetMacAddress.equalsIgnoreCase(bluetoothDevice.getAddress());
                }
            });
            startScan();
        }
    };

    private String deviceMac;
    private String firewareAddress;
    private OnSimpleDfuProgressListener onSimpleDfuProgressListener;//响应监听器

    /**
     * 配置设备mac地址<br/>
     * 必须配置，必须是设备真实的mac地址
     */
    public DeviceFirewareUpdate setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
        return this;
    }

    /**
     * 配置要升级的固件地址<br/>
     * 可以是一个网上的文件地址，也可以是在本地带文件名的完整路径<br/>
     * 必须配置
     */
    public DeviceFirewareUpdate setFirewareAddress(String firewareAddress) {
        this.firewareAddress = firewareAddress;
        return this;
    }

    /**
     * 配置执行状态监听器<br/>
     * 可不配置，不配置时将无法接收到执行状态报告
     */
    public DeviceFirewareUpdate setOnSimpleDfuProgressListener(OnSimpleDfuProgressListener onSimpleDfuProgressListener) {
        this.onSimpleDfuProgressListener = onSimpleDfuProgressListener;
        return this;
    }

    @Override
    public String toString() {
        return "DeviceFirewareUpdate{" + "deviceMac='" + deviceMac + '\'' + ", firewareAddress='" + firewareAddress + '\'' + ", onSimpleDfuProgressListener=" + onSimpleDfuProgressListener + '}';
    }

    /**
     * 任务开始
     */
    public void walk(){
        LogManager.Companion.i(TAG, "接口请求数据:" + this);
        checkParams();
    }

    /**
     * 验证参数
     */
    private void checkParams() {
        if (!RegexUtils.INSTANCE.checkAddress(deviceMac)) {
            handleError(deviceMac, -10003);
            return;
        }
        if (TextUtils.isEmpty(firewareAddress)) {
            LogManager.Companion.e(TAG, "TextUtils.isEmpty(firewareAddress) false");
            handleError(deviceMac, -10045);
            return;
        }
        if (!caculateTargetAddress()) {
            LogManager.Companion.e(TAG, "进入固件更新的mac地址转换失败");
            handleError(deviceMac, -10010);
            return;
        }
        bleManage = new BLEManage();
        if (!bleManage.hasBLEFeature()) {
            handleError(deviceMac, -10000);
            return;
        }
//       bleManage.setTargetDeviceAddress(deviceMac);
        bleManage.setListenterObject(onBLEScanListener);
        if (firewareAddress.toLowerCase(Locale.getDefault()).startsWith("http:") || firewareAddress.toLowerCase(Locale.getDefault()).startsWith("https")) {//暂不支持网络地址的固件升级
            handleError(deviceMac, -10048);
            return;
        }
        LogManager.Companion.i(TAG, "固件地址指向一个本地地址");
        if (!new File(firewareAddress).exists()) {
            LogManager.Companion.e(TAG, "固件地址指向一个本地地址，但是该地址不存在任何文件");
            handleError(deviceMac, -10047);
            return;
        }
        LogManager.Companion.i(TAG, "固件地址指向一个本地地址，文件存在");
        final String extension = "(?i)ZIP"; //(?i) = case insensitive
        final boolean statusOk = MimeTypeMap.getFileExtensionFromUrl(firewareAddress).matches(extension);
        if (!statusOk) {
            LogManager.Companion.e(TAG, "文件格式检测失败");
            handleError(deviceMac, -10011);
            return;
        }
        initDFU();
//       //是网络地址
//       String filePath = BLESDKLibrary.context.getFilesDir().getLogFileRelativePath() + "data/" + BLESDKLibrary.context.getPackageName() + "/files/";
//       String fileName = firewareAddress.substring(firewareAddress.lastIndexOf("/") + 1);
//       ServerUtil.downloadFile(firewareAddress, filePath, fileName, new NoHttpUtil.DownloadResponseListener() {
//           @Override
//           public void onProgress(int progress) {
//               LogManager.i(TAG, "下载进度：" + progress);
//           }
//
//           @Override
//           public void onSuccess(String filePath) {
//               firewareAddress = filePath;
//               LogManager.i(TAG, "文件下载成功，路径为：" + firewareAddress);
//               if (!new File(firewareAddress).exists()) {
//                   LogManager.e(TAG, "固件地址指向一个内部地址，但是该地址不存在任何文件");
//                   handleError(deviceMac, -10047);
//                   return;
//               }
//               LogManager.i(TAG, "固件地址指向一个内部地址，文件存在");
//               final String extension = "(?i)ZIP"; //(?i) = case insensitive
//               final boolean statusOk = MimeTypeMap.getFileExtensionFromUrl(firewareAddress).matches(extension);
//               if (!statusOk) {
//                   LogManager.e(TAG, "文件格式检测失败");
//                   handleError(deviceMac, -10011);
//                   return;
//               }
//               initDFU();
//           }
//
//           @Override
//           public void onFailure(Integer integer) {
//               LogManager.e(TAG, BLEMsgCode.parseMessageCode(integer));
//               handleError(deviceMac, integer);
//           }
//       });
    }

    /**
     * 转换正常的mac地址为进入dfu模式的mac地址，目前小嘀管家协议的为原始地址最后一段mac地址自增1
     */
    private boolean caculateTargetAddress() {
        String targetmacaddress = deviceMac;
        String foremacaddress = targetmacaddress.substring(0, targetmacaddress.length() - 2);
        String lastsegmentmac = targetmacaddress.split(":")[5];
        int tempmacint = Integer.parseInt(lastsegmentmac, 16);
        int lastsegmentmacint = 0;
        if (tempmacint != 0xFF) {
            lastsegmentmacint = tempmacint + 1;
        }
        String lastsegmentmacupdate = Integer.toHexString(lastsegmentmacint);
        if (lastsegmentmacupdate.length() == 1) {
            lastsegmentmacupdate = "0" + lastsegmentmacupdate;
        }
        lastsegmentmacupdate = lastsegmentmacupdate.toUpperCase(Locale.getDefault());
        newTargetMacAddress = foremacaddress + lastsegmentmacupdate;
        LogManager.Companion.i(TAG, "进入DFU前的设备地址=" + targetmacaddress + "\n进入DFU后的设备地址=" + newTargetMacAddress);
        return true;
    }

    /**
     * 初始化dfu
     */
    private void initDFU(){
        startDFU = false;
        sendDFUCmd = false;
        progress = 0;
        DfuServiceListenerHelper.registerProgressListener(AppUtils.Companion.getApp(), dfuProgressListener);
        if(BLEManage.checkConnectStatus(deviceMac)){
            LogManager.Companion.i(TAG, "设备已连接，请发送命令让设备进入固件更新模式");
            if (onSimpleDfuProgressListener != null) {
                onSimpleDfuProgressListener.inputFirewareUpdateMode(deviceMac, onInputFirewareUpdateModeListener);
            }
            return;
        }
        startScan();
    }

    /**
     * 扫描更新固件的蓝牙设备
     */
    private void startScan() {
        try {
            bleManage.startScan();
            LogManager.Companion.i(TAG, "已开始扫描");
        } catch (Exception e) {
            e.printStackTrace();
            bleManage.stopScan();
            LogManager.Companion.e(TAG, "开始扫描设备发生异常");
            handleError(deviceMac, -10012);
        }
    }

    /**
     * 启动蓝牙服务传输数据到设备
     */
    private void postDataToBLEDevice(BluetoothDevice device) {
        updateFinished = false;

        //final Intent service = new Intent(contentActivity, DfuService.class);
        //
        //service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS,
        //device.getAddress());
        //service.putExtra(DfuService.EXTRA_DEVICE_NAME, device.getName());
        //service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE,
        //DfuService.MIME_TYPE_ZIP);
        //service.putExtra(DfuService.EXTRA_FILE_TYPE, DfuService.TYPE_AUTO);
        //service.putExtra(DfuService.EXTRA_FILE_PATH, filePathWithName); //a
        //logFileRelativePath or URI must be provided.
        ////service.putExtra(DfuService.EXTRA_FILE_URI, "");
        ////Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or
        //BIN file is given above.
        ////In case of a ZIP file, the init packet (a DAT file) must be
        //included inside the ZIP file.
        ////service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, "");
        ////service.putExtra(DfuService.EXTRA_INIT_FILE_URI, "");
        //service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
        //
        //contentActivity.startService(service);

        final DfuServiceInitiator starter = new DfuServiceInitiator(device.getAddress()).setDeviceName(device.getName()).setKeepBond(false);
        starter.setZip(null, firewareAddress);
        starter.start(AppUtils.Companion.getApp(), DfuService.class);
        LogManager.Companion.i(TAG, "starter started!");

    }

    /**
     * 处理异常
     */
    private  void handleError(final String deviceMac, final int errorCode) {
        LogManager.Companion.e(TAG, BLEMsgCode.parseMessageCode(errorCode));
//       if (new File(firewareAddress).exists()) {
//           FileUtils.INSTANCE.deleteFile(firewareAddress);
//       }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (onSimpleDfuProgressListener != null) {
                    onSimpleDfuProgressListener.onError(deviceMac, errorCode);
                }

            }
        });
    }

    /**
     * 作者：dccjll<br>
     * 创建时间：2017/11/6 13:36<br>
     * 功能描述：进入固件更新模式监听器<br>
     */
    public static interface OnInputFirewareUpdateModeListener {

        /**
         * 进入固件更新模式成功
         */
        void inputFirewareUpdateModeSuccess();
    }
}
