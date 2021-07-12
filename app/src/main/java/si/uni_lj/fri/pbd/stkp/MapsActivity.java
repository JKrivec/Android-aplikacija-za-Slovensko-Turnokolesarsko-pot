package si.uni_lj.fri.pbd.stkp;



import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import si.uni_lj.fri.pbd.stkp.orientation.ImprovedOrientationSensor2Provider;
import si.uni_lj.fri.pbd.stkp.orientation.OrientationProvider;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Marker marker;
    private LatLng currLocation;
    private float currBearing;
    private float currRotation = 0;
    private static final int LOCATION_PERMISSION_REQUEST_NUM = 1;
    private OrientationProvider currentOrientationProvider;

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

    private boolean yetToAnimatePerspective = false;
    private int yetToAnimatePerspectiveSkips = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //registerCompassSensors();

        // Set warnings
        noLocationWarning = findViewById(R.id.no_location_warning);
        noGPSWarning = findViewById(R.id.no_gps_warning);

        // Find location btn
        findLocationBtn = findViewById(R.id.find_location_btn);
        findLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateButtonClick(v);
                findLocation();
            }
        });

        // Lock marker btn
        lockMarkerBtn = findViewById(R.id.lock_marker_btn);
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
                    currentOrientationProvider.start();
                    yetToAnimatePerspective = true;
                    // set button hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
                } else {
                    animatePerspective(currLocation, 0, map.getCameraPosition().zoom, 0, 1000);
                    currentOrientationProvider.stop();
                    isPerspectiveLocked = false;
                    setMarkerIcon(0);
                    marker.setRotation(0);
                    // unset button hue
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));
                }
            }
        });
        // Setup precise orientation provider for "perspective mode"
        //sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        currentOrientationProvider = new ImprovedOrientationSensor2Provider((SensorManager) this.getSystemService(SENSOR_SERVICE), this);
        if (isPerspectiveLocked) {
            currentOrientationProvider.start();
        }
    }

    @Override
    protected void onDestroy() {
        stopLocationTracking();
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        if (isPerspectiveLocked) {
            currentOrientationProvider.start();
        }
        super.onResume();
    }

    @Override
    protected  void onPause(){
        currentOrientationProvider.stop();
        super.onPause();
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
        //map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

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
        boolean drawInternal = getIntent().getBooleanExtra("drawInternal", false);
        DrawingGpxPoints gpxDrawer = new DrawingGpxPoints(map, fileNamesToDraw, drawInternal, getApplicationContext());
        gpxDrawer.draw();


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
            // add a marker to the map
            marker = map.addMarker(new MarkerOptions()
                    .position(currLocation)
                    .flat(true));
            // Set circe for the icon at first
            setMarkerIcon(0);
        }
        // if already exists, just change its position
        marker.setPosition(currLocation);
    }

    private void setMarkerIcon(int type) {
        // create smaller size icon from the gps png
        int height = 75;
        int width = 75;
        Bitmap bitmap;
        switch (type) {
            // use 1 when in "perspective" mode
            case 1:
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gps_arrow);
                break;

            default:
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.street_view_solid);
                break;
        }
        Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, width, height, false);
        BitmapDescriptor scaledGpsIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
        if (marker != null) {
            marker.setIcon(scaledGpsIcon);
        }
    }

    private void rotateMarker() {
        if (marker != null) {
            marker.setRotation(currRotation);
            if (isPerspectiveLocked && !animatingCamera) {
                CameraPosition currentPlace = new CameraPosition.Builder()
                        .target(new LatLng(currLocation.latitude, currLocation.longitude))
                        .bearing(currRotation).tilt(60).zoom(map.getCameraPosition().zoom).build();
                map.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
            }
        }

    }
    // =====================/ GPS arrow =====================

    // ===================== Buttons =====================
    private void findLocation() {
        if (marker != null && currLocation != null) {
            animatingCamera = true;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currLocation,18f),new GoogleMap.CancelableCallback() {
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

    private void animatePerspective(LatLng location, float rotation, float zoom, float tilt, int miliseconds) {
        Toast.makeText(this, "rotation: " + rotation, Toast.LENGTH_SHORT).show();
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
    // =====================/ Buttons =====================

    /**
     * Update from the orientation provider
     */
    public void update(){
        float[] angles = new float[3];
        currentOrientationProvider.getEulerAngles(angles);
        // TODO: remap axis when angles[0] reaches < -75 (phone held vertically)
        currRotation = (float) (angles[0] * 180.0f / Math.PI);
        rotateMarker();

        // Some spaghetti to make rotation into perspective mode feel right
        // The first few values from orientation provider after starting it are skewed
        if (yetToAnimatePerspective) {
            if (yetToAnimatePerspectiveSkips > 0) {
                yetToAnimatePerspectiveSkips--;
                return;
            }
            yetToAnimatePerspectiveSkips = 5;
            animatePerspective(currLocation, currRotation, map.getCameraPosition().zoom, 60, 1000);
            isPerspectiveLocked = true;
            setMarkerIcon(1);
            yetToAnimatePerspective = false;
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


}
