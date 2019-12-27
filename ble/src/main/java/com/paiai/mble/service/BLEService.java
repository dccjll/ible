package com.paiai.mble.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import com.dcc.ibase.log.LogManager;
import com.paiai.mble.BLEConfig;
import com.paiai.mble.BLEManage;
import com.paiai.mble.BLEMsgCode;

import java.util.Map;


/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 13:27<br>
 * 功能描述：蓝牙监控服务<br>
 */
public class BLEService extends Service/* implements RxUtils.EventHandle<String>*/ {

	private final static String TAG = BLEService.class.getSimpleName();
	private static final String CHANNEL_ID = "bleService";
	private boolean continueRunning;
	/*public static final String BLESERVICE_DESTORY = "com.paiai.ble.core.manage.BLESERVICE_DESTORY";//关闭服务的事件标记*/
	public static final int DISCONNECT_REQUEST_INTERVAL = 5000;//请求断开的时间间隔,单位ms
	private Handler disconnectHandler;
	private Runnable disconnectRunnable = new Runnable() {
		@Override
		public void run() {
			if (!continueRunning) {
				LogManager.Companion.i(TAG, "服务已暂停");
				return;
			}
			if (!BLEConfig.AUTO_DISCONNECT_WHEN_NO_DATA_INTERACTION) {
				LogManager.Companion.i(TAG, "已设置为不主动断开连接");
				return;
			}
			disconnectExpiredGatt();
			try {
				Thread.sleep(DISCONNECT_REQUEST_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			disconnectHandler.post(this);
		}
	};

	public static final String FLAG_STOP = "com.dsm.ible.impl.dwy.service.BLEService.Stop";
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) {
				return;
			}
			if (FLAG_STOP.equalsIgnoreCase(intent.getAction())) {
				LogManager.Companion.i(TAG, "请求停止蓝牙监控服务");
				continueRunning = false;
				//关闭所有打开的蓝牙连接
				BLEManage.disconnectAllBluetoothGatt();
				stopSelf();
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		new BLEMsgCode();
		continueRunning = true;
		/*RxUtils.subscribe(this, String.class, this, BLESERVICE_DESTORY);*/
		//如果某一个连接超过规定的时间仍然没有通讯的话，主动断开连接
		IntentFilter intentFilter = new IntentFilter(FLAG_STOP);
		registerReceiver(broadcastReceiver, intentFilter);
		new Thread(() -> {
			Looper.prepare();
			disconnectHandler = new Handler();
			disconnectHandler.post(disconnectRunnable);
			Looper.loop();
		}).start();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "小嘀管家前台蓝牙服务", NotificationManager.IMPORTANCE_HIGH);
			notificationChannel.enableLights(false);
			notificationChannel.setLightColor(Color.RED);
			notificationChannel.setShowBadge(false);
			notificationChannel.enableVibration(false);
			notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(notificationChannel);
			}
			startForeground(168, new NotificationCompat.Builder(this, CHANNEL_ID).build());
		}

		LogManager.Companion.i(TAG, "蓝牙监控服务已启动");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
				"小嘀管家前台蓝牙服务", NotificationManager.IMPORTANCE_HIGH);
			notificationChannel.enableLights(false);
			notificationChannel.setLightColor(Color.RED);
			notificationChannel.setShowBadge(false);
			notificationChannel.enableVibration(false);
			notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(notificationChannel);
			}
			startForeground(168, new NotificationCompat.Builder(this, CHANNEL_ID).build());
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		/*RxUtils.unSubscribe(this);*/
		unregisterReceiver(broadcastReceiver);
		LogManager.Companion.i(TAG, "蓝牙监控服务已停止");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*@Override
	public void accept(String s) {
		if (BLESERVICE_DESTORY.equalsIgnoreCase(s)) {
			LogManager.Companion.i(TAG, "请求停止蓝牙监控服务");
			continueRunning = false;
			//关闭所有打开的蓝牙连接
			BLEManage.disconnectAllBluetoothGatt();
			stopSelf();
		}
	}*/

	/**
	 * 断开过期的连接
	 */
	private void disconnectExpiredGatt() {
		if(BLEManage.connectedBluetoothGattList != null){
			for (Map<String,Object> map: BLEManage.connectedBluetoothGattList) {
				long timeInterval = System.currentTimeMillis() - (Long)map.get("connectedTime");
				if(timeInterval >= BLEConfig.AUTO_DISCONNECT_INTERVAL_WHEN_NO_DATA_INTERACTION){
					BluetoothGatt bluetoothGatt = (BluetoothGatt) map.get("bluetoothGatt");
					LogManager.Companion.i(TAG, "连接" + bluetoothGatt.getDevice().getAddress() + "超过规定的时间仍然没有通讯，主动断开并关闭连接");
					BLEManage.disconnect(bluetoothGatt);
				}
			}
		}
		if(BLEManage.bluetoothDeviceList != null){
			for (Map<String, Object> map : BLEManage.bluetoothDeviceList) {
				long timeInterval = System.currentTimeMillis() - (Long) map.get("foundTime");
				if (timeInterval >= BLEConfig.DEVICE_MAX_CACHED_TIME) {
					String deviceMac = ((BluetoothDevice) map.get("bluetoothDevice")).getAddress();
					LogManager.Companion.i(TAG, "设备" + deviceMac + "超过规定的缓存时间，自动清除");
					BLEManage.bluetoothDeviceList.remove(map);
				}
			}
		}
	}
}