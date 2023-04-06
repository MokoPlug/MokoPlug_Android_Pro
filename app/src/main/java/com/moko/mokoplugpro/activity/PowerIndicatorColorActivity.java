package com.moko.mokoplugpro.activity;

import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.databinding.ActivityPowerIndicatorColorBinding;
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

import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class PowerIndicatorColorActivity extends BaseActivity<ActivityPowerIndicatorColorBinding> implements NumberPickerView.OnValueChangeListener, CompoundButton.OnCheckedChangeListener {

    private int productType;

    private boolean savedParamsError;

    private int mSelected;

    @Override
    protected void onCreate() {
        productType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, 0);
        mBind.npvColorSettings.setMinValue(0);
        mBind.npvColorSettings.setMaxValue(8);
        mBind.npvColorSettings.setValue(0);
        mBind.npvColorSettings.setOnValueChangedListener(this);
        mBind.cbIndicatorSwitchStatus.setOnCheckedChangeListener(this);
        EventBus.getDefault().register(this);
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getIndicatorPowerSwitchStatus());
        orderTasks.add(OrderTaskAssembler.getPowerIndicatorColor());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected ActivityPowerIndicatorColorBinding getViewBinding() {
        return ActivityPowerIndicatorColorBinding.inflate(getLayoutInflater());
    }

    @Override
    public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
        mSelected = newVal;
        if (newVal > 1) {
            mBind.llColorSettings.setVisibility(View.GONE);
        } else {
            mBind.llColorSettings.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 500)
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

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 500)
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
                                    case KEY_INDICATOR_POWER_SWITCH_STATUS:
                                        if (result == 0) {
                                            savedParamsError = true;
                                        }
                                        if (!mBind.cbIndicatorSwitchStatus.isChecked()) {
                                            if (savedParamsError) {
                                                savedParamsError = false;
                                                ToastUtils.showToast(this, "Setup failed!");
                                            } else {
                                                ToastUtils.showToast(this, "Setup succeed!");
                                            }
                                        }
                                        break;
                                    case KEY_POWER_INDICATOR:
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
                                // read
                                switch (paramsKeyEnum) {
                                    case KEY_POWER_INDICATOR:
                                        if (length > 0) {
                                            mSelected = value[4] & 0xFF;
                                            mBind.npvColorSettings.setValue(mSelected);
                                            if (mSelected > 1) {
                                                mBind.llColorSettings.setVisibility(View.GONE);
                                            } else {
                                                mBind.llColorSettings.setVisibility(View.VISIBLE);
                                            }
                                            byte[] blueBytes = Arrays.copyOfRange(value, 5, 7);
                                            int blue = MokoUtils.toInt(blueBytes);
                                            mBind.etBlue.setText(String.valueOf(blue));
                                            byte[] greenBytes = Arrays.copyOfRange(value, 7, 9);
                                            int green = MokoUtils.toInt(greenBytes);
                                            mBind.etGreen.setText(String.valueOf(green));
                                            byte[] yellowBytes = Arrays.copyOfRange(value, 9, 11);
                                            int yellow = MokoUtils.toInt(yellowBytes);
                                            mBind.etYellow.setText(String.valueOf(yellow));
                                            byte[] orangeBytes = Arrays.copyOfRange(value, 11, 13);
                                            int orange = MokoUtils.toInt(orangeBytes);
                                            mBind.etOrange.setText(String.valueOf(orange));
                                            byte[] redBytes = Arrays.copyOfRange(value, 13, 15);
                                            int red = MokoUtils.toInt(redBytes);
                                            mBind.etRed.setText(String.valueOf(red));
                                            byte[] purpleBytes = Arrays.copyOfRange(value, 15, 17);
                                            int purple = MokoUtils.toInt(purpleBytes);
                                            mBind.etPurple.setText(String.valueOf(purple));
                                        }
                                        break;
                                    case KEY_INDICATOR_POWER_SWITCH_STATUS:
                                        if (length == 1) {
                                            int enable = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.cbIndicatorSwitchStatus.setChecked(enable == 1);
                                            mBind.svColorSetting.setVisibility(enable == 1 ? View.VISIBLE : View.GONE);
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
        if (!mBind.cbIndicatorSwitchStatus.isChecked()) {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setIndicatorPowerSwitchStatus(0));
            return;
        }
        if (isValid()) {
            showLoadingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private void saveParams() {
        final String blue = mBind.etBlue.getText().toString();
        final String green = mBind.etGreen.getText().toString();
        final String yellow = mBind.etYellow.getText().toString();
        final String orange = mBind.etOrange.getText().toString();
        final String red = mBind.etRed.getText().toString();
        final String purple = mBind.etPurple.getText().toString();
        final int blueValue = Integer.parseInt(blue);
        final int greenValue = Integer.parseInt(green);
        final int yellowValue = Integer.parseInt(yellow);
        final int orangeValue = Integer.parseInt(orange);
        final int redValue = Integer.parseInt(red);
        final int purpleValue = Integer.parseInt(purple);
        savedParamsError = false;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setIndicatorPowerSwitchStatus(1));
        orderTasks.add(OrderTaskAssembler.setPowerIndicatorColor(mSelected,
                blueValue, greenValue, yellowValue,
                orangeValue, redValue, purpleValue));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        final String blue = mBind.etBlue.getText().toString();
        final String green = mBind.etGreen.getText().toString();
        final String yellow = mBind.etYellow.getText().toString();
        final String orange = mBind.etOrange.getText().toString();
        final String red = mBind.etRed.getText().toString();
        final String purple = mBind.etPurple.getText().toString();
        if (TextUtils.isEmpty(blue) || TextUtils.isEmpty(green) || TextUtils.isEmpty(yellow)
                || TextUtils.isEmpty(orange) || TextUtils.isEmpty(red) || TextUtils.isEmpty(purple)) {
            return false;
        }
        int max = 4416;
        if (productType == 1) {
            max = 2160;
        } else if (productType == 2) {
            max = 3588;
        }
        final int blueValue = Integer.parseInt(blue);
        if (blueValue < 2 || blueValue >= (max - 5)) {
            return false;
        }

        final int greenValue = Integer.parseInt(green);
        if (greenValue <= blueValue || greenValue > (max - 4)) {
            return false;
        }

        final int yellowValue = Integer.parseInt(yellow);
        if (yellowValue <= greenValue || yellowValue > (max - 3)) {
            return false;
        }

        final int orangeValue = Integer.parseInt(orange);
        if (orangeValue <= yellowValue || orangeValue > (max - 2)) {
            return false;
        }

        final int redValue = Integer.parseInt(red);
        if (redValue <= orangeValue || redValue > (max - 1)) {
            return false;
        }

        final int purpleValue = Integer.parseInt(purple);
        if (purpleValue <= redValue || purpleValue > max) {
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

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        mBind.svColorSetting.setVisibility(b ? View.VISIBLE : View.GONE);
    }
}
