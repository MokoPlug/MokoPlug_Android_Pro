package com.moko.mokoplugpro.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.activity.DeviceInfoActivity;
import com.moko.mokoplugpro.databinding.FragmentSwitchProBinding;
import com.moko.mokoplugpro.dialog.TimerDialog;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import androidx.core.content.ContextCompat;

public class SwitchFragment extends Fragment {

    private static final String TAG = SwitchFragment.class.getSimpleName();
    private FragmentSwitchProBinding mBind;
    private boolean onOff = false;
    private DeviceInfoActivity activity;

    public SwitchFragment() {
    }

    public static SwitchFragment newInstance() {
        SwitchFragment fragment = new SwitchFragment();
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
                                    case KEY_COUNT_DOWN:
                                        if (length == 5) {
                                            int switchStatus = value[4] & 0xFF;
                                            int countdown = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 9));
                                            setCountdown(switchStatus, countdown);
                                        }
                                        break;
                                    case KEY_SWITCH_STATUS:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            changeSwitchStatus(status);
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
                                    case KEY_SWITCH_STATUS:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            changeSwitchStatus(status);
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
        mBind = FragmentSwitchProBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        EventBus.getDefault().register(this);
        return mBind.getRoot();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView: ");
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    public void changeSwitchState() {
        onOff = !onOff;
        activity.changeSwitchState(onOff);
    }

    private void changeSwitchStatus(int status) {
        if (status == countdownOnOff) {
            countdownOnOff = -1;
            mBind.tvCountdownTips.setVisibility(View.GONE);
        }
        onOff = status == 1;
        mBind.clSwitchBg.setBackgroundColor(ContextCompat.getColor(activity, onOff ? R.color.grey_f5f5f5 : R.color.black_333333));
        mBind.ivSwitchState.setImageDrawable(ContextCompat.getDrawable(activity, onOff ? R.drawable.plug_switch_on : R.drawable.plug_switch_off));
        mBind.tvSwitchState.setText(String.format("Socket is %s", onOff ? "on" : "off"));
        mBind.tvSwitchState.setTextColor(ContextCompat.getColor(activity, onOff ? R.color.blue_2681ff : R.color.grey_808080));
        mBind.tvCountdownTips.setTextColor(ContextCompat.getColor(activity, onOff ? R.color.blue_2681ff : R.color.grey_808080));
    }

    public boolean getSwitchState() {
        return onOff;
    }

    public void setTimer() {
        boolean onOff = activity.getSwitchState();
        TimerDialog timerDialog = new TimerDialog();
        timerDialog.setOnOff(onOff);
        timerDialog.setListener(dialog -> {
            int hour = dialog.getWvHour();
            int minute = dialog.getWvMinute();
            int countdown = hour * 3600 + minute * 60;
            activity.setTimer(countdown);
            dialog.dismiss();
        });
        timerDialog.show(activity.getSupportFragmentManager());
    }

    private int countdownOnOff = -1;

    private void setCountdown(int switchStatus, int countdown) {
        countdownOnOff = switchStatus;
        if (countdown > 1) {
            mBind.tvCountdownTips.setVisibility(View.VISIBLE);
            int hour = countdown / 3600;
            int minute = (countdown % 3600) / 60;
            int second = (countdown % 3600) % 60;
            String countDown = String.format("%02d:%02d:%02d", hour, minute, second);
            mBind.tvCountdownTips.setText(getString(R.string.countdown_tips_pro, switchStatus == 0 ? "OFF" : "ON", countDown));
        } else {
            mBind.tvCountdownTips.setVisibility(View.GONE);
        }
    }
}
