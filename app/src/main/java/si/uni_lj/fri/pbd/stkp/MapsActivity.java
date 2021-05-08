package si.uni_lj.fri.pbd.stkp;



import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationAvailability;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap map;
    private Marker marker;
    private SensorManager sensorManager;
    private Sensor accelometer;
    private Sensor magnetometer;
    private LatLng currLocation;
    private float currRotation = 0;
    private final int rotationBufferLength = 15;
    private float[] rotationBuffer = new float[rotationBufferLength];
    private int rotationBufferIx = 0;
    boolean moveToPosition = false;
    private static final int LOCATION_PERMISSION_REQUEST_NUM = 1;

    // Buttons
    ImageButton findLocationBtn;
    ImageButton lockMarkerBtn;
    ImageButton lockPerspectiveBtn;

    volatile boolean animatingCamera = false;
    boolean isCameraLocked = false;
    boolean isPerspectiveLocked = false;

    // Warnings
    TextView noLocationWarning;
    TextView noGPSWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(si.uni_lj.fri.pbd.stkp.R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(si.uni_lj.fri.pbd.stkp.R.id.map);
        mapFragment.getMapAsync(this);
        registerCompassSensors();

        // Set warnings
        noLocationWarning = findViewById(si.uni_lj.fri.pbd.stkp.R.id.no_location_warning);
        noGPSWarning = findViewById(si.uni_lj.fri.pbd.stkp.R.id.no_gps_warning);

        // Find location btn
        findLocationBtn = findViewById(si.uni_lj.fri.pbd.stkp.R.id.find_location_btn);
        findLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateButtonClick(v);
                findLocation();
            }
        });

        // Lock marker btn
        lockMarkerBtn = findViewById(si.uni_lj.fri.pbd.stkp.R.id.lock_marker_btn);
        lockMarkerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateButtonClick(v);
                if (!isCameraLocked) {
                    isCameraLocked = true;
                    // set hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
                } else {
                    isCameraLocked = false;
                    // unset hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));
                }

            }
        });

        // Lock perspective btn
        lockPerspectiveBtn = findViewById(si.uni_lj.fri.pbd.stkp.R.id.lock_perspective_btn);
        lockPerspectiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateButtonClick(v);
                if (currLocation == null) {
                    Toast.makeText(getApplicationContext(),"Lokacija ni znana",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isPerspectiveLocked) {
                    animatePerspective(currLocation, currRotation, map.getCameraPosition().zoom, 60, 1000);

                    isPerspectiveLocked = true;
                    // set hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
                } else {
                    animatePerspective(currLocation, 0, map.getCameraPosition().zoom, 0, 1000);

                    isPerspectiveLocked = false;
                    // unset hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));

                }

            }
        });


    }

    private void animatePerspective(LatLng location, float rotation, float zoom, float tilt, int miliseconds) {
        if (location != null) {
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(location)
                    .bearing(rotation)
                    .tilt(tilt)
                    .zoom(zoom)
                    .build();
            animatingCamera = true;
            map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace), miliseconds, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    animatingCamera = false;
                }
                @Override
                public void onCancel() {
                    animatingCamera = false;
                }
            });
        }
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

        // Override default marker behaviour, so google maps suggestions don't show under buttons
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 400, null);
                marker.showInfoWindow();
                return true;
            }
        });

        // Draw gpx files
        String[] fileNamesToDraw = getIntent().getStringArrayExtra("fileNamesToDraw");
        DrawingGpxPoints gpxDrawer = new DrawingGpxPoints(map, fileNamesToDraw, getApplicationContext());
        gpxDrawer.run();


        // Start  location tracking
        //
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If not request it
            String[] requestPermissionString = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, requestPermissionString, LOCATION_PERMISSION_REQUEST_NUM);
        } else {
            // Start  location tracking
            startLocationTracking();
        }

    }

    // ===================== Location tracking =====================
    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            // On the first call, set location to known
            if (currLocation == null) {
                noLocationWarning.setVisibility(View.INVISIBLE);
            }
            // Read location and save it into class variable
            double lat = locationResult.getLastLocation().getLatitude();
            double lng = locationResult.getLastLocation().getLongitude();
            currLocation = new LatLng(lat, lng);

            // Draw the marker on the map
            drawMarker();
            // if camera is locked, then move and zoom the camera
            if (isCameraLocked) {
                followCamera(lat, lng);
            }
        }

        // Display warning if there is no location service available
        @Override
        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
            if (!locationAvailability.isLocationAvailable()) {
                noGPSWarning.setVisibility(View.VISIBLE);
            } else {
                noGPSWarning.setVisibility(View.INVISIBLE);
            }
        }
    };

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
    // =====================/ Location tracking =====================

    /*
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantedResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_NUM) {
            if (grantedResults.length > 0 && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationTracking();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    } */

    // ===================== GPS arrow =====================
    private void drawMarker()  {
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
                    .position(currLocation)
                    .flat(true)
                    .icon(scaledGpsArrow));


        }
        // if already exists, just change its position
        marker.setPosition(currLocation);
        //marker.setRotation(bearing);
    }

    private void rotateMarker(float rotation) {
        if (marker != null) {
            marker.setRotation(rotation);

            if (isPerspectiveLocked && !animatingCamera) {
                //animatePerspective(currLocation, currRotation, map.getCameraPosition().zoom, 65, 100);

                CameraPosition currentPlace = new CameraPosition.Builder()
                        .target(new LatLng(currLocation.latitude, currLocation.longitude))
                        .bearing(rotation).tilt(60).zoom(map.getCameraPosition().zoom).build();
                map.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));


            }
        }

    }
    // =====================/ GPS arrow =====================

    // ===================== Buttons =====================
    private void findLocation() {
        if (marker != null && currLocation != null) {

            animatingCamera = true;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLocation,19.5f),new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    animatingCamera = false;
                }

                @Override
                public void onCancel() {
                    animatingCamera = false;

                }
            });

        } else {
            Toast.makeText(this,"Lokacija ni znana",Toast.LENGTH_SHORT).show();
        }
    }

    private void followCamera(double lat, double lng) {
        if (marker != null) {
            map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        }
    }
    // =====================/ Buttons =====================


    private void zoomCamera(double lat, double lng) {
        float zoom = map.getCameraPosition().zoom;
        Log.d("debug", "Zoom -> " + zoom);
        if (zoom < 15) {
            map.animateCamera(CameraUpdateFactory.zoomTo(19.5f));
        }
    }

    private void rotateMap(float rotation) {
        if(marker != null) {
            CameraPosition rotate = new CameraPosition.Builder().bearing(rotation).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(rotate));
        }
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
            currRotation = deg;
            //rotateMap(deg);
            //Log.d("debug", "(deg): " + deg);

        }
    }

    // =================== button click animation ===================
    private AlphaAnimation fadeIn = new AlphaAnimation(1F, 0.2F);
    private AlphaAnimation fadeOut = new AlphaAnimation(0.2f, 1F);

    private void simulateButtonClick(View view) {
        fadeIn.setDuration(200);
        fadeOut.setDuration(200);
        view.startAnimation(fadeIn);
        view.startAnimation(fadeOut);
    }
    // ===================/ button click animation ===================


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
