package com.paiai.mble;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 11:22<br>
 * 功能描述：蓝牙基础参数配置<br>
 */
public class BLEConfig {

    /**
     * 动态参数，根据实际手机型号酌情匹配
     */
    //长扫描标志，设置该标志时将一直扫描，除非手动停止扫描
    public static boolean LONG_SCAN_FALG = false;
    //单次扫描的超时时间间隔
    public static int SCAN_TIMEOUT_INTERVAL = 5 * 1000;
    //最多扫描次数
    public static int MAX_SCAN_COUNT = 3;

    //扫描到的设备缓存时间
    public static int DEVICE_MAX_CACHED_TIME = 2 * 60 * 1000;
    //最多连接次数
    public static int MAX_CONNECT_COUNT = 5;

    //连接上设备后开始找服务的时间间隔
    public static int START_FIND_SERVICE_INTERVAL = 10;
    //找服务成功后开始打开通知的时间间隔
    public static int START_OPEN_NOTIFICATION_INTERVAL = 0;

    //没有数据交互时是否自动断开
    public static boolean AUTO_DISCONNECT_WHEN_NO_DATA_INTERACTION = true;
    //蓝牙连接上之后，多久没有数据交互将主动断开连接的间隔时间，AutoDisconnectWhenNoDataInteraction为true时有效
    public static int AUTO_DISCONNECT_INTERVAL_WHEN_NO_DATA_INTERACTION = 2 * 60 * 1000;

    //整个任务的超时时间
    public static int WHOLE_TASK_TIMEOUT_INTERVAL = 10 * 1000;
    //任务完成后执行断开的时间间隔，任务完成后断开标识(BLEResponseManager中的disconnectOnFinish)为true时有效
    public static int START_DISCONNECT_INTERVAL_WHEN_FINISH = 2 * 1000;

    /**
     * 静态参数
     */
    //蓝牙发送数据分包，每个包的最大长度为20个字节
    public static final int MAX_BYTES = 20;
    //发送多个数据包时的时间间隔(毫秒)
    public static long SEND_NEXT_PACKAGE_INTERVAL = 70;
    //最多连接设备数量
    public static final int MaxConnectDeviceNum = 6;

    /**
     * 设置整个任务的超时时间，为某些情况下需要临时延长任务的超时时间提供中间支持
     */
    public static void configWholeTaskTimeoutInterval(int wholeTaskTimeoutInterval) {
        if (wholeTaskTimeoutInterval > 0) {
            WHOLE_TASK_TIMEOUT_INTERVAL = wholeTaskTimeoutInterval;
        }
    }

    /**
     * 恢复整个任务的超时时间
     */
    public static void resumeWholeTaskTimeoutInterval() {
        WHOLE_TASK_TIMEOUT_INTERVAL = 10 * 1000;
    }

    /**
     * 设置数据包发送的时间间隔
     */
    public static void configSendNextPackageInterval(long sendNextPackageInterval) {
        if (sendNextPackageInterval > 70) {
            SEND_NEXT_PACKAGE_INTERVAL = sendNextPackageInterval;
        }
    }
}
