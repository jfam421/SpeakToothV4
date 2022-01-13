package com.example.speaktoothv4;

public class DeviceItem {
   String name;
   int color;
   String mac;

    public DeviceItem(String name, int color, String mac) {
        this.name = name;
        this.color = color;
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
