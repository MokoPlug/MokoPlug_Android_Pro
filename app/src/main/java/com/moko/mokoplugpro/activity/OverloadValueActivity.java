package com.moko.mokoplugpro.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.dialog.LoadingMessageDialog;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.ParamsWriteKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OverloadValueActivity extends BaseActivity {

    @BindView(R2.id.et_overload_value)
    EditText etOverloadValue;
    private boolean mReceiverTag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overload_value);
        ButterKnife.bind(this);

        int overloadTopValue = MokoSupport.getInstance().overloadTopValue;
        etOverloadValue.setText(String.valueOf(overloadTopValue));
        etOverloadValue.setSelection(String.valueOf(overloadTopValue).length());

        getFocuable(etOverloadValue);

        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                    if (MokoSupport.getInstance().isBluetoothOpen()) {
                        dismissSyncProgressDialog();
                        finish();
                    }
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
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
                    case CHAR_PARAMS_WRITE:
                        final int cmd = value[1] & 0xFF;
                        ParamsWriteKeyEnum configKeyEnum = ParamsWriteKeyEnum.fromParamKey(cmd);
                        switch (configKeyEnum) {
                            case SET_OVERLOAD_TOP_VALUE:
                                if (0 == (value[3] & 0xFF)) {
                                    MokoSupport.getInstance().overloadTopValue = Integer.parseInt(etOverloadValue.getText().toString());
                                    ToastUtils.showToast(OverloadValueActivity.this, R.string.success);
                                    OverloadValueActivity.this.setResult(OverloadValueActivity.this.RESULT_OK);
                                    finish();
                                } else {
                                    ToastUtils.showToast(OverloadValueActivity.this, R.string.failed);
                                }
                                break;
                        }
                }
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            dismissSyncProgressDialog();
                            finish();
                            break;
                    }
                }
            }
        }
    };

    public void onBack(View view) {
        if (isWindowLocked())
            return;
        finish();
    }

    public void onConfirm(View view) {
        if (isWindowLocked())
            return;
        String overloadValue = etOverloadValue.getText().toString();
        if (TextUtils.isEmpty(overloadValue)) {
            ToastUtils.showToast(this, "can't be blank");
            return;
        }
        int value = Integer.parseInt(overloadValue);
        if (value < 10 || value > 3680) {
            ToastUtils.showToast(this, "the range is 10~3680");
            return;
        }
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.writeOverloadTopValue(value));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
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
