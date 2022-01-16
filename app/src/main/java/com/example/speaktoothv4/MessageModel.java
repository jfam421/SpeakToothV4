package com.example.speaktoothv4;
//The class that contains info about the message(type and text)

public class MessageModel {
    //Text of the message
    //Type of the message
    //sender messageType = 1 (message out); sender messageType = 0 (message in)
    public String message;
    public int messageType;
    // Constructor of the class object
    public MessageModel(String message, int messageType) {
        this.message = message;
        this.messageType = messageType;
    }
}