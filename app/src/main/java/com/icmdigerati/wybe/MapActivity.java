package com.icmdigerati.wybe;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.icmdigerati.wybe.Services.LocationMonitoringService;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private boolean mAlreadyStartedService = false;
    private TextView mMsgView;
    private GoogleMap mMap;
    private Polyline mPolyline;
    private List<LatLng> mPolylinePoints = new ArrayList<>();
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mMsgView = findViewById(R.id.tvCoords);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String latitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE);
                        String longitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE);

                        if (latitude != null && longitude != null) {
                            mMsgView.setText(getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude);
                        }

                        Location currentLocation = new Location("");
                        currentLocation.setLatitude(Double.parseDouble(latitude));
                        currentLocation.setLongitude(Double.parseDouble(longitude));

                        UpdateLocation(currentLocation);
                    }
                }, new IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mPolyline = mMap.addPolyline(new PolylineOptions()
                .color(Color.BLUE)
                .width(5f));
    }

    @Override
    public void onResume() {
        super.onResume();
        startStep1();
    }

    private void UpdateLocation(Location location) {
        LatLng myCoordinates = new LatLng(location.getLatitude(), location.getLongitude());

        if(mPolylinePoints.size() > 0) {
            LatLng lastCoordinates = mPolylinePoints.get(mPolylinePoints.size() - 1);

            if (lastCoordinates != null) {
                double roundedLastLatitude = (double) Math.round(lastCoordinates.latitude * 10000d) / 10000d;
                double roundedLastLongitude = (double) Math.round(lastCoordinates.longitude * 10000d) / 10000d;

                double roundedNewLatitude = (double) Math.round(location.getLatitude() * 10000d) / 10000d;
                double roundedNewLongitude = (double) Math.round(location.getLongitude() * 10000d) / 10000d;

                if (roundedLastLatitude != roundedNewLatitude || roundedLastLongitude != roundedNewLongitude) {
                    mPolylinePoints.add(myCoordinates);
                    mPolyline.setPoints(mPolylinePoints);
                }
            }
        } else {
            mPolylinePoints.add(myCoordinates);
            mPolyline.setPoints(mPolylinePoints);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myCoordinates, 16));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        }

    private void startStep1() {
        if(isGooglePlayServicesAvailable()) {
            startStep2(null);
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_google_playservice_available, Toast.LENGTH_LONG).show();
        }
    }

    private Boolean startStep2(DialogInterface dialog) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if(activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            promptInternetConnection();
            return false;
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();

        if(dialog != null) {
            dialog.dismiss();
        }

        if(checkPermissions()){
            startStep3();
        } else {
            requestPermissions();
        }

        return true;
    }

    private void promptInternetConnection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle(R.string.title_alert_no_internet);
        builder.setMessage(R.string.msg_alert_no_internet);

        String positiveText = getString(R.string.btn_label_refresh);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(startStep2(dialog)) {
                            if(checkPermissions()) {
                                startStep3();
                            } else if (!checkPermissions()) {
                                requestPermissions();
                            }
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startStep3() {
        if(!mAlreadyStartedService && mMsgView != null) {
            mMsgView.setText(R.string.msg_location_service_started);

            Intent intent = new Intent(this, LocationMonitoringService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            mAlreadyStartedService = true;
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }

            return false;
        }

        return true;
    }

    private boolean checkPermissions() {
        int permissionState1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionState2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if(shouldProvideRationale || shouldProvideRationale2) {
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(MapActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If img_user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startStep3();

            } else {
                // Permission denied.

                // Notify the img_user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the img_user for permission (device policy or "Never ask
                // again" prompts). Therefore, a img_user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    @Override
    public void onDestroy() {


        //Stop location sharing service to app server.........

        stopService(new Intent(this, LocationMonitoringService.class));
        mAlreadyStartedService = false;
        //Ends................................................

        /*Intent intent = new Intent(this, UploadPackageService.class);
        intent.putParcelableArrayListExtra("CORDS", (ArrayList<? extends Parcelable>) mPolylinePoints);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        mAlreadyStartedService = true;
*/
        super.onDestroy();
    }
}
