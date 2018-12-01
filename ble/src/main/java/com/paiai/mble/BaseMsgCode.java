package com.paiai.mble;

import android.util.Log;
import android.util.SparseArray;

import com.dcc.ibase.utils.AppUtils;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/10 09 53 星期五<br>
 * 功能描述：<br>消息码
 */

public class BaseMsgCode {

    public static final SparseArray<String> codeMap = new SparseArray<>();

    static {
        String[] lockMsgCodeArr = AppUtils.Companion.getApp().getResources().getStringArray(R.array.base_baseMsgCode);
        try {
            for (String lockMsgCode : lockMsgCodeArr) {
                String[] arr = lockMsgCode.split("#");
                codeMap.put(Integer.parseInt(arr[0]), arr[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换消息码
     */
    public static String parseMessageCode(int msgCode) {
        String originBleMsg = "System Error";
        try {
            String bleString = codeMap.get(msgCode);
            if(bleString != null) {
                String[] bleStringArr = bleString.split("\\|");
                originBleMsg = bleStringArr[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return originBleMsg;
    }

    /**
     * 获取消息码对应的描述信息等级
     */
    private static int getMessageLevel(int msgCode) {
        int bleLevel = Log.INFO;
        try {
            String bleLogLevelString = getMessageLevelTag(msgCode);
            if ("VERBOSE".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.VERBOSE;
            } else if ("DEBUG".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.DEBUG;
            } else if ("INFO".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.INFO;
            } else if ("WARN".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.WARN;
            } else if ("ERROR".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.ERROR;
            } else if ("ASSERT".equalsIgnoreCase(bleLogLevelString)) {
                bleLevel = Log.ASSERT;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bleLevel;
    }

    /**
     * 获取消息码对应的描述信息等级
     */
    public static String getMessageLevelTag(int msgCode) {
        String bleLevelMessage = "INFO";
        try {
            String bleString = codeMap.get(msgCode);
            String[] bleStringArr = bleString.split("\\|");
            bleLevelMessage = bleStringArr[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bleLevelMessage;
    }

    /**
     * 判断消息码表示的消息是否是前台用户需要的消息
     */
    public static boolean logForUser(int msgCode) {
        return getMessageLevel(msgCode) <= Log.INFO;
    }

    /**
     * 判断消息码表示的消息是否是系统的消息
     */
    public static boolean logForSystem(int msgCode) {
        return getMessageLevel(msgCode) > Log.INFO;
    }

    /**
     * 系统中是否能匹配到消息码
     */
    public static boolean msgCodeOk(int msgCode) {
        for (int i = 0; i < codeMap.size(); i++) {
            if (msgCode == codeMap.keyAt(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取消息码表示的消息，如果消息码为非用户消息，则显示为替换消息
     */
    public static String getMessage(int msgCode, String replaceMsg) {
        if (logForUser(msgCode)) {
            return parseMessageCode(msgCode);
        }
        return replaceMsg;
    }
}
