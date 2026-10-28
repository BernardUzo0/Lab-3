
package com.example.donttouchmyphone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MotionDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_motion_details);

        TextView textStatus = findViewById(R.id.textMotionStatus);
        TextView textTime = findViewById(R.id.textMotionTime);
        TextView textStrength = findViewById(R.id.textMotionStrength);

        // Gets saved movement details
        SharedPreferences prefs = getSharedPreferences(
                "motion_prefs",
                MODE_PRIVATE
        );

        long motionTime = prefs.getLong(
                "last_motion_time_ms",
                -1
        );

        float motionChange = prefs.getFloat(
                "last_motion_change",
                0
        );

        // Check if any movement has been recorded
        if (motionTime == -1) {

            textStatus.setText("No motion recorded yet");
            textTime.setText("No movement time available");
            textStrength.setText("No movement data available");

        } else {

            // Format movement date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "dd MMM yyyy, HH:mm:ss",
                    Locale.getDefault()
            );

            String formattedTime = dateFormat.format(
                    new Date(motionTime)
            );

            textStatus.setText("Motion Detected!");

            textTime.setText(formattedTime);

            textStrength.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.2f m/s²",
                            motionChange
                    )
            );
        }
    }
}
