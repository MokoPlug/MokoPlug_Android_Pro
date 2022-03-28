package com.moko.support.pro;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoBleManager;
import com.moko.ble.lib.callback.MokoResponseCallback;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.OrderServices;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

final class MokoBleConfig extends MokoBleManager {

    private MokoResponseCallback mMokoResponseCallback;
    private BluetoothGattCharacteristic passwordCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BluetoothGattCharacteristic paramsCharacteristic;
    private BluetoothGattCharacteristic configCharacteristic;

    public MokoBleConfig(@NonNull Context context, MokoResponseCallback callback) {
        super(context);
        mMokoResponseCallback = callback;
    }

    @Override
    public boolean init(BluetoothGatt gatt) {
        final BluetoothGattService service = gatt.getService(OrderServices.SERVICE_CUSTOM.getUuid());
        if (service != null) {
            passwordCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_PASSWORD.getUuid());
            notifyCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_NOTIFY.getUuid());
            configCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_CONFIG.getUuid());
            paramsCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_PARAMS.getUuid());
            enablePasswordNotify();
            enableNotify();
            enableConfigNotify();
            enableParamNotify();
            return true;
        }
        return false;
    }

    @Override
    public void write(BluetoothGattCharacteristic characteristic, byte[] value) {
    }

    @Override
    public void read(BluetoothGattCharacteristic characteristic, byte[] value) {
        mMokoResponseCallback.onCharacteristicRead(characteristic, value);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void discovered(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID lastCharacteristicUUID = characteristic.getUuid();
        if (paramsCharacteristic.getUuid().equals(lastCharacteristicUUID)) {
            gatt.requestMtu(247);
            mMokoResponseCallback.onServicesDiscovered(gatt);
        }
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }

    public void enablePasswordNotify() {
        setIndicationCallback(passwordCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(passwordCharacteristic, value);
        });
        enableNotifications(passwordCharacteristic).enqueue();
    }

    public void disablePasswordNotify() {
        disableNotifications(passwordCharacteristic).enqueue();
    }

    public void enableNotify() {
        setIndicationCallback(notifyCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(notifyCharacteristic, value);
        });
        enableNotifications(notifyCharacteristic).enqueue();
    }

    public void enableParamNotify() {
        setIndicationCallback(paramsCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(paramsCharacteristic, value);
        });
        enableNotifications(paramsCharacteristic).enqueue();
    }

    public void disableParamNotify() {
        disableNotifications(paramsCharacteristic).enqueue();
    }

    public void enableConfigNotify() {
        setIndicationCallback(configCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(configCharacteristic, value);
        });
        enableNotifications(configCharacteristic).enqueue();
    }

    public void disableConfigNotify() {
        disableNotifications(configCharacteristic).enqueue();
    }
}