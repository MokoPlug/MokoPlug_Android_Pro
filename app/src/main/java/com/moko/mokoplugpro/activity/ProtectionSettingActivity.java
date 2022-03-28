package com.moko.mokoplugpro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
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
import java.util.List;

import butterknife.ButterKnife;

public class ProtectionSettingActivity extends BaseActivity {

    private int productType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protection_setting);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
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
                            if (flag == 0x00) {
                                switch (paramsKeyEnum) {
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

    public void onOverloadProtectionClick(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverLoadProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, productType);
        startActivity(intent);
    }

    public void onOverVoltageProtectionClick(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverVoltageProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, productType);
        startActivity(intent);
    }

    public void onUnderVoltageProtectionClick(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, UnderVoltageProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, productType);
        startActivity(intent);
    }

    public void onOverCurrentProtectionClick(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverCurrentProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PRODUCT_TYPE, productType);
        startActivity(intent);
    }
}
