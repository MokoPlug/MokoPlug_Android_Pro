package com.moko.mokoplugpro.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.moko.mokoplugpro.dialog.ChangePasswordDialog;
import com.moko.mokoplugpro.dialog.LoadingDialog;
import com.moko.mokoplugpro.entity.PlugInfo;
import com.moko.mokoplugpro.service.DfuService;
import com.moko.mokoplugpro.utils.FileUtils;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.entity.ConfigKeyEnum;
import com.moko.support.pro.entity.NotifyKeyEnum;
import com.moko.support.pro.entity.OrderCHAR;
import com.moko.support.pro.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class SettingActivity extends BaseActivity {

    public static final int REQUEST_CODE_SELECT_FIRMWARE = 0x10;

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
    private Handler mHandler;

    private boolean mIsConnectEnable;
    private boolean mIsPasswordVerifyEnable;
    private boolean mIsClearEnergyResetEnable;
    private boolean isUpdate;
    private String mDeviceMac;
    private String mDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mPlugInfo = getIntent().getParcelableExtra(AppConstants.EXTRA_KEY_PLUG_INFO);
        tvTitle.setText(mPlugInfo.name);
        mHandler = new Handler(Looper.getMainLooper());
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getConnectEnable());
        orderTasks.add(OrderTaskAssembler.getPasswordVerifyEnable());
        orderTasks.add(OrderTaskAssembler.getClearEnergyEnable());
        orderTasks.add(OrderTaskAssembler.getAdvName());
        orderTasks.add(OrderTaskAssembler.getMac());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                if (isUpdate) {
                    return;
                }
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
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
                                    case KEY_ADV_NAME:
                                        if (length > 0) {
                                            byte[] advNameBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                            mDeviceName = new String(advNameBytes);
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                    case CHAR_MAC:
                        StringBuffer stringBuffer = new StringBuffer(new String(value));
                        stringBuffer.insert(2, ":");
                        stringBuffer.insert(5, ":");
                        stringBuffer.insert(8, ":");
                        stringBuffer.insert(11, ":");
                        stringBuffer.insert(14, ":");
                        mDeviceMac = stringBuffer.toString();
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
        showSyncingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setConnectEnable(mIsConnectEnable ? 1 : 0));
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
                showSyncingProgressDialog();
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPasswordVerifyEnable(0));
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getPasswordVerifyEnable());
            });
            dialog.show(getSupportFragmentManager());
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Enable verification");
            dialog.setMessage("Device will be disconnected, you need enter the password to connect.");
            dialog.setOnAlertConfirmListener(() -> {
                mIsPasswordVerifyEnable = true;
                showSyncingProgressDialog();
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPasswordVerifyEnable(1));
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getPasswordVerifyEnable());
            });
            dialog.show(getSupportFragmentManager());
        }
    }

    public void onChangePassword(View view) {
        if (isWindowLocked())
            return;
        final ChangePasswordDialog dialog = new ChangePasswordDialog(this);
        dialog.setOnPasswordClicked(password -> {
            showSyncingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPassword(password));
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
            showSyncingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setClearEnergyEnable(0));
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getClearEnergyEnable());
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Warning");
            dialog.setMessage("When turn on this option, when perform a reset, the energy data will be deleted.");
            dialog.setOnAlertConfirmListener(() -> {
                mIsPasswordVerifyEnable = true;
                showSyncingProgressDialog();
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPasswordVerifyEnable(1));
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getClearEnergyEnable());
            });
            dialog.show(getSupportFragmentManager());

        }
    }

    public void onCheckUpdate(View view) {
        if (isWindowLocked())
            return;
        chooseFirmwareFile();
    }

    public void onSystemInfo(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, SystemInfoActivity.class);
        startActivity(intent);
    }

    public void onReset(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Device");
        dialog.setMessage("Please confirm again whether to reset the device.");
        dialog.setOnAlertConfirmListener(() -> {
            showSyncingProgressDialog();
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

    public void showSyncingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    public void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FIRMWARE) {
            if (resultCode == RESULT_OK) {
                //得到uri，后面就是将uri转化成file的过程。
                Uri uri = data.getData();
                String firmwareFilePath = FileUtils.getPath(this, uri);
                if (TextUtils.isEmpty(firmwareFilePath))
                    return;
                final File firmwareFile = new File(firmwareFilePath);
                if (firmwareFile.exists()) {
                    final DfuServiceInitiator starter = new DfuServiceInitiator(mDeviceMac)
                            .setDeviceName(mDeviceName)
                            .setKeepBond(false)
                            .setDisableNotification(true);
                    starter.setZip(null, firmwareFilePath);
                    starter.start(this, DfuService.class);
                    showDFUProgressDialog("Waiting...");
                    isUpdate = true;
                } else {
                    ToastUtils.showToast(this, "file is not exists!");
                }
            }
        }
    }

    public void chooseFirmwareFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_FIRMWARE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    private ProgressDialog mDFUDialog;

    private void showDFUProgressDialog(String tips) {
        mDFUDialog = new ProgressDialog(SettingActivity.this);
        mDFUDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDFUDialog.setCanceledOnTouchOutside(false);
        mDFUDialog.setCancelable(false);
        mDFUDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDFUDialog.setMessage(tips);
        if (!isFinishing() && mDFUDialog != null && !mDFUDialog.isShowing()) {
            mDFUDialog.show();
        }
    }

    private void dismissDFUProgressDialog() {
        mDeviceConnectCount = 0;
        if (!isFinishing() && mDFUDialog != null && mDFUDialog.isShowing()) {
            mDFUDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
        builder.setTitle("Dismiss");
        builder.setCancelable(false);
        builder.setMessage("The device disconnected!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_OK);
                finish();
            }
        });
        builder.show();
    }

    private int mDeviceConnectCount;

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            XLog.w("onDeviceConnecting...");
            mDeviceConnectCount++;
            if (mDeviceConnectCount > 3) {
                Toast.makeText(SettingActivity.this, "Error:DFU Failed", Toast.LENGTH_SHORT).show();
                dismissDFUProgressDialog();
                final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(SettingActivity.this);
                final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
                abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
                manager.sendBroadcast(abortAction);
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            XLog.w("onDeviceDisconnecting...");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mDFUDialog.setMessage("DfuProcessStarting...");
        }


        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mDFUDialog.setMessage("EnablingDfuMode...");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mDFUDialog.setMessage("FirmwareValidating...");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            ToastUtils.showToast(SettingActivity.this, "DFU Successfully!");
            isUpdate = !isUpdate;
            dismissDFUProgressDialog();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            mDFUDialog.setMessage("DfuAborted...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mDFUDialog.setMessage("Progress:" + percent + "%");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            ToastUtils.showToast(SettingActivity.this, "Opps!DFU Failed. Please try again!");
            XLog.i("Error:" + message);
            isUpdate = !isUpdate;
            dismissDFUProgressDialog();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void back(View view) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }
}
