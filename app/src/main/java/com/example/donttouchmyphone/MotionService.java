
package com.example.donttouchmyphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class MotionService extends Service implements SensorEventListener {
    private static final String TAG = "MotionService";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private HandlerThread sensorThread;

    private boolean sensorRegistered = false;
    private static final float MOVEMENT_THRESHOLD = 2.5f;
    private static final long ALERT_COOLDOWN_MS = 5000;

    private static final String ALERT_CHANNEL_ID = "motion_alert_channel";

    private float previousX;
    private float previousY;
    private float previousZ;

    private boolean firstReading = true;
    private long lastAlertTime = 0;


    private static final String CHANNEL_ID = "motion_monitor_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER
            );
        }

        sensorThread = new HandlerThread("AccelerometerThread");
        sensorThread.start();

        Log.d(TAG, "Accelerometer thread started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Active monitoring",
                NotificationManager.IMPORTANCE_LOW
        );

        manager.createNotificationChannel(channel);

// Creates a different notification channel for the movement alerts
        NotificationChannel alertChannel = new NotificationChannel(
                ALERT_CHANNEL_ID,
                "Movement Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );

        manager.createNotificationChannel(alertChannel);


        Intent homeIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                10,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Phone movement monitoring")
                .setContentText("Service started")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(1, notification);
        }

        // Checks if accelerometer is available
        if (sensorManager == null || accelerometer == null) {
            Log.e(TAG, "Accelerometer not available");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Register the sensor on separate thread
        if (!sensorRegistered) {

            sensorRegistered = sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                    new Handler(sensorThread.getLooper())
            );

            if (!sensorRegistered) {
                Log.e(TAG, "Failed to register accelerometer");
                stopSelf();
                return START_NOT_STICKY;
            }

            Log.d(TAG, "Accelerometer monitoring started");
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        Log.d(TAG, "Thread=" + Thread.currentThread().getName()
                + " X=" + x
                + " Y=" + y
                + " Z=" + z);

// Calculates any change in the acceleration
        if (!firstReading) {

            float dx = x - previousX;
            float dy = y - previousY;
            float dz = z - previousZ;

            float change = (float) Math.sqrt(
                    dx * dx + dy * dy + dz * dz
            );

            long currentTime = SystemClock.elapsedRealtime();

            if (change >= MOVEMENT_THRESHOLD &&
                    currentTime - lastAlertTime >= ALERT_COOLDOWN_MS) {

                lastAlertTime = currentTime;

                Log.d(TAG, "Motion detected! Change = " + change);

                sendMotionNotification(change);
            }

        } else {
            firstReading = false;
        }

// Saves the current readings for the next comparison
        previousX = x;
        previousY = y;
        previousZ = z;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action required
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMotionNotification(float change) {

        // Get the current date and time
        long currentTime = System.currentTimeMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMM yyyy, HH:mm:ss",
                Locale.getDefault()
        );

        String time = dateFormat.format(new Date(currentTime));

        // Saves movement details for the next activity
        getSharedPreferences("motion_prefs", MODE_PRIVATE)
                .edit()
                .putLong("last_motion_time_ms", currentTime)
                .putFloat("last_motion_change", change)
                .apply();

        Log.d(TAG, "Motion detected at " + time);

        // Checks for notification permission
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            Log.w(TAG, "Notification permission not granted");
            return;
        }

        // Opens the MainActivity when the notification is tapped
        Intent intent = new Intent(this, MotionDetailsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                20,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        // Creates a motion alert
        Notification notification = new Notification.Builder(
                this,
                ALERT_CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Motion Detected!")
                .setContentText("Phone moved at " + time)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        manager.notify(2, notification);
    }

    @Override
    public void onDestroy() {

        if (sensorManager != null && sensorRegistered) {
            sensorManager.unregisterListener(this);
            sensorRegistered = false;
        }

        if (sensorThread != null) {
            sensorThread.quitSafely();
        }

        Log.d(TAG, "Monitoring service stopped");

        super.onDestroy();
    }

}
