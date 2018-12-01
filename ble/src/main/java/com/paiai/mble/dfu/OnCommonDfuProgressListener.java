package com.paiai.mble.dfu;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:35<br>
 * 功能描述：<br>
 * 通用的DFU升级监听器<br/>
 * 最外层适用<br>
 */
public interface OnCommonDfuProgressListener {

    /**
     *  开始更新
     * @param deviceAddress 设备mac地址
     */
    void onDfuProcessStarting(final String deviceAddress);

    /**
     * 更新完成
     * @param deviceAddress 设备mac地址
     * @param newFirewareVersionOnDevice    更新的硬件版本号
     */
    void onDfuCompleted(final String deviceAddress, final String newFirewareVersionOnDevice, final boolean syncStateOnServer);

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
