package com.moko.mokoplugpro.activity;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.databinding.ActivityAdvInfoBinding;
import com.moko.mokoplugpro.entity.TxPowerEnum;
import com.moko.mokoplugpro.event.DataChangedEvent;
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

public class AdvInfoActivity extends BaseActivity<ActivityAdvInfoBinding> implements SeekBar.OnSeekBarChangeListener {

    private final String FILTER_ASCII = "[ -~]*";
    private boolean savedParamsError;

    @Override
    protected void onCreate() {
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        mBind.etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20), filter});
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        EventBus.getDefault().register(this);
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getAdvName());
        orderTasks.add(OrderTaskAssembler.getAdvInterval());
        orderTasks.add(OrderTaskAssembler.getTxPower());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected ActivityAdvInfoBinding getViewBinding() {
        return ActivityAdvInfoBinding.inflate(getLayoutInflater());
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
                                    case KEY_ADV_NAME:
                                        DataChangedEvent dataChangedEvent = new DataChangedEvent();
                                        final String advName = mBind.etAdvName.getText().toString();
                                        dataChangedEvent.setValue(advName);
                                        EventBus.getDefault().post(dataChangedEvent);
                                    case KEY_ADV_INTERVAL:
                                        if (result == 0) {
                                            savedParamsError = true;
                                        }
                                        break;
                                    case KEY_TX_POWER:
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
                                    case KEY_ADV_NAME:
                                        if (length > 0) {
                                            byte[] advNameBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                            mBind.etAdvName.setText(new String(advNameBytes));
                                        }
                                        break;
                                    case KEY_ADV_INTERVAL:
                                        if (length == 1) {
                                            int interval = value[4] & 0xFF;
                                            mBind.etAdvInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                    case KEY_TX_POWER:
                                        if (length == 1) {
                                            int txPower = value[4];
                                            TxPowerEnum txPowerEnum = TxPowerEnum.fromTxPower(txPower);
                                            mBind.sbTxPower.setProgress(txPowerEnum.ordinal());
                                            mBind.tvTxPowerValue.setText(String.format("%ddBm", txPower));
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
        final String advName = mBind.etAdvName.getText().toString();
        final String advInterval = mBind.etAdvInterval.getText().toString();
        final int interval = Integer.parseInt(advInterval);
        final int txPower = TxPowerEnum.fromOrdinal(mBind.sbTxPower.getProgress()).getTxPower();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setAdvName(advName));
        orderTasks.add(OrderTaskAssembler.setAdvInterval(interval));
        orderTasks.add(OrderTaskAssembler.setTxPower(txPower));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        final String advName = mBind.etAdvName.getText().toString();
        if (TextUtils.isEmpty(advName)) {
            return false;
        }
        final String advInterval = mBind.etAdvInterval.getText().toString();
        if (TextUtils.isEmpty(advInterval)) {
            return false;
        }
        final int interval = Integer.parseInt(advInterval);
        if (interval < 1 || interval > 100) {
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
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
        mBind.tvTxPowerValue.setText(String.format("%ddBm", txPowerEnum.getTxPower()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
