package com.moko.support.pro.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.pro.entity.OrderCHAR;

public class GetMacTask extends OrderTask {

    public byte[] data;

    public GetMacTask() {
        super(OrderCHAR.CHAR_MAC, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
