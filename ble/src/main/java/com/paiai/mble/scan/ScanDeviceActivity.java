package com.paiai.mble.scan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.paiai.mble.R;

/**
 * 作者：dccjll<br>
 * 创建时间：2017/11/6 14:30<br>
 * 功能描述：<br>
 * 扫描设备的界面<br>
 * 可以接收三个参数：<br>
 * longScanFlag(Boolean) 为true则一直扫描，不停止<br>
 * bleDeviceType(Integer) 扫描设备的过滤器，匹配BluetoothDevice对象的getType类型<br>
 * deviceNameStartWith(String) 扫描设备的过滤器，匹配设备名称开始的字符串<br>
 */
public class ScanDeviceActivity extends Activity {
    private boolean longScanFlag;//长扫描标记
    private int bleDeviceType;//设备类型
    private String deviceNameStartWith;//设备名称开始的信息
    private ScanDevice scanDevice;
    /*private CommonRVAdapter<BluetoothDevice> commonRVAdapter;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mble_activity_scan_device);

        Intent intent = getIntent();
        if (intent != null) {
            longScanFlag = intent.getBooleanExtra("longScanFlag", false);
            bleDeviceType = intent.getIntExtra("bleDeviceType", 0);
            deviceNameStartWith = intent.getStringExtra("deviceNameStartWith");
        }
        /*BLEHelper.requestLocationPermissionAndFeature(new BLEHelper.OnLocationPermissionAndFeatureCheckListener() {
            @Override
            public void onSuccess() {
                initRvList();
                scanDevice();
            }

            @Override
            public void onFail(int errorCode) {
                Toast.makeText(Utils.getApp(), "need location permission and feature", Toast.LENGTH_LONG).show();
            }

            @Override
            public void showRationaleDialog(PermissionUtils.OnRationaleListener.ShouldRequest shouldRequest) {
                shouldRequest.again(true);
            }

            @Override
            public void confirmShowPermissionSettingTransActivity(PermissionHelper.OnPermissionConfirmListener onPermissionConfirmListener) {
                onPermissionConfirmListener.onAgree();
            }

            @Override
            public void confirmShowLocationSettingTransActivity(PermissionHelper.OnPermissionConfirmListener onPermissionConfirmListener) {
                onPermissionConfirmListener.onAgree();
            }
        });*/
        initRvList();
        scanDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanDevice != null) {
            scanDevice.stopScan();
        }
    }

    private void initRvList() {
        /*RecyclerView deviceListRv = (RecyclerView) findViewById(R.id.deviceListRv);
        deviceListRv.setLayoutManager(new LinearLayoutManager(this));
        commonRVAdapter = new CommonRVAdapter<BluetoothDevice>(this, null, android.R.layout.simple_list_item_2, android.R.layout.simple_list_item_1) {
            @Override
            public void onBindNullDataViewHolder(RecyclerView.Adapter adapter, RVViewHolder rvViewHolder, int position, BluetoothDevice entry, List<BluetoothDevice> data) {
                ((TextView)rvViewHolder.findViewById(android.R.id.text1)).setText(BLEMsgCode.parseMessageCode(-10009));
            }

            @Override
            public void onBindViewHolder(RecyclerView.Adapter adapter, RVViewHolder rvViewHolder, int position, final BluetoothDevice entry, List<BluetoothDevice> data) {
                ((TextView)rvViewHolder.findViewById(android.R.id.text1)).setText(entry.getName());
                ((TextView)rvViewHolder.findViewById(android.R.id.text2)).setText(entry.getAddress());
                ((View)rvViewHolder.findViewById(android.R.id.text1).getParent()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.putExtra("deviceName", entry.getName());
                        intent.putExtra("deviceMac", entry.getAddress());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }

            @Override
            public BluetoothDevice getSelectEntry() {
                return null;
            }
        };
        deviceListRv.setAdapter(commonRVAdapter);*/
    }

    private void scanDevice() {
        /*scanDevice = new ScanDevice();
        scanDevice.setLongScanFlag(longScanFlag).setType(bleDeviceType).setDeviceNameStartWithString(deviceNameStartWith).setOnBLEScanListener(new BLEScan.OnBLEScanListener() {
            @Override
            public void onFoundDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                commonRVAdapter.addFlush(bluetoothDevice);
            }

            @Override
            public void onScanFinish(List<Map<String, Object>> bluetoothDeviceList) {

            }

            @Override
            public void onScanFail(int errorCode) {
                ToastUtils.showLong(BLEMsgCode.parseMessageCode(errorCode));
            }
        }).startScan();*/
    }
}
