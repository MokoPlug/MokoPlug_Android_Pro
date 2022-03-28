package com.moko.support.pro.service;

import com.moko.support.pro.entity.DeviceInfo;

public interface DeviceInfoAnalysis<T> {
    T parseDeviceInfo(DeviceInfo deviceInfo);
}
