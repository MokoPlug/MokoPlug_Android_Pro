package com.moko.mokoplugpro.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ButtonSettingActivity extends BaseActivity {


    @BindView(R2.id.iv_btn_switch)
    ImageView ivBtnSwitch;
    @BindView(R2.id.iv_btn_reset)
    ImageView ivBtnReset;

    private boolean isBtnSwitchEnable;
    private boolean isBtnResetEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_setting);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getButtonControlEnable());
        orderTasks.add(OrderTaskAssembler.getButtonResetEnable());
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
                                    case KEY_BUTTON_CONTROL_ENABLE:
                                    case KEY_BUTTON_RESET_ENABLE:
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
                                    case KEY_BUTTON_CONTROL_ENABLE:
                                        if (length == 1) {
                                            int enable = value[4] & 0xFF;
                                            isBtnSwitchEnable = enable == 1;
                                            ivBtnSwitch.setImageResource(isBtnSwitchEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                        }
                                        break;
                                    case KEY_BUTTON_RESET_ENABLE:
                                        if (length == 1) {
                                            int enable = value[4] & 0xFF;
                                            isBtnResetEnable = enable == 1;
                                            ivBtnReset.setImageResource(isBtnResetEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
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

    public void onBtnSwitch(View view) {
        if (isWindowLocked())
            return;
        isBtnSwitchEnable  = !isBtnSwitchEnable;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setButtonControlEnable(isBtnSwitchEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getButtonControlEnable());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void onBtnReset(View view) {
        if (isWindowLocked())
            return;
        isBtnResetEnable  = !isBtnResetEnable;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setButtonResetEnable(isBtnResetEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getButtonResetEnable());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
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
