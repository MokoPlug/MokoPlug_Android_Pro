package com.moko.mokoplugpro.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.activity.DeviceInfoActivity;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PowerFragment extends Fragment {

    private static final String TAG = PowerFragment.class.getSimpleName();
    @BindView(R2.id.tv_voltage)
    TextView tvVoltage;
    @BindView(R2.id.tv_current)
    TextView tvCurrent;
    @BindView(R2.id.tv_power)
    TextView tvPower;
    @BindView(R2.id.tv_power_factor)
    TextView tvPowerFactor;
    @BindView(R2.id.tv_frequency)
    TextView tvFrequency;

    private DeviceInfoActivity activity;

    public PowerFragment() {
    }

    public static PowerFragment newInstance() {
        PowerFragment fragment = new PowerFragment();
        return fragment;
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        activity.runOnUiThread(() -> {
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_NOTIFY:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xED)
                                return;
                            NotifyKeyEnum notifyKeyEnum = NotifyKeyEnum.fromNotifyKey(cmd);
                            if (notifyKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x02) {
                                switch (notifyKeyEnum) {
                                    case KEY_POWER_DATA:
                                        if (length == 11) {
                                            int voltage = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int current = MokoUtils.toIntSigned(Arrays.copyOfRange(value, 6, 8));
                                            int power = MokoUtils.toIntSigned(Arrays.copyOfRange(value, 8, 12));
                                            int frequency = MokoUtils.toInt(Arrays.copyOfRange(value, 12, 14));
                                            int powerFactor = value[14] & 0xFF;
                                            setPowerData(voltage, current, power, frequency, powerFactor);
                                        }
                                        break;

                                }
                            }
                        }
                        break;
                }
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                activity.dismissLoadingProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_CONFIG:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xED)
                                return;
                            ConfigKeyEnum configKeyEnum = ConfigKeyEnum.fromConfigKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x00) {
                                switch (configKeyEnum) {
                                    case KEY_POWER_DATA:
                                        if (length == 11) {
                                            int voltage = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int current = MokoUtils.toIntSigned(Arrays.copyOfRange(value, 6, 8));
                                            int power = MokoUtils.toIntSigned(Arrays.copyOfRange(value, 8, 12));
                                            int frequency = MokoUtils.toInt(Arrays.copyOfRange(value, 12, 14));
                                            int powerFactor = value[14] & 0xFF;
                                            setPowerData(voltage, current, power, frequency, powerFactor);
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_power_pro, container, false);
        ButterKnife.bind(this, view);
        activity = (DeviceInfoActivity) getActivity();
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView: ");
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    private void setPowerData(int voltage, int current, int power, int frequency, int powerFactor) {
        String voltageStr = MokoUtils.getDecimalFormat("0.#").format(voltage * 0.1f);
        String currentStr = String.valueOf(current);
        String powerStr = MokoUtils.getDecimalFormat("0.#").format(power * 0.1f);
        String frequencyStr = MokoUtils.getDecimalFormat("0.##").format(frequency * 0.01f);
        String powerFactorStr = MokoUtils.getDecimalFormat("0.##").format(powerFactor * 0.01f);
        tvVoltage.setText(voltageStr);
        tvCurrent.setText(currentStr);
        tvPower.setText(powerStr);
        tvFrequency.setText(frequencyStr);
        tvPowerFactor.setText(powerFactorStr);
    }

}
