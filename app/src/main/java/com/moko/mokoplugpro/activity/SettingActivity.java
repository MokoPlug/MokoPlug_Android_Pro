package com.moko.mokoplugpro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.dialog.AlertMessageDialog;
import com.moko.mokoplugpro.dialog.ChangePasswordDialog;
import com.moko.mokoplugpro.dialog.LoadingDialog;
import com.moko.mokoplugpro.entity.PlugInfo;
import com.moko.mokoplugpro.event.DataChangedEvent;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingActivity extends BaseActivity {


    @BindView(R2.id.tv_title)
    TextView tvTitle;
    @BindView(R2.id.tv_change_password)
    TextView tvChangePassword;
    @BindView(R2.id.iv_connectStatus)
    ImageView ivConnectStatus;
    @BindView(R2.id.iv_password_verify_enable)
    ImageView ivPasswordVerifyEnable;
    @BindView(R2.id.iv_clear_energy_reset_enable)
    ImageView ivClearEnergyResetEnable;

    private PlugInfo mPlugInfo;

    private boolean mIsConnectEnable;
    private boolean mIsPasswordVerifyEnable;
    private boolean mIsClearEnergyResetEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mPlugInfo = getIntent().getParcelableExtra(AppConstants.EXTRA_KEY_PLUG_INFO);
        tvTitle.setText(mPlugInfo.name);
        EventBus.getDefault().register(this);
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getConnectEnable());
        orderTasks.add(OrderTaskAssembler.getPasswordVerifyEnable());
        orderTasks.add(OrderTaskAssembler.getClearEnergyEnable());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataChangedEvent(DataChangedEvent event) {
        String value = event.getValue();
        tvTitle.setText(value);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
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

            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissLoadingProgressDialog();
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
                                    case KEY_CONNECT_STATUS:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            mIsConnectEnable = status == 1;
                                            ivConnectStatus.setImageResource(mIsConnectEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                        }
                                        break;
                                    case KEY_PASSWORD_VERIFY_ENABLE:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            mIsPasswordVerifyEnable = status == 1;
                                            ivPasswordVerifyEnable.setImageResource(mIsPasswordVerifyEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
                                            tvChangePassword.setVisibility(mIsPasswordVerifyEnable ? View.VISIBLE : View.GONE);
                                        }
                                        break;
                                    case KEY_CLEAR_ENERGY_ENABLE:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            mIsClearEnergyResetEnable = status == 1;
                                            ivClearEnergyResetEnable.setImageResource(mIsClearEnergyResetEnable ? R.drawable.ic_checked : R.drawable.ic_unchecked);
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


    public void onAdvertisement(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, AdvInfoActivity.class);
        startActivity(intent);
    }

    public void onConnectable(View view) {
        if (isWindowLocked())
            return;
        mIsConnectEnable = !mIsConnectEnable;
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setConnectEnable(mIsConnectEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getConnectEnable());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void onPasswordVerifyEnable(View view) {
        if (isWindowLocked())
            return;
        if (mIsPasswordVerifyEnable) {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Disable verification");
            dialog.setMessage("No password required for the next connection, please confirm again whether to disable it.");
            dialog.setOnAlertConfirmListener(() -> {
                mIsPasswordVerifyEnable = false;
                showLoadingProgressDialog();
                List<OrderTask> orderTasks = new ArrayList<>();
                orderTasks.add(OrderTaskAssembler.setPasswordVerifyEnable(0));
                orderTasks.add(OrderTaskAssembler.getPasswordVerifyEnable());
                MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
            });
            dialog.show(getSupportFragmentManager());
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Enable verification");
            dialog.setMessage("Device will be disconnected, you need enter the password to connect.");
            dialog.setOnAlertConfirmListener(() -> {
                mIsPasswordVerifyEnable = true;
                showLoadingProgressDialog();
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPasswordVerifyEnable(1));
            });
            dialog.show(getSupportFragmentManager());
        }
    }

    public void onChangePassword(View view) {
        if (isWindowLocked())
            return;
        final ChangePasswordDialog dialog = new ChangePasswordDialog(this);
        dialog.setOnPasswordClicked(password -> {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.changePassword(password));
        });
        dialog.show();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override

            public void run() {
                runOnUiThread(() -> dialog.showKeyboard());
            }
        }, 200);
    }

    public void onPowerOnDefaultMode(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, PowerStatusActivity.class);
        startActivity(intent);
    }

    public void onPeriodicalReport(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, PeriodicalReportActivity.class);
        startActivity(intent);
    }

    public void onEnergyReport(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, EnergyReportActivity.class);
        startActivity(intent);
    }

    public void onProtectionSwitch(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, ProtectionSettingActivity.class);
        startActivity(intent);
    }

    public void onNotificationSwitch(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, LoadStatusNotifyActivity.class);
        startActivity(intent);
    }

    public void onIndicatorSetting(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, IndicatorSettingActivity.class);
        startActivity(intent);
    }

    public void onButtonSwitch(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, ButtonSettingActivity.class);
        startActivity(intent);
    }

    public void onClearEnergyReset(View view) {
        if (isWindowLocked())
            return;
        if (mIsClearEnergyResetEnable) {
            mIsClearEnergyResetEnable = false;
            showLoadingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setClearEnergyEnable(0));
            orderTasks.add(OrderTaskAssembler.getClearEnergyEnable());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Warning");
            dialog.setMessage("When turn on this option, when perform a reset, the energy data will be deleted.");
            dialog.setOnAlertConfirmListener(() -> {
                mIsClearEnergyResetEnable = true;
                showLoadingProgressDialog();
                List<OrderTask> orderTasks = new ArrayList<>();
                orderTasks.add(OrderTaskAssembler.setClearEnergyEnable(1));
                orderTasks.add(OrderTaskAssembler.getClearEnergyEnable());
                MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
            });
            dialog.show(getSupportFragmentManager());

        }
    }

    public void onSystemInfo(View view) {
        if (isWindowLocked())
            return;

        Intent intent = new Intent(this, SystemInfoActivity.class);
        startActivityForResult(intent, AppConstants.REQUEST_CODE_UPDATE);
    }

    public void onReset(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Device");
        dialog.setMessage("Please confirm again whether to reset the device.");
        dialog.setOnAlertConfirmListener(() -> {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.reset());
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onDisconnect(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Disconnect Device");
        dialog.setMessage("Please confirm again whether to disconnect the device.");
        dialog.setOnAlertConfirmListener(() -> {
            MokoSupport.getInstance().disConnectBle();
        });
        dialog.show(getSupportFragmentManager());
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
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void back(View view) {
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_UPDATE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}
