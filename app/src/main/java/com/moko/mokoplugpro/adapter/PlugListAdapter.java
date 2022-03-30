package com.moko.mokoplugpro.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.entity.PlugInfo;

public class PlugListAdapter extends BaseQuickAdapter<PlugInfo, BaseViewHolder> {
    public PlugListAdapter() {
        super(R.layout.item_device_pro);
    }

    @Override
    protected void convert(BaseViewHolder helper, PlugInfo item) {
        final String rssi = String.format("%ddBm", item.rssi);
        helper.setText(R.id.tv_rssi, rssi);
        final String name = TextUtils.isEmpty(item.name) ? "N/A" : item.name;
        helper.setText(R.id.tv_name, name);
        helper.setText(R.id.tv_mac, String.format("MAC:%s", item.mac));

        helper.setText(R.id.tv_tx_power, String.format("Tx Power:%sdBm", item.txPower));
        helper.setText(R.id.tv_voltage, String.format("%s V", item.voltage));
        helper.setText(R.id.tv_current, String.format("%s A", item.current));
        helper.setText(R.id.tv_power, String.format("%s W", item.power));
        helper.setText(R.id.tv_power_factor, String.format("%s %%", item.powerFactor));
        helper.setText(R.id.tv_frequency, String.format("%s HZ", item.currentRate));
        helper.setText(R.id.tv_energy, String.format("%s KWh", item.energyTotal));
        helper.setVisible(R.id.tv_connect, item.isConnectable == 1);
        helper.addOnClickListener(R.id.tv_connect);
        if (item.overLoad == 1) {
            helper.setText(R.id.tv_status, "OverLoad");
            return;
        }
        if (item.overCurrent == 1) {
            helper.setText(R.id.tv_status, "OverCurrent");
            return;
        }
        if (item.overVoltage == 1) {
            helper.setText(R.id.tv_status, "OverVoltage");
            return;
        }
        if (item.sagVoltage == 1) {
            helper.setText(R.id.tv_status, "UnderVoltage");
            return;
        }
        if (item.onOff == 1) {
            helper.setText(R.id.tv_status, "ON");
            return;
        }
        helper.setText(R.id.tv_status, "OFF");
    }
}