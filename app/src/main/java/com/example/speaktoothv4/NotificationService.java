package com.example.speaktoothv4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationService extends Service {

    public class NotificationServiceBinder extends Binder {
        public NotificationService getNotificationService() {
            return NotificationService.this;
        }
    }

    private final IBinder mBinder = new NotificationServiceBinder();
    public boolean serviceIsRunning;

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }

    //On start service change boolean check to true;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public void sendNotification(String name, String text) {
        //Setup notification
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, "Msg")
                .setSmallIcon(R.drawable.chat1)
                .setContentTitle("New message from: " + name)
                .setContentText("Open the application to check it")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        //Check sdk version because if sdk > 27 so there is notification channel have to be created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Msg", "Msg", importance);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        //Make notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder2.build());
    }

    @Override
    public void onDestroy() {
        //Destroy the service and set isRunning to false
        serviceIsRunning = false;
        super.onDestroy();
    }
}