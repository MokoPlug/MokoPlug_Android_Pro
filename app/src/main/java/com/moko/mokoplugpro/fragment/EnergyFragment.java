package com.moko.mokoplugpro.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.activity.DeviceInfoActivity;
import com.moko.mokoplugpro.adapter.EnergyListAdapter;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class EnergyFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = EnergyFragment.class.getSimpleName();
    @BindView(R2.id.rg_energy)
    RadioGroup rgEnergy;
    @BindView(R2.id.tv_energy_total)
    TextView tvEnergyTotal;
    @BindView(R2.id.tv_duration)
    TextView tvDuration;
    @BindView(R2.id.tv_unit)
    TextView tvUnit;
    @BindView(R2.id.rv_energy)
    RecyclerView rvEnergy;
    @BindView(R2.id.rb_hourly)
    RadioButton rbHourly;
    @BindView(R2.id.rb_daily)
    RadioButton rbDaily;
    @BindView(R2.id.rb_totally)
    RadioButton rbTotally;
    @BindView(R2.id.cl_energy)
    ConstraintLayout clEnergy;
    @BindView(R2.id.tv_clear_energy_data)
    TextView tvClearEnergyData;
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
                                        if (length > 0 && rbHourly.isChecked()) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            tvDuration.setText(String.format("00:00 to %02d:00,%02d-%02d", hour, month, day));
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 9, length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 9 + 2 * i;
                                                int to = 11 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                energyInfo.time = String.format("%02d:00", i);
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyBytes) * 0.01f);
                                                energyInfoList.add(0, energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
                                        }
                                        break;
                                    case KEY_ENERGY_DAILY:
                                        if (length > 0 && rbDaily.isChecked()) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 9, length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.set(Calendar.YEAR, year);
                                            calendar.set(Calendar.MONTH, month - 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, day);
                                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                                            String end = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            calendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
                                            String start = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            tvDuration.setText(String.format("%s to %s", start, end));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 9 + 2 * i;
                                                int to = 11 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                calendar.add(Calendar.DAY_OF_MONTH, -i);
                                                String date = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                                energyInfo.time = date;
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyBytes) * 0.01f);
                                                energyInfoList.add(energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
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
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 9, 4 + length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            tvDuration.setText(String.format("00:00 to %02d:00,%02d-%02d", hour, month, day));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 9 + 2 * i;
                                                int to = 11 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                energyInfo.time = String.format("%02d:00", i);
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyBytes) * 0.01f);
                                                energyInfoList.add(0, energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
                                        }
                                        break;
                                    case KEY_ENERGY_DAILY:
                                        if (length > 0) {
                                            int year = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int month = value[6] & 0xFF;
                                            int day = value[7] & 0xFF;
                                            int hour = value[8] & 0xFF;
                                            int count = value[9] & 0xFF;
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 9, 4 + length);
                                            if (energyDataBytes.length % 2 != 0
                                                    || energyDataBytes.length / 2 != count)
                                                break;
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.set(Calendar.YEAR, year);
                                            calendar.set(Calendar.MONTH, month - 1);
                                            calendar.set(Calendar.DAY_OF_MONTH, day);
                                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                                            String end = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            calendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
                                            String start = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                            tvDuration.setText(String.format("%s to %s", start, end));
                                            energyInfoList.clear();
                                            int energyDataSum = 0;
                                            for (int i = 0; i < count; i++) {
                                                int from = 9 + 2 * i;
                                                int to = 11 + 2 * i;
                                                byte[] energyBytes = Arrays.copyOfRange(value, from, to);
                                                int energyInt = MokoUtils.toInt(energyBytes);
                                                energyDataSum += energyInt;
                                                EnergyInfo energyInfo = new EnergyInfo();
                                                calendar.add(Calendar.DAY_OF_MONTH, -i);
                                                String date = MokoUtils.calendar2strDate(calendar, "MM-dd");
                                                energyInfo.time = date;
                                                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyBytes) * 0.01f);
                                                energyInfoList.add(energyInfo);
                                            }
                                            adapter.replaceData(energyInfoList);
                                            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
                                        }
                                        break;
                                    case KEY_ENERGY_TOTALLY:
                                        if (length == 4) {
                                            byte[] energyDataBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(MokoUtils.toInt(energyDataBytes) * 0.01f));
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
        View view = inflater.inflate(R.layout.fragment_energy_pro, container, false);
        ButterKnife.bind(this, view);
        energyInfoList = new ArrayList<>();
        adapter = new EnergyListAdapter();
        adapter.replaceData(energyInfoList);
        adapter.openLoadAnimation();
        rvEnergy.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvEnergy.setAdapter(adapter);
        activity = (DeviceInfoActivity) getActivity();
        rgEnergy.setOnCheckedChangeListener(this);
        EventBus.getDefault().register(this);
        return view;
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
            clEnergy.setVisibility(View.VISIBLE);
            tvClearEnergyData.setVisibility(View.GONE);
            tvUnit.setText("Hour");
            activity.getEnergyHourly();
        } else if (checkedId == R.id.rb_daily) {
            // 切换月
            clEnergy.setVisibility(View.VISIBLE);
            tvClearEnergyData.setVisibility(View.GONE);
            tvUnit.setText("Date");
            activity.getEnergyDaily();
        } else if (checkedId == R.id.rb_totally) {
            // 切换总电能
            clEnergy.setVisibility(View.GONE);
            tvClearEnergyData.setVisibility(View.VISIBLE);
            activity.getEnergyTotally();
        }
    }
}
