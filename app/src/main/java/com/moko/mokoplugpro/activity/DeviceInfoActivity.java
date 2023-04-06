package com.moko.mokoplugpro.activity;


import android.app.FragmentManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RadioGroup;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.databinding.ActivityDeviceInfoProBinding;
import com.moko.mokoplugpro.dialog.AlertMessageDialog;
import com.moko.mokoplugpro.dialog.LoadingDialog;
import com.moko.mokoplugpro.entity.PlugInfo;
import com.moko.mokoplugpro.event.DataChangedEvent;
import com.moko.mokoplugpro.fragment.EnergyFragment;
import com.moko.mokoplugpro.fragment.PowerFragment;
import com.moko.mokoplugpro.fragment.SwitchFragment;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IdRes;

public class DeviceInfoActivity extends BaseActivity<ActivityDeviceInfoProBinding> implements RadioGroup.OnCheckedChangeListener {

    private FragmentManager fragmentManager;
    private SwitchFragment switchFragment;
    private PowerFragment powerFragment;
    private EnergyFragment energyFragment;
    private PlugInfo mPlugInfo;
    private Handler mHandler;
    private String mOverStatusShown;
    private int mOverStatus;

    @Override
    protected void onCreate() {
        initFragment();
        mBind.rgOptions.setOnCheckedChangeListener(this);
        mBind.radioBtnSwitch.setChecked(true);
        mPlugInfo = getIntent().getParcelableExtra(AppConstants.EXTRA_KEY_PLUG_INFO);
        mBind.tvTitle.setText(mPlugInfo.name);
        mHandler = new Handler(Looper.getMainLooper());
        EventBus.getDefault().register(this);
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setSystemTime());
            orderTasks.add(OrderTaskAssembler.getSwitchStatus());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }

    @Override
    protected ActivityDeviceInfoProBinding getViewBinding() {
        return ActivityDeviceInfoProBinding.inflate(getLayoutInflater());
    }

    private void showOverDialog() {
        dismissLoadingProgressDialog();
        mOverStatusShown = "";
        if (mOverStatus == 1)
            mOverStatusShown = "overload";
        if (mOverStatus == 2)
            mOverStatusShown = "overvoltage";
        if (mOverStatus == 3)
            mOverStatusShown = "overcurrent";
        if (mOverStatus == 4)
            mOverStatusShown = "undervoltage";
        String message = String.format("Detect the socket %s, please confirm whether to exit the %s status by APP?", mOverStatusShown, mOverStatusShown);
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(message);
        dialog.setOnAlertCancelListener(() -> {
            MokoSupport.getInstance().disConnectBle();
        });
        dialog.setOnAlertConfirmListener(() -> {
            showClearOverStatusDialog();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void showClearOverStatusDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(String.format("If YES, the socket will exit %s status, and please make sure it is within the protection threshold. If NO, you need manually reboot it to exit this status.", mOverStatusShown));
        dialog.setOnAlertCancelListener(() -> {
            MokoSupport.getInstance().disConnectBle();
        });
        dialog.setOnAlertConfirmListener(() -> {
            showLoadingProgressDialog();
            clearOverStatus();
        });
        dialog.show(getSupportFragmentManager());

    }

    private void initFragment() {
        fragmentManager = getFragmentManager();
        powerFragment = PowerFragment.newInstance();
        energyFragment = EnergyFragment.newInstance();
        switchFragment = SwitchFragment.newInstance();
        fragmentManager.beginTransaction()
                .add(R.id.frame_container, powerFragment)
                .add(R.id.frame_container, energyFragment)
                .add(R.id.frame_container, switchFragment)
                .hide(powerFragment)
                .hide(energyFragment)
                .show(switchFragment)
                .commit();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataChangedEvent(DataChangedEvent event) {
        String value = event.getValue();
        mBind.tvTitle.setText(value);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
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
                                    case KEY_LOAD_STATUS:
                                        if (length > 0) {
                                            int status = value[4] & 0xFF;
                                            ToastUtils.showToast(this, status == 1 ? "Load starts working now!" : "Load stops working now!");
                                        }
                                        break;
                                    case KEY_OVER_LOAD:
                                        if (length > 0 && value[4] == 1) {
                                            mOverStatus = 1;
                                            showOverDialog();
                                        }
                                        break;
                                    case KEY_OVER_VOLTAGE:
                                        if (length > 0 && value[4] == 1) {
                                            mOverStatus = 2;
                                            showOverDialog();
                                        }
                                        break;
                                    case KEY_OVER_CURRENT:
                                        if (length > 0 && value[4] == 1) {
                                            mOverStatus = 3;
                                            showOverDialog();
                                        }
                                        break;
                                    case KEY_SAG_VOLTAGE:
                                        if (length > 0 && value[4] == 1) {
                                            mOverStatus = 4;
                                            showOverDialog();
                                        }
                                        break;
                                    case KEY_DISCONNECT:
                                        if (length > 0) {
                                            int disconnectType = value[4] & 0xFF;
                                            if (disconnectType == 2)
                                                ToastUtils.showToast(this, "Bluetooth disconnect!");
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
                            if (flag == 0x01) {
                                switch (configKeyEnum) {
                                    case KEY_OVER_LOAD_CLEAR:
                                    case KEY_OVER_VOLTAGE_CLEAR:
                                    case KEY_OVER_CURRENT_CLEAR:
                                    case KEY_SAG_VOLTAGE_CLEAR:
                                        if (length > 0 && value[4] == 1) {
                                            mOverStatus = 0;
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                switch (configKeyEnum) {
                                    case KEY_SWITCH_STATUS:
                                        if (length == 5) {
                                            if (value[5] == 1)
                                                mOverStatus = 1;
                                            if (value[7] == 1)
                                                mOverStatus = 2;
                                            if (value[6] == 1)
                                                mOverStatus = 3;
                                            if (value[8] == 1)
                                                mOverStatus = 4;
                                            if (mOverStatus > 0)
                                                showOverDialog();
                                        }
                                        break;
                                }
                            }
                        }
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onBack(View view) {
        if (isWindowLocked())
            return;
        back();
    }

    public void onSetting(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, SettingActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_PLUG_INFO, mPlugInfo);
        startActivityForResult(intent, AppConstants.REQUEST_CODE_UPDATE);
    }

    private void back() {
        if (MokoSupport.getInstance().isBluetoothOpen()) {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Disconnect Device");
            dialog.setMessage("Please confirm again whether to disconnect the device.");
            dialog.setOnAlertConfirmListener(() -> {
                MokoSupport.getInstance().disConnectBle();
            });
            dialog.show(getSupportFragmentManager());
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.radioBtn_switch) {
            showSwitch();
        } else if (checkedId == R.id.radioBtn_power) {
            showPower();
        } else if (checkedId == R.id.radioBtn_energy) {
            showEnergy();
        }
    }

    private void showEnergy() {
        fragmentManager.beginTransaction()
                .hide(switchFragment)
                .hide(powerFragment)
                .show(energyFragment)
                .commit();
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyHourly());
    }

    private void showPower() {
        fragmentManager.beginTransaction()
                .hide(switchFragment)
                .show(powerFragment)
                .hide(energyFragment)
                .commit();
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getPowerData());
    }

    private void showSwitch() {
        fragmentManager.beginTransaction()
                .show(switchFragment)
                .hide(powerFragment)
                .hide(energyFragment)
                .commit();
    }


    private void clearOverStatus() {
        if (mOverStatus == 1) {
            XLog.i("清除过载状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverLoadClear());
        }
        if (mOverStatus == 2) {
            XLog.i("清除过压状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverVoltageClear());
        }
        if (mOverStatus == 3) {
            XLog.i("清除过流状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverCurrentClear());
        }
        if (mOverStatus == 4) {
            XLog.i("清除欠压状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSagVoltageClear());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Switch
    ///////////////////////////////////////////////////////////////////////////

    public void onChangeSwitch(View view) {
        if (isWindowLocked())
            return;
        switchFragment.changeSwitchState();
    }

    public void changeSwitchState(boolean onOff) {
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setSwitchStatus(onOff ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getSwitchStatus());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public boolean getSwitchState() {
        return switchFragment.getSwitchState();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Timer
    ///////////////////////////////////////////////////////////////////////////
    public void onTimer(View view) {
        if (isWindowLocked())
            return;
        switchFragment.setTimer();
    }

    public void setTimer(int countdown) {
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setCountdown(countdown));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Energy
    ///////////////////////////////////////////////////////////////////////////


    public void getEnergyHourly() {
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyHourly());
    }

    public void getEnergyDaily() {
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyDaily());
    }

    public void getEnergyTotally() {
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyTotally());
    }

    public void onClearEnergyData(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Energy Data");
        dialog.setMessage("After reset, all energy data will be deleted, please confirm again whether to reset it?");
        dialog.setOnAlertConfirmListener(() -> {
            showLoadingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setEnergyClear());
            orderTasks.add(OrderTaskAssembler.getEnergyTotally());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        });
        dialog.show(getSupportFragmentManager());
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
