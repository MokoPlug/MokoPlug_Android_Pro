package com.moko.mokoplugpro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

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

public class IndicatorSettingActivity extends BaseActivity {


    @BindView(R2.id.iv_ble_advertising)
    ImageView ivBleAdvertising;
    @BindView(R2.id.iv_protection_signal)
    ImageView ivProtectionSignal;
    private boolean isBleAdvEnable;
    private boolean isProtectionSignalEnable;
    private int productType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indicator_setting);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getIndicatorBleAdvStatus());
        orderTasks.add(OrderTaskAssembler.getIndicatorPowerProtectionStatus());
        orderTasks.add(OrderTaskAssembler.getProductType());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
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

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
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
                                    case KEY_INDICATOR_BLE_ADV_STATUS:
                                    case KEY_INDICATOR_POWER_PROTECTION_STATUS:
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
                                    case KEY_INDICATOR_BLE_ADV_STATUS:
                                        if (length == 1) {
                                            isBleAdvEnable = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length)) == 1;
                                            ivBleAdvertising.setImageResource(isBleAdvEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                        }
                                        break;
                                    case KEY_INDICATOR_POWER_PROTECTION_STATUS:
                                        if (length == 1) {
                                            isProtectionSignalEnable = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length)) == 1;
                                            ivProtectionSignal.setImageResource(isProtectionSignalEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                        }
                                        break;
                                    case KEY_PRODUCT_TYPE:
                                        if (length == 1) {
                                            productType = value[4] & 0xFF;
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

    public void onBleAdv(View view) {
        if (isWindowLocked())
            return;
        isBleAdvEnable = !isBleAdvEnable;
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setIndicatorBleAdvStatus(isBleAdvEnable ? 1 : 0));
    }

    public void onBleConnected(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, BleConnectedActivity.class);
        startActivity(intent);
    }

    public void onIndicatorStatus(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, PowerIndicatorColorActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, productType);
        startActivity(intent);
    }

    public void onProtectionSignal(View view) {
        if (isWindowLocked())
            return;
        isProtectionSignalEnable = !isProtectionSignalEnable;
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setIndicatorPowerProtectionStatus(isProtectionSignalEnable ? 1 : 0));
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
