package com.example.hw2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;

    private final int MAX_NUM_SAMPLES = 1000000;
    private boolean measureGyro;
    private double[] gyroSum;
    private double[] gyroSum2;
    private int gyroCounter;
    private boolean measureAccel;
    private double[] accelSum;
    private double[] accelSum2;
    private int accelCounter;

    private double[] gyro = new double[3];
    private double[] accel = new double[3];

    private double[] GYRO_BIAS = new double[]{0.0, 0.0, 0.0};
    private double[] GYRO_NOISE = new double[]{0.0, 0.0, 0.0};
    private double[] ACCEL_BIAS = new double[]{0.0, 0.0, 0.0};
    private double[] ACCEL_NOISE = new double[]{0.0, 0.0, 0.0};

    private boolean plottingAccelTilt;
    private ArrayList<Float> accelTilt;
    private boolean plottingGyroTilt;
    private ArrayList<Float> gyroTilt;
    private boolean plottingCompTilt;
    private ArrayList<Float> compTilt;


    private boolean continuousMeasurementComp;
    private double[] currTilt = new double[]{0.0, 0.0, 0.0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(accelListener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(accelListener);
    }

    // Units in radians/s
    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            for (int i = 0; i < 3; i++) {
                gyro[i] = event.values[i];
            }
            if (measureGyro) mGyro();
            if (plottingGyroTilt) ;
            if (plottingCompTilt) ;
            if (continuousMeasurementComp) ;
        }
    };

    public void mGyro() {
        if (measureAccel) {
            Log.d("gyroListener", "Not measuring Gyro because measuring Accel");
            measureGyro = false;
            return;
        }
        if (gyroSum2 == null) {
            Log.d("gyroListener", "Starting to measure Gyro bias and noise");
            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_measure_gyro);
            gyroSum = new double[3];
            gyroSum2 = new double[3];
            gyroCounter = 0;
        }
        if (gyroCounter < MAX_NUM_SAMPLES) {
            for (int i = 0; i < 3; i++) {
                gyroSum[i] += gyro[i];
                gyroSum2[i] += gyro[i] * gyro[i];
            }
            gyroCounter += 1;
        }
        if (gyroCounter == MAX_NUM_SAMPLES) {
            GYRO_BIAS = new double[3];
            GYRO_NOISE = new double[3];
            for (int i = 0; i < 3; i++) {
                GYRO_BIAS[i] = gyroSum[i] / MAX_NUM_SAMPLES;
                GYRO_NOISE[i] = gyroSum2[i] / MAX_NUM_SAMPLES - GYRO_BIAS[i] * GYRO_BIAS[i];
            }
            // Set gyro bias and noise in document;
            Log.d("gyroListener", "Updating gyro bias and noise");
            Log.d("gyroListener", "bias " + Arrays.toString(GYRO_BIAS));
            Log.d("gyroListener", "noise " + Arrays.toString(GYRO_NOISE));
            TextView gyroBiasText = (TextView) findViewById(R.id.GyroBias);
            gyroBiasText.setText(Arrays.toString(GYRO_BIAS));
            TextView gyroNoiseText = (TextView) findViewById(R.id.GyroNoise);
            gyroNoiseText.setText(Arrays.toString(GYRO_NOISE));
            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_none);
            gyroSum = null;
            gyroSum2 = null;
        }
    }

    // Units in m/s^2
    public SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            for (int i = 0; i < 3; i++) {
                accel[i] = event.values[i];
            }
            if (measureAccel) mAccel();
            if (plottingAccelTilt) mAccelTilt();
            if (plottingCompTilt) mCompTilt();
            if (continuousMeasurementComp) mCompTilt();
        }
    };

    public void mAccel() {
        if (measureGyro) {
            Log.d("accelListener", "Not measuring Accel because measuring Gyro");
            measureAccel = false;
            return;
        }
        if (accelSum2 == null) {
            Log.d("accelListener", "Starting to measure Accel bias and noise");
            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_measure_accel);
            accelSum = new double[3];
            accelSum2 = new double[3];
            accelCounter = 0;
        }
        if (accelCounter < MAX_NUM_SAMPLES) {
            for (int i = 0; i < 3; i++) {
                accelSum[i] += accel[i];
                accelSum2[i] += accel[i] * accel[i];
            }
            accelCounter += 1;
        }
        if (accelCounter == MAX_NUM_SAMPLES) {
            ACCEL_BIAS = new double[3];
            ACCEL_NOISE = new double[3];
            for (int i = 0; i < 3; i++) {
                ACCEL_BIAS[i] = accelSum[i] / MAX_NUM_SAMPLES;
                ACCEL_NOISE[i] = accelSum2[i] / MAX_NUM_SAMPLES - ACCEL_BIAS[i] * ACCEL_BIAS[i];
            }
            // Set accel bias and noise in document;
            Log.d("accelListener", "Updating accel bias and noise");
            Log.d("accelListener", "bias " + Arrays.toString(ACCEL_BIAS));
            Log.d("accelListener", "noise " + Arrays.toString(ACCEL_NOISE));
            TextView accelBiasText = (TextView) findViewById(R.id.AccelBias);
            accelBiasText.setText(Arrays.toString(ACCEL_BIAS));
            TextView accelNoiseText = (TextView) findViewById(R.id.AccelNoise);
            accelNoiseText.setText(Arrays.toString(ACCEL_NOISE));
            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_none);
            accelSum = null;
            accelSum2 = null;
        }
    }

    // Button for estimating bias and noise for accelerometer and gyroscope
    public void measureGyroBiasNoise(View view) {
        measureGyro = true;
    }

    public void measureAccelBiasNoise(View view) {
        measureAccel = true;
    }

    // Button for plotting tilt over the duration of 5 minutes (Accel only)
    public void plotTiltAccel(View view) {
        if (plottingGyroTilt) {
            Log.d("plotTiltAccel", "Not plotting Accel Tilt because currently plotting Gyro Tilt");
            return;
        }
        if (plottingCompTilt) {
            Log.d("plotTiltAccel", "Not plotting Accel Tilt because currently plotting Comp Tilt");
            return;
        }
        if (continuousMeasurementComp) {
            Log.d("plotTiltAccel", "Not plotting Accel Tilt because currently measuring CompTilt");
            return;
        }
        plottingAccelTilt = true;
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_accel_tilt);

    }

    // Button for plotting tilt over the duration of 5 minutes (Gryo only)
    public void plotTiltGyro(View view) {
        if (plottingAccelTilt) {
            Log.d("plotTiltGyro", "Not plotting Gyro Tilt because currently plotting Accel Tilt");
            return;
        }
        if (plottingCompTilt) {
            Log.d("plotTiltGyro", "Not plotting Gyro Tilt because currently plotting Comp Tilt");
            return;
        }
        if (continuousMeasurementComp) {
            Log.d("plotTiltGyro", "Not plotting Gyro Tilt because currently measuring CompTilt");
            return;
        }
        plottingGyroTilt = true;
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_gyro_tilt);
    }

    // Button for plotting tilt over the duration of 5 minutes (Accel+Gyro+Comp filter only)
    public void plotTiltComp(View view) {
        if (plottingGyroTilt) {
            Log.d("plotTiltComp", "Not plotting Comp Tilt because currently plotting Gyro Tilt");
            return;
        }
        if (plottingAccelTilt) {
            Log.d("plotTiltComp", "Not plotting Comp Tilt because currently plotting Accel Tilt");
            return;
        }
        if (continuousMeasurementComp) {
            Log.d("plotTiltComp", "Not plotting Comp Tilt because currently measuring CompTilt");
            return;
        }
        plottingCompTilt = true;
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_comp_tilt);
    }

    // Measure tilt
    public void measureTiltComp(View view) {
        if (plottingGyroTilt) {
            Log.d("measureTiltComp", "Not measuring Comp Tilt because currently plotting Gyro Tilt");
            return;
        }
        if (plottingAccelTilt) {
            Log.d("measureTiltComp", "Not measuring Comp Tilt because currently plotting Accel Tilt");
            return;
        }
        if (plottingCompTilt) {
            Log.d("measureTiltComp", "Not measuring Comp Tilt because currently measuring CompTilt");
            return;
        }
        continuousMeasurementComp = true;
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_getting_comp_tilt);
    }

}
