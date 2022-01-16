package com.example.speaktoothv4;
//The class for custom listview of paired devices

public class DeviceItem {
    //The name of the device
    public String name;
    //The color of his icon
    public int color;
    //The mac of the device
    public String mac;

    //Create object of the class device item
    public DeviceItem(String name, int color, String mac) {
        this.name = name;
        this.color = color;
        this.mac = mac;
    }

    public String getMac() {
        return mac;
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
