package com.moko.support.pro.entity;

import java.io.Serializable;


public enum ParamsKeyEnum implements Serializable {
    KEY_ADV_NAME(0x11),
    KEY_PASSWORD_VERIFY_ENABLE(0x12),
    KEY_PASSWORD(0x13),
    KEY_POWER_ON_DEFAULT(0x14),
    KEY_SWITCH_REPORT_INTERVAL(0x15),
    KEY_POWER_REPORT_INTERVAL(0x16),
    KEY_INDICATOR_BLE_ADV_STATUS(0x17),
    KEY_INDICATOR_BLE_CONNECTED_STATUS(0x18),
    KEY_INDICATOR_POWER_SWITCH_STATUS(0x19),
    KEY_INDICATOR_POWER_PROTECTION_STATUS(0x1A),
    KEY_TX_POWER(0x1B),
    KEY_BUTTON_RESET_ENABLE(0x1C),
    KEY_BUTTON_CONTROL_ENABLE(0x1D),
    KEY_CLEAR_ENERGY_ENABLE(0x1E),
    KEY_PRODUCT_TYPE(0x21),
    KEY_ADV_INTERVAL(0x31),
    KEY_CONNECT_STATUS(0x32),
    KEY_ENERGY_SAVED_INTERVAL(0x33),
    KEY_POWER_CHANGE_THRESHOLD(0x34),
    KEY_OVER_LOAD_PROTECTION(0x35),
    KEY_OVER_VOLTAGE_PROTECTION(0x36),
    KEY_SAG_VOLTAGE_PROTECTION(0x37),
    KEY_OVER_CURRENT_PROTECTION(0x38),
    KEY_POWER_INDICATOR(0x39),
    KEY_LOAD_SWITCH(0x3A),
    ;

    private int paramsKey;

    ParamsKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsKeyEnum fromParamKey(int paramsKey) {
        for (ParamsKeyEnum paramsKeyEnum : ParamsKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
