package com.moko.support.pro;


import com.moko.ble.lib.task.OrderTask;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.ParamsKeyEnum;
import com.moko.support.pro.task.ConfigTask;
import com.moko.support.pro.task.GetFirmwareRevisionTask;
import com.moko.support.pro.task.GetMacTask;
import com.moko.support.pro.task.GetManufacturerNameTask;
import com.moko.support.pro.task.GetModelNumberTask;
import com.moko.support.pro.task.GetProductTypeTask;
import com.moko.support.pro.task.GetSoftwareRevisionTask;
import com.moko.support.pro.task.ParamsTask;
import com.moko.support.pro.task.SetPasswordTask;

import androidx.annotation.IntRange;

public class OrderTaskAssembler {

    public static OrderTask getFirmwareVersion() {
        GetFirmwareRevisionTask task = new GetFirmwareRevisionTask();
        return task;
    }

    public static OrderTask getSoftwareRevision() {
        GetSoftwareRevisionTask task = new GetSoftwareRevisionTask();
        return task;
    }

    public static OrderTask getMac() {
        GetMacTask task = new GetMacTask();
        return task;
    }

    public static OrderTask getProductType() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_PRODUCT_TYPE);
        return task;
    }

    public static OrderTask getManufacturerName() {
        GetManufacturerNameTask task = new GetManufacturerNameTask();
        return task;
    }

    public static OrderTask getModelNumber() {
        GetModelNumberTask getMacTask = new GetModelNumberTask();
        return getMacTask;
    }


    public static OrderTask getPassword() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_PASSWORD);
        return task;
    }

    public static OrderTask setPassword(String password) {
        SetPasswordTask task = new SetPasswordTask();
        task.setData(password);
        return task;
    }

    public static OrderTask getAdvName() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_ADV_NAME);
        return task;
    }

    public static OrderTask setAdvName(String advName) {
        ParamsTask task = new ParamsTask();
        task.setAdvName(advName);
        return task;
    }

    public static OrderTask getAdvInterval() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_ADV_INTERVAL);
        return task;
    }

    public static OrderTask setAdvInterval(@IntRange(from = 1, to = 100) int interval) {
        ParamsTask task = new ParamsTask();
        task.setAdvInterval(interval);
        return task;
    }

    public static OrderTask getTxPower() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_TX_POWER);
        return task;
    }

    public static OrderTask setTxPower(int txPower) {
        ParamsTask task = new ParamsTask();
        task.setTxPower(txPower);
        return task;
    }


    public static OrderTask getPasswordVerifyEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_PASSWORD_VERIFY_ENABLE);
        return task;
    }

    public static OrderTask setPasswordVerifyEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setPasswordVerifyEnable(enable);
        return task;
    }

    public static OrderTask getPowerOnDefault() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_POWER_ON_DEFAULT);
        return task;
    }

    public static OrderTask setPowerOnDefault(@IntRange(from = 0, to = 2) int status) {
        ParamsTask task = new ParamsTask();
        task.setPowerOnDefault(status);
        return task;
    }

    public static OrderTask getSwitchReportInterval() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_SWITCH_REPORT_INTERVAL);
        return task;
    }

    public static OrderTask setSwitchReportInterval(@IntRange(from = 10, to = 600) int interval) {
        ParamsTask task = new ParamsTask();
        task.setSwitchReportInterval(interval);
        return task;
    }

    public static OrderTask getPowerReportInterval() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_POWER_REPORT_INTERVAL);
        return task;
    }

    public static OrderTask setPowerReportInterval(@IntRange(from = 1, to = 600) int interval) {
        ParamsTask task = new ParamsTask();
        task.setPowerReportInterval(interval);
        return task;
    }

    public static OrderTask getIndicatorBleAdvStatus() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_INDICATOR_BLE_ADV_STATUS);
        return task;
    }

    public static OrderTask setIndicatorBleAdvStatus(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setIndicatorBleAdvStatus(enable);
        return task;
    }

    public static OrderTask getIndicatorBleConnectedStatus() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_INDICATOR_BLE_CONNECTED_STATUS);
        return task;
    }

    public static OrderTask setIndicatorBleConnectedStatus(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setIndicatorBleConnectedStatus(enable);
        return task;
    }

    public static OrderTask getIndicatorPowerSwitchStatus() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_INDICATOR_POWER_SWITCH_STATUS);
        return task;
    }

    public static OrderTask setIndicatorPowerSwitchStatus(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setIndicatorPowerSwitchStatus(enable);
        return task;
    }

    public static OrderTask getIndicatorPowerProtectionStatus() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_INDICATOR_POWER_PROTECTION_STATUS);
        return task;
    }

    public static OrderTask setIndicatorPowerProtectionStatus(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setIndicatorPowerProtectionStatus(enable);
        return task;
    }

    public static OrderTask getButtonResetEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_BUTTON_RESET_ENABLE);
        return task;
    }

    public static OrderTask setButtonResetEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setButtonResetEnable(enable);
        return task;
    }

    public static OrderTask getButtonControlEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_BUTTON_CONTROL_ENABLE);
        return task;
    }

    public static OrderTask setButtonControlEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setButtonControlEnable(enable);
        return task;
    }

    public static OrderTask getClearEnergyEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_CLEAR_ENERGY_ENABLE);
        return task;
    }

    public static OrderTask setClearEnergyEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setClearEnergyEnable(enable);
        return task;
    }

    public static OrderTask getConnectEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_CONNECT_STATUS);
        return task;
    }

    public static OrderTask setConnectEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setConnectEnable(enable);
        return task;
    }

    public static OrderTask getEnergySavedInterval() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_ENERGY_SAVED_INTERVAL);
        return task;
    }

    public static OrderTask setEnergySavedInterval(@IntRange(from = 1, to = 60) int interval) {
        ParamsTask task = new ParamsTask();
        task.setEnergySavedInterval(interval);
        return task;
    }

    public static OrderTask getPowerChangeThreshold() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_POWER_CHANGE_THRESHOLD);
        return task;
    }

    public static OrderTask setPowerChangeThreshold(@IntRange(from = 1, to = 100) int threshold) {
        ParamsTask task = new ParamsTask();
        task.setPowerChangeThreshold(threshold);
        return task;
    }

    public static OrderTask getOverLoadProtection() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_OVER_LOAD_PROTECTION);
        return task;
    }

    public static OrderTask setOverLoadProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                                  @IntRange(from = 1, to = 30) int time) {
        ParamsTask task = new ParamsTask();
        task.setOverLoadProtection(enable, value, time);
        return task;
    }

    public static OrderTask getOverVoltageProtection() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_OVER_VOLTAGE_PROTECTION);
        return task;
    }

    public static OrderTask setOverVoltageProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                                     @IntRange(from = 1, to = 30) int time) {
        ParamsTask task = new ParamsTask();
        task.setOverVoltageProtection(enable, value, time);
        return task;
    }

    public static OrderTask getSagVoltageProtection() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_SAG_VOLTAGE_PROTECTION);
        return task;
    }

    public static OrderTask setSagVoltageProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                                    @IntRange(from = 1, to = 30) int time) {
        ParamsTask task = new ParamsTask();
        task.setSagVoltageProtection(enable, value, time);
        return task;
    }

    public static OrderTask getOverCurrentProtection() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_OVER_CURRENT_PROTECTION);
        return task;
    }

    public static OrderTask setOverCurrentProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                                     @IntRange(from = 1, to = 30) int time) {
        ParamsTask task = new ParamsTask();
        task.setOverCurrentProtection(enable, value, time);
        return task;
    }

    public static OrderTask getPowerIndicatorColor() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_POWER_INDICATOR);
        return task;
    }

    public static OrderTask setPowerIndicatorColor(int option, int blue, int green, int yellow, int orange, int red, int purple) {
        ParamsTask task = new ParamsTask();
        task.setPowerIndicatorColor(option, blue, green, yellow, orange, red, purple);
        return task;
    }

    public static OrderTask getLoadNotifySwitch() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_ENERGY_SAVED_INTERVAL);
        return task;
    }

    public static OrderTask setLoadNotifySwitch(@IntRange(from = 0, to = 1) int addEnable,
                                                @IntRange(from = 0, to = 1) int removeEnable) {
        ParamsTask task = new ParamsTask();
        task.setLoadNotifySwitch(addEnable, removeEnable);
        return task;
    }

    public static OrderTask getSwitchStatus() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_SWITCH_STATUS);
        return task;
    }

    public static OrderTask setSwitchStatus(@IntRange(from = 0, to = 1) int onOff) {
        ConfigTask task = new ConfigTask();
        task.setSwitchStatus(onOff);
        return task;
    }

    public static OrderTask getPowerData() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_POWER_DATA);
        return task;
    }

    public static OrderTask getOverLoad() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_OVER_LOAD);
        return task;
    }

    public static OrderTask setOverLoadClear() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_OVER_LOAD_CLEAR);
        return task;
    }

    public static OrderTask getOverCurrent() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_OVER_CURRENT);
        return task;
    }

    public static OrderTask setOverCurrentClear() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_OVER_CURRENT_CLEAR);
        return task;
    }

    public static OrderTask getOverVoltage() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_OVER_VOLTAGE);
        return task;
    }

    public static OrderTask setOverVoltageClear() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_OVER_VOLTAGE_CLEAR);
        return task;
    }

    public static OrderTask getSagVoltage() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_SAG_VOLTAGE);
        return task;
    }

    public static OrderTask setSagVoltageClear() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_SAG_VOLTAGE_CLEAR);
        return task;
    }

    public static OrderTask setCountdown(@IntRange(from = 1, to = 86400) int countdown) {
        ConfigTask task = new ConfigTask();
        task.setCountdown(countdown);
        return task;
    }

    public static OrderTask getEnergyTotally() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_ENERGY_TOTALLY);
        return task;
    }

    public static OrderTask getEnergyDaily() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_ENERGY_DAILY);
        return task;
    }

    public static OrderTask getEnergyHourly() {
        ConfigTask task = new ConfigTask();
        task.getData(ConfigKeyEnum.KEY_ENERGY_HOURLY);
        return task;
    }

    public static OrderTask setEnergyClear() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_ENERGY_CLEAR);
        return task;
    }

    public static OrderTask setSystemTime() {
        ConfigTask task = new ConfigTask();
        task.setSystemTime();
        return task;
    }

    public static OrderTask reset() {
        ConfigTask task = new ConfigTask();
        task.setData(ConfigKeyEnum.KEY_RESET);
        return task;
    }
}
