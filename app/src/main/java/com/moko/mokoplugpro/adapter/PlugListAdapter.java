package com.moko.mokoplugpro.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.entity.PlugInfo;

public class PlugListAdapter extends BaseQuickAdapter<PlugInfo, BaseViewHolder> {
    public PlugListAdapter() {
        super(R.layout.item_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, PlugInfo item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_rssi, String.format("%ddBm", item.rssi));
        if (item.overLoad == 1) {
            helper.setText(R.id.tv_device_status, "Overload");
            return;
        }
        if (item.overCurrent == 1) {
            helper.setText(R.id.tv_device_status, "Overcurrent");
            return;
        }
        if (item.overVoltage == 1) {
            helper.setText(R.id.tv_device_status, "Overvoltage");
            return;
        }
        if (item.sagVoltage == 1) {
            helper.setText(R.id.tv_device_status, "Undervoltage");
            return;
        }
        if (item.onOff == 1) {
            helper.setText(R.id.tv_device_status, "OFF");
            return;
        }
        helper.setText(R.id.tv_device_status, String.format("ON/%sW/%sV/%sA\n%s/%sHz/%sKwh",
                item.power, item.voltage, item.current, item.powerFactor, item.currentRate, item.energyTotal));
    }
}