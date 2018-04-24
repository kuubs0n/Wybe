package com.icmdigerati.wybe;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapActivitybak extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private MarkerOptions mo;
    private Marker mMarker;
    private Polyline mPolyline;
    private List<LatLng> mPolylinePoints = new ArrayList<LatLng>();
    private boolean mIsTracking = false;

    final static int PERMISSION_ALL = 1;
    final static String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    @BindView(R.id.ivButton)
    ImageView mIvButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        ButterKnife.bind(this);
        mapFragment.getMapAsync(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL);
        } else {
            requestLocation();
        }
        if (!isLocationEnabled())
            showAlert(1);
    }

    @Override
    public void onLocationChanged(Location location) {
        UpdateLocation(location);
    }

    private void UpdateLocation(Location location) {
        LatLng myCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
        mPolylinePoints.add(myCoordinates);
        mPolyline.setPoints(mPolylinePoints);

        if (mPolylinePoints.size() == 1 && myCoordinates != null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(myCoordinates)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.smallmarker)));
        } else if (mPolylinePoints.size() > 1 && myCoordinates != null) {
            mMarker.setPosition(myCoordinates);
        }

        if (mPolylinePoints.size() == 1)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCoordinates, 16));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Location provider Enabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Location provider Disabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mPolyline = mMap.addPolyline(new PolylineOptions()
                .color(Color.BLUE)
                .width(5f));
    }

    private void requestLocation() {

        if (!mIsTracking)
            return;

        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL);
        }

        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(location != null)
                UpdateLocation(location);
        }
        if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);

    }

    private boolean isLocationEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isPermissionGranted() {
        if(Build.VERSION.SDK_INT >= 23)
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }

        return true;
    }

    private void showAlert(final int status) {
        String message, title, btnText;
        if(status == 1) {
            message = "Your Locations Settings is set to 'OFF'.\nPlease Enable Location to " +
                    "use this app";
            title = "Enable Location";
            btnText = "Location settings";
        } else {
            message = "Please allow this app to access location!";
            title = "Permission access";
            btnText = "Grant";
        }

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton(btnText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (status == 1) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        } else {
                            if(Build.VERSION.SDK_INT >= 23)
                                requestPermissions(PERMISSIONS, PERMISSION_ALL);
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dialog.show();
    }

    @OnClick(R.id.ivButton)
    public void onClick(ImageView iv) {
        if(mIsTracking) {
            mIsTracking = false;
            iv.setImageDrawable(getResources().getDrawable(R.drawable.turnon));
            mLocationManager.removeUpdates(MapActivitybak.this);
        } else {
            mIsTracking = true;
            iv.setImageDrawable(getResources().getDrawable(R.drawable.turnoff));
            requestLocation();
        }
    }
}
