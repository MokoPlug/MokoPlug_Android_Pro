package com.moko.support.pro.entity;

import java.io.Serializable;


public enum ConfigKeyEnum implements Serializable {
    KEY_SWITCH_STATUS(0x51),
    KEY_POWER_DATA(0x52),
    KEY_OVER_LOAD(0x53),
    KEY_OVER_CURRENT(0x54),
    KEY_OVER_VOLTAGE(0x55),
    KEY_SAG_VOLTAGE(0x56),
    KEY_OVER_LOAD_CLEAR(0x57),
    KEY_OVER_CURRENT_CLEAR(0x58),
    KEY_OVER_VOLTAGE_CLEAR(0x59),
    KEY_SAG_VOLTAGE_CLEAR(0x5A),
    KEY_COUNT_DOWN(0x5B),
    KEY_ENERGY_TOTALLY(0x5C),
    KEY_ENERGY_DAILY(0x5D),
    KEY_ENERGY_HOURLY(0x5E),
    KEY_ENERGY_CLEAR(0x5F),
    KEY_SYSTEM_TIME(0x61),
    KEY_RESET(0x62),
    ;


    private int configKey;

    ConfigKeyEnum(int configKey) {
        this.configKey = configKey;
    }


    public int getConfigKey() {
        return configKey;
    }

    public static ConfigKeyEnum fromConfigKey(int configKey) {
        for (ConfigKeyEnum configKeyEnum : ConfigKeyEnum.values()) {
            if (configKeyEnum.getConfigKey() == configKey) {
                return configKeyEnum;
            }
        }
        return null;
    }
}
