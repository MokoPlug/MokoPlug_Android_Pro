package com.moko.mokoplugpro.entity;


import java.io.Serializable;

public enum TxPowerEnum implements Serializable {
    NEGATIVE_40(-40),
    NEGATIVE_20(-20),
    NEGATIVE_8(-8),
    NEGATIVE_4(-4),
    NEGATIVE_0(0),
    POSITIVE_4(4),
    POSITIVE_8(8);

    private int txPower;

    TxPowerEnum(int txPower) {
        this.txPower = txPower;
    }

    public static TxPowerEnum fromOrdinal(int ordinal) {
        for (TxPowerEnum txPowerEnum : TxPowerEnum.values()) {
            if (txPowerEnum.ordinal() == ordinal) {
                return txPowerEnum;
            }
        }
        return null;
    }
    public static TxPowerEnum fromTxPower(int txPower) {
        for (TxPowerEnum txPowerEnum : TxPowerEnum.values()) {
            if (txPowerEnum.getTxPower() == txPower) {
                return txPowerEnum;
            }
        }
        return null;
    }

    public int getTxPower() {
        return txPower;
    }
}
