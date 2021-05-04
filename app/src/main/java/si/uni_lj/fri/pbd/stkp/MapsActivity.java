package si.uni_lj.fri.pbd.stkp;



import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap map;
    private Marker marker;
    private SensorManager sensorManager;
    private Sensor accelometer;
    private Sensor magnetometer;
    private final int rotationBufferLength = 15;
    private float[] rotationBuffer = new float[rotationBufferLength];
    private int rotationBufferIx = 0;
    boolean isCameraLocked = false;
    boolean moveToPosition = false;
    private static final int LOCATION_PERMISSION_REQUEST_NUM = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(si.uni_lj.fri.pbd.stkp.R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(si.uni_lj.fri.pbd.stkp.R.id.map);
        mapFragment.getMapAsync(this);
        registerCompassSensors();
    }

    @Override
    protected void onDestroy() {
        stopLocationTracking();
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        registerCompassSensors();
        super.onResume();
    }

    @Override
    protected  void onPause(){
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    private void registerCompassSensors() {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, accelometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // display slovenia (geometric centre of slovenia as the middle)
        LatLng geoCenter = new LatLng(46.0, 14.7);
        map.moveCamera(CameraUpdateFactory.newLatLng(geoCenter));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(geoCenter, 8));

        String[] fileNamesToDraw = getIntent().getStringArrayExtra("fileNamesToDraw");
        DrawingGpxPoints gpxDrawer = new DrawingGpxPoints(map, fileNamesToDraw, getApplicationContext());
        gpxDrawer.run();



        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If not request it
            String[] requestPermissionString = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, requestPermissionString, LOCATION_PERMISSION_REQUEST_NUM);
        } else {
            startLocationTracking();
        }

    }


    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantedResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_NUM) {
            if (grantedResults.length > 0 && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationTracking();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void drawMarker(double lat, double lng, float bearing)  {
        // create new marker if null
        if (marker == null) {
            // create smaller size icon from the gps png
            int height = 75;
            int width = 75;
            Bitmap b = BitmapFactory.decodeResource(getResources(), si.uni_lj.fri.pbd.stkp.R.drawable.gps_arrow);
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            BitmapDescriptor scaledGpsArrow = BitmapDescriptorFactory.fromBitmap(smallMarker);
            // add a marker to the map
            marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .flat(true)
                    .icon(scaledGpsArrow));


        }
        // if already exists, just change its position
        marker.setPosition(new LatLng(lat, lng));
        //marker.setRotation(bearing);
    }

    private void moveCamera(double lat, double lng) {
        if (marker != null) {
            map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));

        }
    }

    private void zoomToPosition(LatLng position) {
        if (marker != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position,19.5f));
            moveToPosition = false;
        }
    }

    private void zoomCamera(double lat, double lng) {
        float zoom = map.getCameraPosition().zoom;
        Log.d("debug", "Zoom -> " + zoom);
        if (zoom < 15) {
            map.animateCamera(CameraUpdateFactory.zoomTo(19.5f));
        }
    }

    private void rotateMarker(float rotation) {
        if (marker != null) {
            marker.setRotation(rotation);
        }

    }

    private void rotateMap(float rotation) {
        if(marker != null) {
            CameraPosition rotate = new CameraPosition.Builder().bearing(rotation).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(rotate));
        }
    }


    // if camera is locked the camera follows current location
    public void toggleCameraLock(View v) {
        Log.d("debug", "toggleCameraLock()");
        if (isCameraLocked) {
            isCameraLocked = false  ;
        } else {
            isCameraLocked = true;
        }
    }

    public void findPosition(View v) {
        Log.d("debug", "findPosition()");
        this.moveToPosition = true;
    }


    private void startLocationTracking() {

        LocationRequest locationRequest =  LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(50);

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationTracking() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);

    }

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] R = new float[9];
    private float[] I = new float[9];

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            for (int i = 0; i < 3; i++){
                mGravity[i] = alpha * mGravity[i] + (1 - alpha) * event.values[i];
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            for (int i = 0; i < 3; i++){
                mGeomagnetic[i] = alpha * mGeomagnetic[i] + (1 - alpha) * event.values[i];
            }

        }

        boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                mGeomagnetic);
        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            // Log.d(TAG, "azimuth (rad): " + azimuth);
            float deg;
            deg = (float) Math.toDegrees(orientation[0]); // orientation
            deg = (deg + 360) % 360;
            rotateMarker(deg);
            //rotateMap(deg);
            //Log.d("debug", "(deg): " + deg);

        }
    }



    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            double lat = locationResult.getLastLocation().getLatitude();
            double lng = locationResult.getLastLocation().getLongitude();
            float bearing = locationResult.getLastLocation().getBearing();

            Log.d("debug", lat + ", " + lng);
            drawMarker(lat, lng, bearing);
            // if camera is locked, then move and zoom the camera
            if (isCameraLocked) {
                moveCamera(lat, lng);
                //zoomCamera(lat, lng);
            }

            if (moveToPosition) {
                zoomToPosition(new LatLng(lat, lng));
            }

        }
    };


    // =============================== OLD CODE (LOCATION SERVICE) ===============================
    /*private boolean isLocationServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            // Loop over all running services, and check if one matches the class name
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) return true;
                }
            }
            // not found
            return  false;
        }
        return false;
    }

    private void startLocationService() {
        if(!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(LocationService.START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(LocationService.STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }

     */
    // ===============================/ OLD CODE (LOCATION SERVICE) ===============================

}
