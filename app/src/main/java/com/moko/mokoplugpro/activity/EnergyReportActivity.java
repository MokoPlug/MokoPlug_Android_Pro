package com.moko.mokoplugpro.activity;

import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.databinding.ActivityEnergyReportBinding;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnergyReportActivity extends BaseActivity<ActivityEnergyReportBinding> {

    private boolean savedParamsError;

    @Override
    protected void onCreate() {
        EventBus.getDefault().register(this);
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getEnergySavedInterval());
        orderTasks.add(OrderTaskAssembler.getPowerChangeThreshold());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected ActivityEnergyReportBinding getViewBinding() {
        return ActivityEnergyReportBinding.inflate(getLayoutInflater());
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                if (MokoSupport.getInstance().isBluetoothOpen()) {
                    dismissLoadingProgressDialog();
                    finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
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
                                    case KEY_OVER_LOAD:
                                    case KEY_OVER_VOLTAGE:
                                    case KEY_OVER_CURRENT:
                                    case KEY_SAG_VOLTAGE:
                                        if (length > 0 && value[4] == 1) {
                                            finish();
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                ToastUtils.showToast(this, R.string.timeout);
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissLoadingProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {

                    case CHAR_PARAMS:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xED)
                                return;
                            ParamsKeyEnum paramsKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (paramsKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01) {
                                int result = value[4] & 0xFF;
                                switch (paramsKeyEnum) {
                                    case KEY_ENERGY_SAVED_INTERVAL:
                                        if (result == 0) {
                                            savedParamsError = true;
                                        }
                                        break;
                                    case KEY_POWER_CHANGE_THRESHOLD:
                                        if (result == 0) {
                                            savedParamsError = true;
                                        }
                                        if (savedParamsError) {
                                            savedParamsError = false;
                                            ToastUtils.showToast(this, "Setup failed!");
                                        } else {
                                            ToastUtils.showToast(this, "Setup succeed!");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                switch (paramsKeyEnum) {
                                    case KEY_ENERGY_SAVED_INTERVAL:
                                        if (length == 1) {
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.etEnergyReportInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                    case KEY_POWER_CHANGE_THRESHOLD:
                                        if (length == 1) {
                                            int threshold = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.etPowerChangeThreshold.setText(String.valueOf(threshold));
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

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isValid()) {
            showLoadingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private void saveParams() {
        final String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        final String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
        final int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
        final int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setEnergySavedInterval(energyReportInterval));
        orderTasks.add(OrderTaskAssembler.setPowerChangeThreshold(powerChangeThreshold));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        final String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        if (TextUtils.isEmpty(energyReportIntervalStr)) {
            return false;
        }
        final int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
        if (energyReportInterval < 1 || energyReportInterval > 60) {
            return false;
        }
        final String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
        if (TextUtils.isEmpty(powerChangeThresholdStr)) {
            return false;
        }
        final int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
        if (powerChangeThreshold < 1 || powerChangeThreshold > 100) {
            return false;
        }
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked())
            return;
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
