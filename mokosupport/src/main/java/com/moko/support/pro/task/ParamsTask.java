package com.moko.support.pro.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.ParamsKeyEnum;

import androidx.annotation.IntRange;

public class ParamsTask extends OrderTask {
    public byte[] data;

    public ParamsTask() {
        super(OrderCHAR.CHAR_PARAMS, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(ParamsKeyEnum key) {
        switch (key) {
            case KEY_ADV_NAME:
            case KEY_PASSWORD_VERIFY_ENABLE:
            case KEY_PASSWORD:
            case KEY_POWER_ON_DEFAULT:
            case KEY_SWITCH_REPORT_INTERVAL:
            case KEY_POWER_REPORT_INTERVAL:
            case KEY_INDICATOR_BLE_ADV_STATUS:
            case KEY_INDICATOR_BLE_CONNECTED_STATUS:
            case KEY_INDICATOR_POWER_SWITCH_STATUS:
            case KEY_INDICATOR_POWER_PROTECTION_STATUS:
            case KEY_TX_POWER:
            case KEY_BUTTON_RESET_ENABLE:
            case KEY_BUTTON_CONTROL_ENABLE:
            case KEY_CLEAR_ENERGY_ENABLE:
            case KEY_PRODUCT_TYPE:
            case KEY_ADV_INTERVAL:
            case KEY_CONNECT_STATUS:
            case KEY_ENERGY_SAVED_INTERVAL:
            case KEY_POWER_CHANGE_THRESHOLD:
            case KEY_OVER_LOAD_PROTECTION:
            case KEY_OVER_VOLTAGE_PROTECTION:
            case KEY_SAG_VOLTAGE_PROTECTION:
            case KEY_OVER_CURRENT_PROTECTION:
            case KEY_POWER_INDICATOR:
            case KEY_LOAD_SWITCH:
                createGetConfigData(key.getParamsKey());
                break;
        }
    }

    private void createGetConfigData(int configKey) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x00,
                (byte) configKey,
                (byte) 0x00
        };
        response.responseValue = data;
    }

    public void setAdvName(String name) {
        byte[] dataBytes = name.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_ADV_NAME.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setPasswordVerifyEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_PASSWORD_VERIFY_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setPowerOnDefault(@IntRange(from = 0, to = 2) int status) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_POWER_ON_DEFAULT.getParamsKey(),
                (byte) 0x01,
                (byte) status
        };
        response.responseValue = data;
    }

    public void setSwitchReportInterval(@IntRange(from = 10, to = 600) int interval) {
        byte[] dataBytes = MokoUtils.toByteArray(interval, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_SWITCH_REPORT_INTERVAL.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
        response.responseValue = data;
    }

    public void setPowerReportInterval(@IntRange(from = 1, to = 600) int interval) {
        byte[] dataBytes = MokoUtils.toByteArray(interval, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_POWER_REPORT_INTERVAL.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
        response.responseValue = data;
    }

    public void setIndicatorBleAdvStatus(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_INDICATOR_BLE_ADV_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setIndicatorBleConnectedStatus(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_INDICATOR_BLE_CONNECTED_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setIndicatorPowerSwitchStatus(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_INDICATOR_POWER_SWITCH_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setIndicatorPowerProtectionStatus(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_INDICATOR_POWER_PROTECTION_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setTxPower(int txPower) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_TX_POWER.getParamsKey(),
                (byte) 0x01,
                (byte) txPower
        };
        response.responseValue = data;
    }

    public void setButtonResetEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_BUTTON_RESET_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setButtonControlEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_BUTTON_CONTROL_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setClearEnergyEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_CLEAR_ENERGY_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setAdvInterval(@IntRange(from = 1, to = 100) int interval) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_ADV_INTERVAL.getParamsKey(),
                (byte) 0x01,
                (byte) interval
        };
        response.responseValue = data;
    }

    public void setConnectEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_CONNECT_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setEnergySavedInterval(@IntRange(from = 1, to = 60) int interval) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_ENERGY_SAVED_INTERVAL.getParamsKey(),
                (byte) 0x01,
                (byte) interval
        };
        response.responseValue = data;
    }

    public void setPowerChangeThreshold(@IntRange(from = 1, to = 100) int threshold) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_POWER_CHANGE_THRESHOLD.getParamsKey(),
                (byte) 0x01,
                (byte) threshold
        };
        response.responseValue = data;
    }

    public void setOverLoadProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                      @IntRange(from = 1, to = 30) int time) {

        byte[] dataBytes = MokoUtils.toByteArray(value, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_OVER_LOAD_PROTECTION.getParamsKey(),
                (byte) 0x04,
                (byte) enable,
                dataBytes[0],
                dataBytes[1],
                (byte) time
        };
        response.responseValue = data;
    }

    public void setOverVoltageProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                         @IntRange(from = 1, to = 30) int time) {

        byte[] dataBytes = MokoUtils.toByteArray(value, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_OVER_VOLTAGE_PROTECTION.getParamsKey(),
                (byte) 0x04,
                (byte) enable,
                dataBytes[0],
                dataBytes[1],
                (byte) time
        };
        response.responseValue = data;
    }

    public void setSagVoltageProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                        @IntRange(from = 1, to = 30) int time) {

        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_SAG_VOLTAGE_PROTECTION.getParamsKey(),
                (byte) 0x03,
                (byte) enable,
                (byte) value,
                (byte) time
        };
        response.responseValue = data;
    }

    public void setOverCurrentProtection(@IntRange(from = 0, to = 1) int enable, int value,
                                         @IntRange(from = 1, to = 30) int time) {

        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_OVER_CURRENT_PROTECTION.getParamsKey(),
                (byte) 0x03,
                (byte) enable,
                (byte) value,
                (byte) time
        };
        response.responseValue = data;
    }

    public void setPowerIndicatorColor(int option, int blue, int green, int yellow, int orange, int red, int purple) {
        byte[] blueBytes = MokoUtils.toByteArray(blue, 2);
        byte[] greenBytes = MokoUtils.toByteArray(green, 2);
        byte[] yellowBytes = MokoUtils.toByteArray(yellow, 2);
        byte[] orangeBytes = MokoUtils.toByteArray(orange, 2);
        byte[] redBytes = MokoUtils.toByteArray(red, 2);
        byte[] purpleBytes = MokoUtils.toByteArray(purple, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_POWER_INDICATOR.getParamsKey(),
                (byte) 0x0D,
                (byte) option,
                blueBytes[0],
                blueBytes[1],
                greenBytes[0],
                greenBytes[1],
                yellowBytes[0],
                yellowBytes[1],
                orangeBytes[0],
                orangeBytes[1],
                redBytes[0],
                redBytes[1],
                purpleBytes[0],
                purpleBytes[1]
        };
    }

    public void setLoadNotifySwitch(@IntRange(from = 0, to = 1) int addEnable,
                                    @IntRange(from = 0, to = 1) int removeEnable) {

        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_LOAD_SWITCH.getParamsKey(),
                (byte) 0x02,
                (byte) addEnable,
                (byte) removeEnable
        };
        response.responseValue = data;
    }
}
