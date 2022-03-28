package com.moko.support.pro.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.pro.entity.OrderCHAR;

public class GetProductTypeTask extends OrderTask {

    public byte[] data;

    public GetProductTypeTask() {
        super(OrderCHAR.CHAR_PRODUCT_TYPE, OrderTask.RESPONSE_TYPE_READ);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
