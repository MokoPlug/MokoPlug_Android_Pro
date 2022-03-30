package com.moko.mokoplugpro.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.dialog.LoadingDialog;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class PowerIndicatorColorActivity extends BaseActivity implements NumberPickerView.OnValueChangeListener, CompoundButton.OnCheckedChangeListener {


    @BindView(R2.id.cb_indicator_switch_status)
    CheckBox cbIndicatorSwitchStatus;
    @BindView(R2.id.npv_color_settings)
    NumberPickerView npvColorSettings;
    @BindView(R2.id.et_blue)
    EditText etBlue;
    @BindView(R2.id.et_green)
    EditText etGreen;
    @BindView(R2.id.et_yellow)
    EditText etYellow;
    @BindView(R2.id.et_orange)
    EditText etOrange;
    @BindView(R2.id.et_red)
    EditText etRed;
    @BindView(R2.id.et_purple)
    EditText etPurple;
    @BindView(R2.id.ll_color_settings)
    LinearLayout llColorSettings;
    @BindView(R2.id.sv_color_setting)
    ScrollView svColorSetting;
    private int productType;

    private boolean savedParamsError;

    private int mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_indicator_color);
        ButterKnife.bind(this);
        productType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, 0);
        npvColorSettings.setMinValue(0);
        npvColorSettings.setMaxValue(8);
        npvColorSettings.setValue(0);
        npvColorSettings.setOnValueChangedListener(this);
        cbIndicatorSwitchStatus.setOnCheckedChangeListener(this);
        EventBus.getDefault().register(this);
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getIndicatorPowerSwitchStatus());
        orderTasks.add(OrderTaskAssembler.getPowerIndicatorColor());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
        if (newVal > 1) {
            llColorSettings.setVisibility(View.GONE);
        } else {
            llColorSettings.setVisibility(View.VISIBLE);
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
                                        if (!cbIndicatorSwitchStatus.isChecked()) {
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
                                            npvColorSettings.setValue(mSelected);
                                            if (mSelected > 1) {
                                                llColorSettings.setVisibility(View.GONE);
                                            } else {
                                                llColorSettings.setVisibility(View.VISIBLE);
                                            }
                                            byte[] blueBytes = Arrays.copyOfRange(value, 5, 7);
                                            int blue = MokoUtils.toInt(blueBytes);
                                            etBlue.setText(String.valueOf(blue));
                                            byte[] greenBytes = Arrays.copyOfRange(value, 7, 9);
                                            int green = MokoUtils.toInt(greenBytes);
                                            etGreen.setText(String.valueOf(green));
                                            byte[] yellowBytes = Arrays.copyOfRange(value, 9, 11);
                                            int yellow = MokoUtils.toInt(yellowBytes);
                                            etYellow.setText(String.valueOf(yellow));
                                            byte[] orangeBytes = Arrays.copyOfRange(value, 11, 13);
                                            int orange = MokoUtils.toInt(orangeBytes);
                                            etOrange.setText(String.valueOf(orange));
                                            byte[] redBytes = Arrays.copyOfRange(value, 13, 15);
                                            int red = MokoUtils.toInt(redBytes);
                                            etRed.setText(String.valueOf(red));
                                            byte[] purpleBytes = Arrays.copyOfRange(value, 15, 17);
                                            int purple = MokoUtils.toInt(purpleBytes);
                                            etPurple.setText(String.valueOf(purple));
                                        }
                                        break;
                                    case KEY_INDICATOR_POWER_SWITCH_STATUS:
                                        if (length == 1) {
                                            int enable = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            cbIndicatorSwitchStatus.setChecked(enable == 1);
                                            svColorSetting.setVisibility(enable == 1 ? View.VISIBLE : View.GONE);
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
        if (!cbIndicatorSwitchStatus.isChecked()) {
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
        final String blue = etBlue.getText().toString();
        final String green = etGreen.getText().toString();
        final String yellow = etYellow.getText().toString();
        final String orange = etOrange.getText().toString();
        final String red = etRed.getText().toString();
        final String purple = etPurple.getText().toString();
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
        final String blue = etBlue.getText().toString();
        final String green = etGreen.getText().toString();
        final String yellow = etYellow.getText().toString();
        final String orange = etOrange.getText().toString();
        final String red = etRed.getText().toString();
        final String purple = etPurple.getText().toString();
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

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        svColorSetting.setVisibility(b ? View.VISIBLE : View.GONE);
    }
}
