package si.uni_lj.fri.pbd.stkp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {

    static final int LOCATION_SERVICE_ID = 69;
    static final String START_LOCATION_SERVICE = "startLocationService";
    static final String STOP_LOCATION_SERVICE = "stop LocationService";

    private LocationCallback locationCallback = new LocationCallback(){

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            Double lat = locationResult.getLastLocation().getLatitude();
            Double lng = locationResult.getLastLocation().getLongitude();
            Log.d("LOCATION UPDATE", lat + ", " + lng);

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startLocationService() {
        String chanelId = "location_notification_chanel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // create intent to stop location service from the notification
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        intent.setAction(LocationService.STOP_LOCATION_SERVICE);
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), chanelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.addAction(R.drawable.gps_arrow, "stop", pStopSelf);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(chanelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(chanelId, "Location service", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest =  LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                Log.d("onStartCommand", action);
                if (action.equals(START_LOCATION_SERVICE)) {
                    startLocationService();
                } else if(action.equals(STOP_LOCATION_SERVICE)) {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


}
