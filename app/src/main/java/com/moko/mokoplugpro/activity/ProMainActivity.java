package com.moko.mokoplugpro.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mokoplugpro.AppConstants;
import com.moko.mokoplugpro.BuildConfig;
import com.moko.mokoplugpro.utils.PlugInfoAnalysisImpl;
import com.moko.mokoplugpro.R;
import com.moko.mokoplugpro.R2;
import com.moko.mokoplugpro.adapter.PlugListAdapter;
import com.moko.mokoplugpro.dialog.AlertMessageDialog;
import com.moko.mokoplugpro.dialog.LoadingDialog;
import com.moko.mokoplugpro.dialog.LoadingMessageDialog;
import com.moko.mokoplugpro.dialog.PasswordDialog;
import com.moko.mokoplugpro.dialog.ScanFilterDialog;
import com.moko.mokoplugpro.entity.PlugInfo;
import com.moko.mokoplugpro.utils.SPUtiles;
import com.moko.mokoplugpro.utils.ToastUtils;
import com.moko.support.pro.MokoBleScanner;
import com.moko.support.pro.MokoSupport;
import com.moko.support.pro.OrderTaskAssembler;
import com.moko.support.pro.callback.MokoScanDeviceCallback;
import com.moko.support.pro.entity.DeviceInfo;
import com.moko.support.pro.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;


public class ProMainActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemChildClickListener {


    @BindView(R2.id.rv_devices)
    RecyclerView rvDevices;
    @BindView(R2.id.iv_refresh)
    ImageView ivRefresh;
    @BindView(R2.id.tv_device_num)
    TextView tvDeviceNum;
    @BindView(R2.id.tv_filter)
    TextView tvFilter;
    @BindView(R2.id.rl_filter)
    RelativeLayout rlFilter;
    @BindView(R2.id.rl_edit_filter)
    RelativeLayout rlEditFilter;
    private boolean mReceiverTag = false;
    private ConcurrentHashMap<String, PlugInfo> plugInfoHashMap;
    private ArrayList<PlugInfo> plugInfos;
    private PlugInfoAnalysisImpl plugInfoParseable;
    private PlugListAdapter adapter;
    private Handler mHandler;
    private boolean isPasswordError;
    private MokoBleScanner mokoBleScanner;
    private PlugInfo mSelectedPlugInfo;

