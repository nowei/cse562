package com.example.hw2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;
    private TriggerEventListener triggerGyroListener;
    private TriggerEventListener triggerAccelListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        triggerGyroListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                // Do work
            }
        };

        sensorManager.requestTriggerSensor(triggerGyroListener, gyroSensor);
    }

    // Button for estimating bias and noise for accelerometer and gyroscope


    // Button for plotting tilt over the duration of 5 minutes (Accel only)


    // Button for plotting tilt over the duration of 5 minutes (Gryo only)


    // Button for plotting tilt over the duration of 5 minutes (Accel+Gyro+Comp filter only)


    // Measure tilt


}
