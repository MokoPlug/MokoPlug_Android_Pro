package com.moko.mokoplugpro.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.activity.DeviceInfoActivity;
import com.moko.mokoplugpro.adapter.EnergyListAdapter;
import com.moko.mokoplugpro.databinding.FragmentEnergyProBinding;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.EnergyInfo;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

public class EnergyFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = EnergyFragment.class.getSimpleName();
    private FragmentEnergyProBinding mBind;
    private List<EnergyInfo> energyInfoList;
    private EnergyListAdapter adapter;

    private DeviceInfoActivity activity;

    public EnergyFragment() {
    }

    public static EnergyFragment newInstance() {
        EnergyFragment fragment = new EnergyFragment();
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
                                    case KEY_ENERGY_HOURLY:
                                        if (length > 0 && mBind.rbHourly.isChecked()) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            mBind.tvDuration.setText(String.format("00:00 to %02d:00,%02d-%02d", hour, month, day));
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 10, length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 10 + 2 * i;
                                                int to = 12 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                energyInfo.time = String.format("%02d:00", i);
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(energyBytes) * 0.001f);
                                                energyInfoList.add(0, energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.###").format(energyDataSum * 0.001f));
                                        }
                                        break;
                                    case KEY_ENERGY_DAILY:
                                        if (length > 0 && mBind.rbDaily.isChecked()) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 10, length);
                                            if (energyDataBytes.length % 3 != 0
                                                    || energyDataBytes.length / 3 != count)
                                                break;
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.set(Calendar.YEAR, year);
                                            calendar.set(Calendar.MONTH, month - 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, day);
                                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                                            String end = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            Calendar startCalendar = (Calendar) calendar.clone();
                                            startCalendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
                                            String start = MokoUtils.calendar2strDate(startCalendar, "MM-dd");
                                            mBind.tvDuration.setText(String.format("%s to %s", start, end));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 10 + 3 * i;
                                                int to = 13 + 3 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                String date = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                                energyInfo.time = date;
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(energyBytes) * 0.001f);
                                                energyInfoList.add(energyInfo);
                                                calendar.add(Calendar.DAY_OF_MONTH, -1);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.###").format(energyDataSum * 0.001f));
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
                            if (flag == 0x01) {
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_ENERGY_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(activity, "Setup failed!");
                                        } else {
                                            ToastUtils.showToast(activity, "Setup succeed!");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                switch (configKeyEnum) {
                                    case KEY_ENERGY_HOURLY:
                                        if (length > 0) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 10, 4 + length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            mBind.tvDuration.setText(String.format("00:00 to %02d:00,%02d-%02d", hour, month, day));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 10 + 2 * i;
                                                int to = 12 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                energyInfo.time = String.format("%02d:00", i);
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(energyBytes) * 0.001f);
                                                energyInfoList.add(0, energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.###").format(energyDataSum * 0.001f));
                                        }
                                        break;
                                    case KEY_ENERGY_DAILY:
                                        if (length > 0) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 10, 4 + length);
                                            if (energyDataBytes.length % 3 != 0
                                                    || energyDataBytes.length / 3 != count)
                                                break;
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.set(Calendar.YEAR, year);
                                            calendar.set(Calendar.MONTH, month - 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, day);
                                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                                            String end = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            Calendar startCalendar = (Calendar) calendar.clone();
                                            startCalendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
                                            String start = MokoUtils.calendar2strDate(startCalendar, "MM-dd");
                                            mBind.tvDuration.setText(String.format("%s to %s", start, end));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 10 + 3 * i;
                                                int to = 13 + 3 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                String date = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                                energyInfo.time = date;
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(energyBytes) * 0.001f);
                                                energyInfoList.add(energyInfo);
                                                calendar.add(Calendar.DAY_OF_MONTH, -1);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.###").format(energyDataSum * 0.001f));
                                        }
                                        break;
                                    case KEY_ENERGY_TOTALLY:
                                        if (length == 4) {
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                            mBind.tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(energyDataBytes) * 0.001f));
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
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentEnergyProBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        energyInfoList = new ArrayList<>();
        adapter = new EnergyListAdapter();
        adapter.replaceData(energyInfoList);
        adapter.openLoadAnimation();
        mBind.rvEnergy.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBind.rvEnergy.setAdapter(adapter);
        mBind.rgEnergy.setOnCheckedChangeListener(this);
        EventBus.getDefault().register(this);
        return mBind.getRoot();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView: ");
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_hourly) {
            // 切换日
            mBind.clEnergy.setVisibility(View.VISIBLE);
            mBind.tvClearEnergyData.setVisibility(View.GONE);
            mBind.tvUnit.setText("Hour");
            mBind.tvEnergyDesc.setText("Today energy:");
            activity.getEnergyHourly();
        } else if (checkedId == R.id.rb_daily) {
            // 切换月
            mBind.clEnergy.setVisibility(View.VISIBLE);
            mBind.tvClearEnergyData.setVisibility(View.GONE);
            mBind.tvUnit.setText("Date");
            mBind.tvEnergyDesc.setText("Last 30 days energy:");
            activity.getEnergyDaily();
        } else if (checkedId == R.id.rb_totally) {
            // 切换总电能
            mBind.tvEnergyDesc.setText("Historical total energy:");
            mBind.clEnergy.setVisibility(View.GONE);
            mBind.tvClearEnergyData.setVisibility(View.VISIBLE);
            activity.getEnergyTotally();
        }
    }
}