    public static String PATH_LOGCAT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pro);
        ButterKnife.bind(this);
        // 初始化Xlog
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MokoPlug" : "MokoPlugPro");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MokoPlug" : "MokoPlugPro");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MokoPlug" : "MokoPlugPro");
        }
        MokoSupport.getInstance().init(getApplicationContext());
        mSavedPassword = SPUtiles.getStringValue(this, AppConstants.SP_KEY_SAVED_PASSWORD, "");
        plugInfoHashMap = new ConcurrentHashMap<>();
        plugInfos = new ArrayList<>();
        adapter = new PlugListAdapter();
        adapter.replaceData(plugInfos);
        adapter.setOnItemChildClickListener(this);
        adapter.openLoadAnimation();
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.shape_recycleview_divider));
        rvDevices.addItemDecoration(itemDecoration);
        rvDevices.setAdapter(adapter);
        mHandler = new Handler(Looper.getMainLooper());
        mokoBleScanner = new MokoBleScanner(this);
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            if (animation == null) {
                startScan();
            }
        }
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
                            if (animation != null) {
                                mHandler.removeMessages(0);
                                mokoBleScanner.stopScanDevice();
                                onStopScan();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (animation == null) {
                                startScan();
                            }
                            break;

                    }
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            mSelectedPlugInfo = null;
            mPassword = "";
            // 设备断开，通知页面更新
            dismissLoadingProgressDialog();
            dismissLoadingMessageDialog();
            if (isPasswordError) {
                isPasswordError = false;
            } else {
                ToastUtils.showToast(this, "Connection Failed, please try again");
            }
            if (animation == null) {
                startScan();
            }
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            dismissLoadingProgressDialog();
            if (TextUtils.isEmpty(mPassword)) {
                // 跳转详情
                Intent intent = new Intent(this, DeviceInfoActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_PLUG_INFO, mSelectedPlugInfo);
                startActivityForResult(intent, AppConstants.REQUEST_CODE_DEVICE_INFO);
            } else {
                showLoadingMessageDialog();
                mHandler.postDelayed(() -> {
                    // open password notify and set password
                    List<OrderTask> orderTasks = new ArrayList<>();
                    orderTasks.add(OrderTaskAssembler.setPassword(mPassword));
                    MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                }, 500);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PASSWORD:
                    MokoSupport.getInstance().disConnectBle();
                    break;
            }
        }
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingMessageDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PASSWORD:
                    if (value.length == 5) {
                        dismissLoadingMessageDialog();
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xED)
                            return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x01 && cmd == 0x01 && length == 0x01) {
                            int result = value[4] & 0xFF;
                            if (1 == result) {
                                mSavedPassword = mPassword;
                                SPUtiles.setStringValue(ProMainActivity.this, AppConstants.SP_KEY_SAVED_PASSWORD, mSavedPassword);
                                XLog.i("Success");
                                Intent intent = new Intent(this, DeviceInfoActivity.class);
                                intent.putExtra(AppConstants.EXTRA_KEY_PLUG_INFO, mSelectedPlugInfo);
                                startActivityForResult(intent, AppConstants.REQUEST_CODE_DEVICE_INFO);
                            }
                            if (0 == result) {
                                isPasswordError = true;
                                ToastUtils.showToast(ProMainActivity.this, "Password Error!");
                                MokoSupport.getInstance().disConnectBle();
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case AppConstants.REQUEST_CODE_DEVICE_INFO:
                    if (animation == null)
                        startScan();
                    break;

            }
        }
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

    private void startScan() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        findViewById(R.id.iv_refresh).startAnimation(animation);
        plugInfoParseable = new PlugInfoAnalysisImpl();
        mokoBleScanner.startScanDevice(this);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mokoBleScanner.stopScanDevice();
            }
        }, 1000 * 60);
    }


    @Override
    public void onStartScan() {
        plugInfoHashMap.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (animation != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.replaceData(plugInfos);
                            tvDeviceNum.setText(String.format("DEVICE(%d)", plugInfos.size()));
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    updateDevices();
                }
            }
        }).start();
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        final PlugInfo plugInfo = plugInfoParseable.parseDeviceInfo(deviceInfo);
        if (plugInfo == null)
            return;
        plugInfoHashMap.put(plugInfo.mac, plugInfo);
    }

    @Override
    public void onStopScan() {
        findViewById(R.id.iv_refresh).clearAnimation();
        animation = null;
    }

    private void updateDevices() {
        plugInfos.clear();
        if (!TextUtils.isEmpty(filterName) || filterRssi != -100) {
            ArrayList<PlugInfo> plugInfosFilter = new ArrayList<>(plugInfoHashMap.values());
            Iterator<PlugInfo> iterator = plugInfosFilter.iterator();
            while (iterator.hasNext()) {
                PlugInfo plugInfo = iterator.next();
                if (plugInfo.rssi > filterRssi) {
                    if (TextUtils.isEmpty(filterName)) {
                        continue;
                    } else {
                        if (TextUtils.isEmpty(plugInfo.name) && TextUtils.isEmpty(plugInfo.mac)) {
                            iterator.remove();
                        } else if (TextUtils.isEmpty(plugInfo.name) && plugInfo.mac.toLowerCase().replaceAll(":", "").contains(filterName.toLowerCase())) {
                            continue;
                        } else if (TextUtils.isEmpty(plugInfo.mac) && plugInfo.name.toLowerCase().contains(filterName.toLowerCase())) {
                            continue;
                        } else if (!TextUtils.isEmpty(plugInfo.name) && !TextUtils.isEmpty(plugInfo.mac) && (plugInfo.name.toLowerCase().contains(filterName.toLowerCase()) || plugInfo.mac.toLowerCase().replaceAll(":", "").contains(filterName.toLowerCase()))) {
                            continue;
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            plugInfos.addAll(plugInfosFilter);
        } else {
            plugInfos.addAll(plugInfoHashMap.values());
        }
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(plugInfos, new Comparator<PlugInfo>() {
            @Override
            public int compare(PlugInfo lhs, PlugInfo rhs) {
                if (lhs.rssi > rhs.rssi) {
                    return -1;
                } else if (lhs.rssi < rhs.rssi) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private Animation animation = null;
    public String filterName;
    public int filterRssi = -100;

    public void onRefresh(View view) {
        if (isWindowLocked())
            return;
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        if (animation == null) {
            startScan();
        } else {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
    }

    public void onAbout(View view) {
        if (isWindowLocked())
            return;
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void onFilterDelete(View view) {
        if (isWindowLocked())
            return;
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        rlFilter.setVisibility(View.GONE);
        rlEditFilter.setVisibility(View.VISIBLE);
        filterName = "";
        filterRssi = -100;
        if (isWindowLocked())
            return;
        if (animation == null) {
            startScan();
        }
    }

    public void onFilter(View view) {
        if (isWindowLocked())
            return;
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        ScanFilterDialog scanFilterDialog = new ScanFilterDialog();
        scanFilterDialog.setFilterName(filterName);
        scanFilterDialog.setFilterRssi(filterRssi);
        scanFilterDialog.setOnScanFilterListener(new ScanFilterDialog.OnScanFilterListener() {
            @Override
            public void onDone(String filterName, int filterRssi) {
                ProMainActivity.this.filterName = filterName;
                ProMainActivity.this.filterRssi = filterRssi;
                if (!TextUtils.isEmpty(filterName) || filterRssi != -100) {
                    rlFilter.setVisibility(View.VISIBLE);
                    rlEditFilter.setVisibility(View.GONE);
                    StringBuilder stringBuilder = new StringBuilder();
                    if (!TextUtils.isEmpty(filterName)) {
                        stringBuilder.append(filterName);
                        stringBuilder.append(";");
                    }
                    if (filterRssi != -100) {
                        stringBuilder.append(String.format("%sdBm", filterRssi + ""));
                        stringBuilder.append(";");
                    }
                    tvFilter.setText(stringBuilder.toString());
                } else {
                    rlFilter.setVisibility(View.GONE);
                    rlEditFilter.setVisibility(View.VISIBLE);
                }
                if (isWindowLocked())
                    return;
                if (animation == null) {
                    startScan();
                }
            }

            @Override
            public void onDismiss() {
                if (isWindowLocked())
                    return;
                if (animation == null) {
                    startScan();
                }
            }
        });
        scanFilterDialog.show(getSupportFragmentManager());
    }

    private String mPassword;
    private String mSavedPassword;
    private String mSelectedDeviceMac;

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        final PlugInfo plugInfo = (PlugInfo) adapter.getItem(position);
        if (plugInfo != null && plugInfo.isConnectable != 0 && !isFinishing()) {
            if (animation != null) {
                mHandler.removeMessages(0);
                mokoBleScanner.stopScanDevice();
            }
            mSelectedPlugInfo = plugInfo;
            mSelectedDeviceMac = plugInfo.mac.replaceAll(":", "");
            if (plugInfo.isNeedVerify == 1) {
                showPasswordDialog(plugInfo);
                return;
            }
            showLoadingProgressDialog();
            ivRefresh.postDelayed(() -> {
                MokoSupport.getInstance().connDevice(plugInfo.mac);
            }, 500);
        }
    }

    private void showPasswordDialog(PlugInfo plugInfo) {
        final PasswordDialog dialog = new PasswordDialog(this);
        dialog.setData(mSavedPassword);
        dialog.setOnPasswordClicked(new PasswordDialog.PasswordClickListener() {
            @Override
            public void onEnsureClicked(String password) {
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    MokoSupport.getInstance().enableBluetooth();
                    return;
                }
                XLog.i(password);
                mPassword = password;
                showLoadingProgressDialog();
                ivRefresh.postDelayed(() -> {
                    MokoSupport.getInstance().connDevice(plugInfo.mac);
                }, 500);
            }

            @Override
            public void onDismiss() {
                if (animation == null) {
                    startScan();
                }
            }
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

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    private void showLoadingMessageDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Verifying..");
        mLoadingMessageDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingMessageDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }


    public void onBack(View view) {
        if (isWindowLocked())
            return;
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(() -> ProMainActivity.this.finish());
            dialog.show(getSupportFragmentManager());
        }
    }
}
