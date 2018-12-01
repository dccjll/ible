package com.paiai.mble.dfu;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:36<br>
 * 功能描述：<br>
 * 简单的DFU升级监听器<br/>
 * 包装层适用
 */
public interface OnSimpleDfuProgressListener {

    /**
     * 发送命令到设备上，让设备进入固件更新模式
     * @param deviceMac 设备mac地址
     * @param onInputFirewareUpdateModeListener 进入固件更新模式指令发送成功后的监听器
     */
    void inputFirewareUpdateMode(String deviceMac, DeviceFirewareUpdate.OnInputFirewareUpdateModeListener onInputFirewareUpdateModeListener);

    /**
     *  开始更新
     * @param deviceAddress 设备mac地址
     */
    void onDfuProcessStarting(final String deviceAddress);

    /**
     * 更新完成
     * @param deviceAddress 设备mac地址
     */
    void onDfuCompleted(final String deviceAddress);

    /**
     *  正在更新
     * @param deviceAddress 设备mac地址
     * @param percent   当前进度值
     * @param speed     当前更新速度
     * @param avgSpeed  平均更新速度
     * @param currentPart   当前更新的片区索引
     * @param partsTotal    总片区数
     */
    void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal);

    /**
     * 更新失败
     * @param deviceAddress 设备mac地址
     * @param errorCode 错误码
     */
    void onError(final String deviceAddress, final int errorCode);
}
