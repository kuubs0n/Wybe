package com.icmdigerati.wybe.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class UploadPackageService extends Service {

    private StorageReference mStorageRef;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_02";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_NONE);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("WYBE")
                    .setContentText("Tracking...")
                    .build();

            startForeground(2, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Bundle data = intent.getExtras();
        ArrayList<LatLng> latLngs = data.getParcelableArrayList("CORDS");

        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();

        File file = new File(Environment.getExternalStorageDirectory(), "cords" + df.format(cal.getTime())  + ".txt");

        if(!file.exists()) {
            try {
                file.createNewFile();
                FileWriter fw = new FileWriter(file);
                for(LatLng cord : latLngs) {
                    fw.write("LAT: " + cord.latitude + " LONG: " + cord.longitude);
                }
                fw.close();

                mStorageRef = FirebaseStorage.getInstance().getReference();

                Uri fileToUpload = Uri.fromFile(file);
                StorageReference ref = mStorageRef.child("cords/" + file.getName());

                ref.putFile(fileToUpload);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return START_NOT_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
