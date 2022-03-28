package com.moko.mokoplugpro.activity;


import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.dialog.AlertMessageDialog;
import com.moko.mokoplugpro.dialog.LoadingDialog;
import com.moko.mokoplugpro.entity.PlugInfo;
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
import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceInfoActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

    @BindView(R2.id.frame_container)
    FrameLayout frameContainer;
    @BindView(R2.id.tv_title)
    TextView tvTitle;
    @BindView(R2.id.radioBtn_power)
    RadioButton radioBtnPower;
    @BindView(R2.id.radioBtn_energy)
    RadioButton radioBtnEnergy;
    @BindView(R2.id.rg_options)
    RadioGroup rgOptions;
    @BindView(R2.id.radioBtn_switch)
    RadioButton radioBtnSwitch;
    private FragmentManager fragmentManager;
    private SwitchFragment switchFragment;
    private PowerFragment powerFragment;
    private EnergyFragment energyFragment;
    private PlugInfo mPlugInfo;
    private Handler mHandler;
    //    private boolean mIsOver;
    private String mOverStatusShown;
    private int mOverStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info_pro);
        ButterKnife.bind(this);
        initFragment();
        rgOptions.setOnCheckedChangeListener(this);
        radioBtnSwitch.setChecked(true);
        mPlugInfo = getIntent().getParcelableExtra(AppConstants.EXTRA_KEY_PLUG_INFO);
        tvTitle.setText(mPlugInfo.name);
        mHandler = new Handler(Looper.getMainLooper());
        EventBus.getDefault().register(this);
        if (mPlugInfo.overLoad == 1)
            mOverStatus = 1;
        if (mPlugInfo.overVoltage == 1)
            mOverStatus = 2;
        if (mPlugInfo.overCurrent == 1)
            mOverStatus = 3;
        if (mPlugInfo.sagVoltage == 1)
            mOverStatus = 4;
        if (mOverStatus > 0) {
//            mIsOver = true;
            showOverDialog();
            return;
        }
        showSyncingProgressDialog();
        mHandler.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setSystemTime());
            orderTasks.add(OrderTaskAssembler.getSwitchStatus());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }

    private void showOverDialog() {
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

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
//                    MokoSupport.getInstance().countDown = 0;
//                    MokoSupport.getInstance().countDownInit = 0;
                    setResult(RESULT_OK);
                    finish();
                }
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
                                            ToastUtils.showToast(this, status == 1 ? "Load starts" : "tops working now!");
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
                                }
                            }
                        }
                        break;
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
                                            showSyncingProgressDialog();
                                            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSystemTime());
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
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyHourly());
    }

    private void showPower() {
        fragmentManager.beginTransaction()
                .hide(switchFragment)
                .show(powerFragment)
                .hide(energyFragment)
                .commit();
        showSyncingProgressDialog();
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
        showSyncingProgressDialog();
        if (mPlugInfo.overLoad == 1) {
            XLog.i("清除过载状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverLoadClear());
        }
        if (mPlugInfo.overVoltage == 1) {
            XLog.i("清除过压状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverVoltageClear());
        }
        if (mPlugInfo.overCurrent == 1) {
            XLog.i("清除过流状态");
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setOverCurrentClear());
        }
        if (mPlugInfo.sagVoltage == 1) {
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
        showSyncingProgressDialog();
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
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setCountdown(countdown));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Energy
    ///////////////////////////////////////////////////////////////////////////


    public void getEnergyHourly() {
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyHourly());
    }

    public void getEnergyDaily() {
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyDaily());
    }

    public void getEnergyTotally() {
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getEnergyTotally());
    }

    public void onClearEnergyData(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Energy Data");
        dialog.setMessage("After reset, all energy data will be deleted, please confirm again whether to reset it?");
        dialog.setOnAlertConfirmListener(() -> {
            showSyncingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setEnergyClear());
            orderTasks.add(OrderTaskAssembler.getEnergyTotally());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        });
        dialog.show(getSupportFragmentManager());
    }


