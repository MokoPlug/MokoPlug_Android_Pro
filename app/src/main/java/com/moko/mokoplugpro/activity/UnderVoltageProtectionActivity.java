package com.moko.mokoplugpro.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.dialog.LoadingMessageDialog;
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

public class UnderVoltageProtectionActivity extends BaseActivity {


    @BindView(R2.id.cb_under_voltage_protection)
    CheckBox cbUnderVoltageProtection;
    @BindView(R2.id.et_voltage_threshold)
    EditText etVoltageThreshold;
    @BindView(R2.id.et_time_threshold)
    EditText etTimeThreshold;
    private int productType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_under_voltage_protection);
        ButterKnife.bind(this);
        productType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, 0);
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getSagVoltageProtection());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 500)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                if (MokoSupport.getInstance().isBluetoothOpen()) {
                    dismissSyncProgressDialog();
                    finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 500)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                ToastUtils.showToast(this, R.string.timeout);
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
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
                                    case KEY_SAG_VOLTAGE_PROTECTION:
                                        if (result == 0) {
                                            ToastUtils.showToast(this, "Setup failed!");
                                        } else {
                                            ToastUtils.showToast(this, "Setup succeed!");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                switch (paramsKeyEnum) {
                                    case KEY_SAG_VOLTAGE_PROTECTION:
                                        if (length == 3) {
                                            int enable = value[4] & 0xFF;
                                            cbUnderVoltageProtection.setChecked(enable == 1);
                                            etVoltageThreshold.setText(String.valueOf(value[5] & 0xFF));
                                            etTimeThreshold.setText(String.valueOf(value[6] & 0xFF));
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
            showSyncingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private void saveParams() {
        final String voltageThresholdStr = etVoltageThreshold.getText().toString();
        final String timeThresholdStr = etTimeThreshold.getText().toString();
        final int voltageThreshold = Integer.parseInt(voltageThresholdStr);
        final int timeThreshold = Integer.parseInt(timeThresholdStr);
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setSagVoltageProtection(cbUnderVoltageProtection.isChecked() ? 1 : 0, voltageThreshold, timeThreshold));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        int min = 196;
        int max = 229;
        if (productType == 1) {
            min = 102;
            max = 119;
        }
        final String voltageThresholdStr = etVoltageThreshold.getText().toString();
        if (TextUtils.isEmpty(voltageThresholdStr)) {
            return false;
        }
        final int voltageThreshold = Integer.parseInt(voltageThresholdStr);
        if (voltageThreshold < min || voltageThreshold > max) {
            return false;
        }
        final String timeThresholdStr = etTimeThreshold.getText().toString();
        if (TextUtils.isEmpty(timeThresholdStr)) {
            return false;
        }
        final int timeThreshold = Integer.parseInt(timeThresholdStr);
        if (timeThreshold < 1 || timeThreshold > 30) {
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

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());

    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }
}
