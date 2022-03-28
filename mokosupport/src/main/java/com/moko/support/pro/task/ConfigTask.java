package com.moko.support.pro.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;

import java.util.Calendar;

import androidx.annotation.IntRange;

public class ConfigTask extends OrderTask {
    public byte[] data;

    public ConfigTask() {
        super(OrderCHAR.CHAR_CONFIG, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void getData(ConfigKeyEnum key) {
        switch (key) {
            case KEY_SWITCH_STATUS:
            case KEY_POWER_DATA:
            case KEY_OVER_LOAD:
            case KEY_OVER_CURRENT:
            case KEY_OVER_VOLTAGE:
            case KEY_SAG_VOLTAGE:
            case KEY_OVER_LOAD_CLEAR:
            case KEY_OVER_CURRENT_CLEAR:
            case KEY_OVER_VOLTAGE_CLEAR:
            case KEY_SAG_VOLTAGE_CLEAR:
            case KEY_ENERGY_TOTALLY:
            case KEY_ENERGY_DAILY:
            case KEY_ENERGY_HOURLY:
            case KEY_ENERGY_CLEAR:
            case KEY_SYSTEM_TIME:
            case KEY_RESET:
                createGetConfigData(key.getConfigKey());
                break;
        }
    }

    public void setData(ConfigKeyEnum key) {
        switch (key) {
            case KEY_OVER_LOAD_CLEAR:
            case KEY_OVER_CURRENT_CLEAR:
            case KEY_OVER_VOLTAGE_CLEAR:
            case KEY_SAG_VOLTAGE_CLEAR:
            case KEY_ENERGY_CLEAR:
            case KEY_RESET:
                createSetConfigData(key.getConfigKey());
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

    private void createSetConfigData(int configKey) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) configKey,
                (byte) 0x00
        };
        response.responseValue = data;
    }


    public void setCountdown(@IntRange(from = 1, to = 86400) int countdown) {
        byte[] countdownBytes = MokoUtils.toByteArray(countdown, 4);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ConfigKeyEnum.KEY_COUNT_DOWN.getConfigKey(),
                (byte) 0x04,
                countdownBytes[0],
                countdownBytes[1],
                countdownBytes[2],
                countdownBytes[3],
        };
        response.responseValue = data;
    }

    public void setSystemTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        byte[] yearBytes = MokoUtils.toByteArray(year, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ConfigKeyEnum.KEY_SYSTEM_TIME.getConfigKey(),
                (byte) 0x07,
                yearBytes[0],
                yearBytes[1],
                (byte) month,
                (byte) date,
                (byte) hour,
                (byte) minute,
                (byte) second
        };
        response.responseValue = data;
    }

    public void setSwitchStatus(@IntRange(from = 0, to = 1) int onOff) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ConfigKeyEnum.KEY_SWITCH_STATUS.getConfigKey(),
                (byte) 0x01,
                (byte) onOff
        };
        response.responseValue = data;
    }
}
