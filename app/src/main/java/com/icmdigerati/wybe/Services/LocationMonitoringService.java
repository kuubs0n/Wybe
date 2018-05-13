package com.icmdigerati.wybe.Services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.storage.StorageReference;
import com.icmdigerati.wybe.Settings.Constants;

    public class LocationMonitoringService extends Service implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            LocationListener {


        private static final String TAG = LocationMonitoringService.class.getSimpleName();
        GoogleApiClient mLocationClient;
        LocationRequest mLocationRequest = new LocationRequest();

        public static final String ACTION_LOCATION_BROADCAST = LocationMonitoringService.class.getName() + "LocationBroadcast";
        public static final String EXTRA_LATITUDE = "extra_latitude";
        public static final String EXTRA_LONGITUDE = "extra_longitude";

        private StorageReference mStorageRef;

        @Override
        public void onCreate() {
            super.onCreate();

            if (Build.VERSION.SDK_INT >= 26) {
                String CHANNEL_ID = "my_channel_01";
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_NONE);

                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("WYBE")
                        .setContentText("Tracking...")
                        .build();

                startForeground(1, notification);
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            mLocationClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mLocationRequest.setInterval(Constants.LOCATION_INTERVAL);
            mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
            mLocationRequest.setSmallestDisplacement(Constants.SMALLEST_DISPLACEMENT);
            int priority = LocationRequest.PRIORITY_HIGH_ACCURACY; //by default HIGH_POWER
            //PRIORITY_BALANCED_POWER_ACCURACY, PRIORITY_LOW_POWER, PRIORITY_NO_POWER are the other priority modes

            mLocationRequest.setPriority(priority);
            mLocationClient.connect();

            //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
            return START_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        /*
         * LOCATION CALLBACKS
         */
        @Override
        public void onConnected(Bundle dataBundle) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

            Log.d(TAG, "Connected to Google API");
        }

        /*
         * Called by Location Services if the connection to the
         * location client drops because of an error.
         */
        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Connection suspended");
        }


        //to get the location change
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Location changed");


            if (location != null) {
                Log.d(TAG, "== location != null");

                //Send result to activities
                sendMessageToUI(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            }

        }

        private void sendMessageToUI(String lat, String lng) {

            Log.d(TAG, "Sending info...");

            Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
            intent.putExtra(EXTRA_LATITUDE, lat);
            intent.putExtra(EXTRA_LONGITUDE, lng);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        }


        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "Failed to connect to Google API");

        }
    }