//    public void onModifyName(View view) {
//        if (isWindowLocked())
//            return;
//        startActivityForResult(new Intent(this, ModifyNameActivity.class), AppConstants.REQUEST_CODE_MODIFY_NAME);
//    }
//
//    public void onModifyPowerStatus(View view) {
//        if (isWindowLocked())
//            return;
//        // 修改上电状态
//        startActivityForResult(new Intent(this, ModifyPowerStatusActivity.class), AppConstants.REQUEST_CODE_MODIFY_POWER_STATUS);
//
//    }
//
//    public void onCheckUpdate(View view) {
//        if (isWindowLocked())
//            return;
//        // 升级
//        startActivityForResult(new Intent(this, FirmwareUpdateActivity.class), AppConstants.REQUEST_CODE_UPDATE);
//
//    }
//
//    public void onModifyAdvInterval(View view) {
//        if (isWindowLocked())
//            return;
//        // 修改广播间隔
//        startActivityForResult(new Intent(this, AdvIntervalActivity.class), AppConstants.REQUEST_CODE_ADV_INTERVAL);
//
//    }
//
//    public void onModifyOverloadValue(View view) {
//        if (isWindowLocked())
//            return;
//        // 修改过载保护值
//        startActivityForResult(new Intent(this, OverloadValueActivity.class), AppConstants.REQUEST_CODE_OVERLOAD_VALUE);
//
//    }
//
//    public void onModifyPowerReportInterval(View view) {
//        if (isWindowLocked())
//            return;
//        // 修改电能上报间隔
//        startActivityForResult(new Intent(this, EnergySavedIntervalActivity.class), AppConstants.REQUEST_CODE_ENERGY_SAVED_INTERVAL);
//    }
//
//    public void onModifyPowerChangeNotification(View view) {
//        if (isWindowLocked())
//            return;
//        // 修改电能变化百分比
//        startActivityForResult(new Intent(this, EnergySavedPercentActivity.class), AppConstants.REQUEST_CODE_ENERGY_SAVED_PERCENT);
//    }

//    public void onModifyEnergyConsumption(View view) {
//        if (isWindowLocked())
//            return;
//        // 重置累计电能
//        AlertMessageDialog dialog = new AlertMessageDialog();
//        dialog.setTitle("Reset Energy Consumption");
//        dialog.setMessage("Please confirm again whether to reset the accumulated electricity? Value will be recounted after clearing.");
//        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
//            @Override
//            public void onClick() {
//                showSyncingProgressDialog();
//                OrderTask orderTask = OrderTaskAssembler.writeResetEnergyTotal();
//                MokoSupport.getInstance().sendOrder(orderTask);
//            }
//        });
//        dialog.show(getSupportFragmentManager());
//    }
//
//    public void onReset(View view) {
//        if (isWindowLocked())
//            return;
//        AlertMessageDialog dialog = new AlertMessageDialog();
//        dialog.setTitle("Reset Device");
//        dialog.setMessage("After reset,the relevant data will be totally cleared");
//        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
//            @Override
//            public void onClick() {
//                showSyncingProgressDialog();
//                OrderTask orderTask = OrderTaskAssembler.writeReset();
//                MokoSupport.getInstance().sendOrder(orderTask);
//            }
//        });
//        dialog.show(getSupportFragmentManager());
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == AppConstants.REQUEST_CODE_MODIFY_NAME) {
//            if (resultCode == RESULT_OK) {
//                final String deviceName = MokoSupport.getInstance().advName;
//                settingFragment.setDeviceName(deviceName);
//                changeName();
//            }
//        }
//        if (requestCode == AppConstants.REQUEST_CODE_ADV_INTERVAL) {
//            if (resultCode == RESULT_OK) {
//                final int advInterval = MokoSupport.getInstance().advInterval;
//                settingFragment.setAdvInterval(advInterval);
//            }
//        }
//        if (requestCode == AppConstants.REQUEST_CODE_OVERLOAD_VALUE) {
//            if (resultCode == RESULT_OK) {
//                final int overloadTopValue = MokoSupport.getInstance().overloadTopValue;
//                settingFragment.setOverloadTopValue(overloadTopValue);
//            }
//        }
//        if (requestCode == AppConstants.REQUEST_CODE_ENERGY_SAVED_INTERVAL) {
//            if (resultCode == RESULT_OK) {
//                final int energySavedInterval = MokoSupport.getInstance().energySavedInterval;
//                settingFragment.setEnergySavedInterval(energySavedInterval);
//            }
//        }
//        if (requestCode == AppConstants.REQUEST_CODE_ENERGY_SAVED_PERCENT) {
//            if (resultCode == RESULT_OK) {
//                final int energySavedPercent = MokoSupport.getInstance().energySavedPercent;
//                settingFragment.setEnergySavedPercent(energySavedPercent);
//            }
//        }
        if (requestCode == AppConstants.REQUEST_CODE_UPDATE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    private LoadingDialog mLoadingDialog;

    public void showSyncingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    public void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }
}
