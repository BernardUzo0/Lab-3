package com.example.donttouchmyphone;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView textStatus = findViewById(R.id.textStatus);

// Start Monitoring button
        findViewById(R.id.buttonStart).setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );

            } else {
                startMonitoring();
            }
        });

// Stop Monitoring button
        findViewById(R.id.buttonStop).setOnClickListener(v -> {

            stopService(new Intent(this, MotionService.class));
            textStatus.setText(R.string.monitoring_off);

        });

// Details button
        findViewById(R.id.buttonDetails).setOnClickListener(v -> {

            Intent detailsIntent = new Intent(
                    this,
                    MotionDetailsActivity.class
            );

            startActivity(detailsIntent);

        });
    }
    private void startMonitoring() {

        Intent serviceIntent =
                new Intent(this, MotionService.class);

        startForegroundService(serviceIntent);

        TextView textStatus = findViewById(R.id.textStatus);
        textStatus.setText("Monitoring service started");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 101) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startMonitoring();

            } else {

                Toast.makeText(
                        this,
                        "Allow notifications to continue",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

}