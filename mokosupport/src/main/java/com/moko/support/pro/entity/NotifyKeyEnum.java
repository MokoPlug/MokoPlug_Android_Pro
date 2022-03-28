package com.moko.support.pro.entity;

import java.io.Serializable;


public enum NotifyKeyEnum implements Serializable {
    KEY_DISCONNECT(0x01),
    KEY_SWITCH_STATUS(0x02),
    KEY_POWER_DATA(0x03),
    KEY_LOAD_STATUS(0x04),
    KEY_ENERGY_TOTAL(0x05),
    KEY_ENERGY_DAILY(0x06),
    KEY_ENERGY_HOURLY(0x07),
    KEY_OVER_LOAD(0x08),
    KEY_OVER_CURRENT(0x09),
    KEY_OVER_VOLTAGE(0x0A),
    KEY_SAG_VOLTAGE(0x0B),
    KEY_COUNT_DOWN(0x0C),
    ;


    private int notifyKey;

    NotifyKeyEnum(int notifyKey) {
        this.notifyKey = notifyKey;
    }


    public int getNotifyKey() {
        return notifyKey;
    }

    public static NotifyKeyEnum fromNotifyKey(int notifyKey) {
        for (NotifyKeyEnum notifyKeyEnum : NotifyKeyEnum.values()) {
            if (notifyKeyEnum.getNotifyKey() == notifyKey) {
                return notifyKeyEnum;
            }
        }
        return null;
    }
}
