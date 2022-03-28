package com.moko.mokoplugpro.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class PlugInfo implements Parcelable {
    public String name;
    public String mac;
    public int rssi;
    public String voltage;
    public String current;
    public String power;
    public String powerFactor;
    public String currentRate;
    public String energyTotal;
    public String txPower;
    public int onOff = 0;
    public int loadState = 0;
    public int overLoad = 0;
    public int overCurrent = 0;
    public int overVoltage = 0;
    public  int sagVoltage = 0;
    public int isNeedVerify = 0;
    public int isConnectable = 0;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.mac);
        dest.writeInt(this.rssi);
        dest.writeString(this.voltage);
        dest.writeString(this.current);
        dest.writeString(this.power);
        dest.writeString(this.powerFactor);
        dest.writeString(this.currentRate);
        dest.writeString(this.energyTotal);
        dest.writeString(this.txPower);
        dest.writeInt(this.onOff);
        dest.writeInt(this.loadState);
        dest.writeInt(this.overLoad);
        dest.writeInt(this.overCurrent);
        dest.writeInt(this.overVoltage);
        dest.writeInt(this.sagVoltage);
        dest.writeInt(this.isNeedVerify);
        dest.writeInt(this.isConnectable);
    }

    public PlugInfo() {
    }

    protected PlugInfo(Parcel in) {
        this.name = in.readString();
        this.mac = in.readString();
        this.rssi = in.readInt();
        this.voltage = in.readString();
        this.current = in.readString();
        this.power = in.readString();
        this.powerFactor = in.readString();
        this.currentRate = in.readString();
        this.energyTotal = in.readString();
        this.txPower = in.readString();
        this.onOff = in.readInt();
        this.loadState = in.readInt();
        this.overLoad = in.readInt();
        this.overCurrent = in.readInt();
        this.overVoltage = in.readInt();
        this.sagVoltage = in.readInt();
        this.isNeedVerify = in.readInt();
        this.isConnectable = in.readInt();
    }

    public static final Parcelable.Creator<PlugInfo> CREATOR = new Parcelable.Creator<PlugInfo>() {
        @Override
        public PlugInfo createFromParcel(Parcel source) {
            return new PlugInfo(source);
        }

        @Override
        public PlugInfo[] newArray(int size) {
            return new PlugInfo[size];
        }
    };
}
