package com.example.speaktoothv4;

public class UserData {
    //Name of the saved user and his icon color
    public String name;
    public int color;

    //Create object of the class user
    public UserData(String name, int color) {
        this.color = color;
        this.name = name;
    }
    //return name of the user
    public String getName() {
        return name;
    }
    //return icon color of the user
    public int getColor() {
        return color;
    }

}
