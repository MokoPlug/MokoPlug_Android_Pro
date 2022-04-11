package com.moko.mokoplugpro.utils;

import android.util.SparseArray;

import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.entity.PlugInfo;
import com.moko.support.pro.entity.DeviceInfo;
import com.moko.support.pro.service.DeviceInfoAnalysis;

import java.util.Arrays;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class PlugInfoAnalysisImpl implements DeviceInfoAnalysis<PlugInfo> {
    @Override
    public PlugInfo parseDeviceInfo(DeviceInfo deviceInfo) {
        ScanResult scanResult = deviceInfo.scanResult;
        SparseArray<byte[]> manufacturer = scanResult.getScanRecord().getManufacturerSpecificData();
        if (manufacturer == null || manufacturer.size() == 0) {
            return null;
        }
        int manufacturerId = manufacturer.keyAt(0);
        byte[] manufacturerData = manufacturer.get(manufacturerId);
        if (manufacturerData.length != 23)
            return null;
        String voltage = "";
        String current = "";
        String power = "";
        String powerFactor = "";
        String currentRate = "";
        String energyTotal = "";
        String txPower = "";
        int onOff = 0;
        int loadState = 0;
        int overLoad = 0;
        int overCurrent = 0;
        int overVoltage = 0;
        int sagVoltage = 0;
        int isNeedVerify = 0;
        int isConnectable = 0;
        byte[] voltageBytes = Arrays.copyOfRange(manufacturerData, 6, 8);
        byte[] currentBytes = Arrays.copyOfRange(manufacturerData, 8, 10);
        byte[] powerBytes = Arrays.copyOfRange(manufacturerData, 10, 14);
        powerFactor = String.valueOf(manufacturerData[14] & 0xFF);
        byte[] currentRateBytes = Arrays.copyOfRange(manufacturerData, 15, 17);
        byte[] energyTotalBytes = Arrays.copyOfRange(manufacturerData, 17, 21);
        txPower = String.valueOf(manufacturerData[21]);
        int state = manufacturerData[22] & 0xFF;
        voltage = MokoUtils.getDecimalFormat("0.#").format(MokoUtils.toInt(voltageBytes) * 0.1f);
        current = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toIntSigned(currentBytes) * 0.001f);
        power = MokoUtils.getDecimalFormat("0.#").format(MokoUtils.toIntSigned(powerBytes) * 0.1f);
        currentRate = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(currentRateBytes) * 0.01f);
        energyTotal = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyTotalBytes) * 0.01f);
        if ((state & 0x01) == 0x01)
            isConnectable = 1;
        if ((state & 0x02) == 0x02)
            isNeedVerify = 1;
        if ((state & 0x04) == 0x04)
            sagVoltage = 1;
        if ((state & 0x08) == 0x08)
            overVoltage = 1;
        if ((state & 0x10) == 0x10)
            overCurrent = 1;
        if ((state & 0x20) == 0x20)
            overLoad = 1;
        if ((state & 0x40) == 0x40)
            loadState = 1;
        if ((state & 0x80) == 0x80)
            onOff = 1;
        PlugInfo plugInfo = new PlugInfo();
        plugInfo.name = deviceInfo.name;
        plugInfo.mac = deviceInfo.mac;
        plugInfo.rssi = deviceInfo.rssi;
        plugInfo.voltage = voltage;
        plugInfo.current = current;
        plugInfo.power = power;
        plugInfo.powerFactor = powerFactor;
        plugInfo.currentRate = currentRate;
        plugInfo.energyTotal = energyTotal;
        plugInfo.txPower = txPower;
        plugInfo.onOff = onOff;
        plugInfo.loadState = loadState;
        plugInfo.overLoad = overLoad;
        plugInfo.overCurrent = overCurrent;
        plugInfo.overVoltage = overVoltage;
        plugInfo.sagVoltage = sagVoltage;
        plugInfo.isNeedVerify = isNeedVerify;
        plugInfo.isConnectable = isConnectable;
        return plugInfo;
    }
}
