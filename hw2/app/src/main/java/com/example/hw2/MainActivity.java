package com.example.hw2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;

    private double[] gyro = new double[3];
    private double[] accel = new double[3];

    private final int MAX_NUM_SAMPLES = 1000;
    private boolean measureGyro;
    private double[] gyroSum;
    private double[] gyroSum2;
    private int gyroCounter;
    private boolean measureAccel;
    private double[] accelSum;
    private double[] accelSum2;
    private int accelCounter;

    private double[] GYRO_BIAS = new double[]{2.274169921875E-5, 3.23486328125E-6, -1.08642578125E-6};
    private double[] GYRO_NOISE = new double[]{2.3132960960268976E-7, 1.7802209034562111E-7, 1.7246551126241682E-7};
    private double[] ACCEL_BIAS = new double[]{0.04230337829589844, -0.08600277709960938, 9.739888958740234};
    private double[] ACCEL_NOISE = new double[]{4.58502718248954E-4, 9.69409790625795E-5, 5.685786921105773E-5};

    private boolean plottingAccelTilt;
    private LinkedList<double[]> accelTilts;
    private double[] accelTilt;

    private Temporal prevMeasureTime;
    private double[] prevAngle;
    private boolean plottingGyroTilt;
    private LinkedList<double[]> gyroTilts;
    private double[] gyroTilt;

    private final double ALPHA = 0.99;
    private boolean plottingCompTilt;
    private LinkedList<double[]> compTilts;
    private double[] compTilt;
//    private final int RECORD_TIME = 10 * 1000; // in ms
    private final int RECORD_TIME = 5 * 60 * 1000; // in ms
    private final int COUNTDOWN_TIME = 30 * 1000;

    private boolean continuousMeasurementComp;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        String s = String.format("[%3.2e, %3.2e, %3.2e]", GYRO_BIAS[0], GYRO_BIAS[1], GYRO_BIAS[2]);
        TextView gyroBiasText = (TextView) findViewById(R.id.GyroBias);
        gyroBiasText.setText(s);

        s = String.format("[%3.2e, %3.2e, %3.2e]", GYRO_NOISE[0], GYRO_NOISE[1], GYRO_NOISE[2]);
        TextView gyroNoiseText = (TextView) findViewById(R.id.GyroNoise);
        gyroNoiseText.setText(s);

        s = String.format("[%3.2e, %3.2e, %3.2e]", ACCEL_BIAS[0], ACCEL_BIAS[1], ACCEL_BIAS[2]);
        TextView accelBiasText = (TextView) findViewById(R.id.AccelBias);
        accelBiasText.setText(s);

        s = String.format("[%3.2e, %3.2e, %3.2e]", ACCEL_NOISE[0], ACCEL_NOISE[1], ACCEL_NOISE[2]);
        TextView accelNoiseText = (TextView) findViewById(R.id.AccelNoise);
        accelNoiseText.setText(s);

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
            double[] copy = new double[3];
            for (int i = 0; i < 3; i++) {
                copy[i] = event.values[i];
            }
            gyro = copy;
            Temporal prev = prevMeasureTime;
            prevMeasureTime = Instant.now();
            if (measureGyro) mGyro();
            if (plottingGyroTilt | plottingCompTilt | continuousMeasurementComp) {
                mGyroTilt(ChronoUnit.MILLIS.between(prev, prevMeasureTime));
            }
            if (plottingCompTilt | continuousMeasurementComp) {
                mCompTilt();
            }
        }
    };

    @SuppressLint("DefaultLocale")
    public void mGyro() {
        if (measureAccel) {
            Log.d("mGyro", "Not measuring Gyro because measuring Accel");
            measureGyro = false;
            return;
        }
        if (gyroSum2 == null) {
            Log.d("mGyro", "Starting to measure Gyro bias and noise");
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
            Log.d("mGyro", "count: " + gyroCounter + " " + Arrays.toString(gyro));
        }
        if (gyroCounter == MAX_NUM_SAMPLES) {
            GYRO_BIAS = new double[3];
            GYRO_NOISE = new double[3];
            for (int i = 0; i < 3; i++) {
                GYRO_BIAS[i] = gyroSum[i] / MAX_NUM_SAMPLES;
                GYRO_NOISE[i] = gyroSum2[i] / MAX_NUM_SAMPLES - GYRO_BIAS[i] * GYRO_BIAS[i];
            }
            // Set gyro bias and noise in document;
            Log.d("mGyro", "Updating gyro bias and noise");
            Log.d("mGyro", "bias " + Arrays.toString(GYRO_BIAS));
            Log.d("mGyro", "noise " + Arrays.toString(GYRO_NOISE));

            String s = String.format("[%3.2e, %3.2e, %3.2e]", GYRO_BIAS[0], GYRO_BIAS[1], GYRO_BIAS[2]);
            TextView gyroBiasText = (TextView) findViewById(R.id.GyroBias);
            gyroBiasText.setText(s);

            s = String.format("[%3.2e, %3.2e, %3.2e]", GYRO_NOISE[0], GYRO_NOISE[1], GYRO_NOISE[2]);
            TextView gyroNoiseText = (TextView) findViewById(R.id.GyroNoise);
            gyroNoiseText.setText(s);
            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_none);
            gyroSum = null;
            gyroSum2 = null;
            measureGyro = false;
        }
    }

    public void mGyroTilt(long millis) {
        double[] copy = gyro;
        double dt = millis / 1000.0;
        double[] degs = new double[3];
        for (int i = 0; i < 3; i++) {
            degs[i] = Math.toDegrees((copy[i] - GYRO_BIAS[i])) * dt;
        }
//        double norm = Math.sqrt(rads[0] * rads[0] + rads[1] * rads[1] + rads[2] * rads[2]);
        for (int i = 0; i < 2; i++) {
            prevAngle[i] = prevAngle[i] + degs[i];
        }
        double pitch = prevAngle[0];
        double roll = prevAngle[1];
        gyroTilt = new double[]{(double)System.currentTimeMillis(), pitch, roll};
//        Log.d("mGyroTilt", Arrays.toString(gyroTilt));
        if (plottingGyroTilt) {
            gyroTilts.add(gyroTilt);
            TextView tilt = (TextView) findViewById(R.id.TiltText);
            @SuppressLint("DefaultLocale") String s = String.format(" gyro [%.2f, %.2f]", pitch, roll);
            tilt.setText(s);
        }
    }

    // Units in m/s^2
    public SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            double[] copy = new double[3];
            for (int i = 0; i < 3; i++) {
                copy[i] = event.values[i];
            }
            accel = copy;
            if (measureAccel) mAccel();
            if (plottingAccelTilt | plottingCompTilt | continuousMeasurementComp) mAccelTilt();
        }
    };

    @SuppressLint("DefaultLocale")
    public void mAccel() {
        if (measureGyro) {
            Log.d("mAccel", "Not measuring Accel because measuring Gyro");
            measureAccel = false;
            return;
        }
        if (accelSum2 == null) {
            Log.d("mAccel", "Starting to measure Accel bias and noise");
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
            Log.d("mAccel", "count: " + accelCounter + " " + Arrays.toString(accel));
        }
        if (accelCounter == MAX_NUM_SAMPLES) {
            ACCEL_BIAS = new double[3];
            ACCEL_NOISE = new double[3];
            for (int i = 0; i < 3; i++) {
                ACCEL_BIAS[i] = accelSum[i] / MAX_NUM_SAMPLES;
                ACCEL_NOISE[i] = accelSum2[i] / MAX_NUM_SAMPLES - ACCEL_BIAS[i] * ACCEL_BIAS[i];
            }
            // Set accel bias and noise in document;
            Log.d("mAccel", "Updating accel bias and noise");
            Log.d("mAccel", "bias " + Arrays.toString(ACCEL_BIAS));
            Log.d("mAccel", "noise " + Arrays.toString(ACCEL_NOISE));

            String s = String.format("[%3.2e, %3.2e, %3.2e]", ACCEL_BIAS[0], ACCEL_BIAS[1], ACCEL_BIAS[2]);
            TextView accelBiasText = (TextView) findViewById(R.id.AccelBias);
            accelBiasText.setText(s);

            s = String.format("[%3.2e, %3.2e, %3.2e]", ACCEL_NOISE[0], ACCEL_NOISE[1], ACCEL_NOISE[2]);
            TextView accelNoiseText = (TextView) findViewById(R.id.AccelNoise);
            accelNoiseText.setText(s);

            TextView status = (TextView) findViewById(R.id.StatusText);
            status.setText(R.string.status_none);
            accelSum = null;
            accelSum2 = null;
            measureAccel = false;
        }
    }

    public void mAccelTilt() {
        double[] copy = accel;

        double norm = Math.sqrt(copy[0] * copy[0] + copy[1] * copy[1] + copy[2] * copy[2]);

        double a_x = copy[0]/norm;
        double a_y = copy[1]/norm;
        double a_z = copy[2]/norm;

//        double pitch = Math.toDegrees(-Math.atan2(a_z, Math.copySign(1, a_y) * Math.sqrt(a_x * a_x + a_y * a_y))) + 90;
//        double roll = Math.toDegrees(-Math.atan2(-a_x, a_y)) - 180;
        double pitch = Math.toDegrees(-Math.atan2(a_y, Math.copySign(1, a_z) * Math.sqrt(a_x * a_x + a_z * a_z)));
        double roll = Math.toDegrees(-Math.atan2(-a_x, a_z));
        accelTilt = new double[]{System.currentTimeMillis(), pitch, roll};
        if (plottingAccelTilt) {
            accelTilts.add(accelTilt);
            TextView tilt = (TextView) findViewById(R.id.TiltText);
            @SuppressLint("DefaultLocale") String s = String.format(" accel [%.2f, %.2f]", pitch, roll);
            tilt.setText(s);
        }
    }

    public void mCompTilt() {
        double[] curr_gyro = gyroTilt;
        double[] curr_accel = accelTilt;
        double pitch = ALPHA * curr_gyro[1] + (1.0 - ALPHA) * curr_accel[1];
        double roll = ALPHA * curr_gyro[2] + (1.0 - ALPHA) * curr_accel[2];
        compTilt = new double[]{System.currentTimeMillis(), pitch, roll};
        if (plottingCompTilt) {
            compTilts.add(compTilt);
            TextView tilt = (TextView) findViewById(R.id.TiltText);
            @SuppressLint("DefaultLocale") String s = String.format(" comp [%.2f, %.2f]", pitch, roll);
            tilt.setText(s);
        }
        if (continuousMeasurementComp) {
            TextView tilt = (TextView) findViewById(R.id.TiltText);
            @SuppressLint("DefaultLocale") String s = String.format(" comp [%.2f, %.2f]", pitch, roll);
            tilt.setText(s);
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
        if (plottingAccelTilt) {
            Log.d("plotTiltAccel", "Not plotting Accel Tilt because already plotting Accel Tilt");
            return;
        }
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
        accelTilts = new LinkedList<>();
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_accel_tilt);
        // Set timer for 5 minutes to turn plottingAccelTilt off and save the file somewhere
        accelTilt = new double[]{0.0, 0.0, 0.0};

        Log.d("plotTiltAccel", "Starting Countdown timer");
//        CountDownTimer accelCDT = new CountDownTimer(5 * 60 * 1000, 60 * 1000) {
        CountDownTimer accelCDT = new CountDownTimer(RECORD_TIME, COUNTDOWN_TIME) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("accelCDT", "Time left: " + millisUntilFinished);
            }

            @Override
            public void onFinish() {
                String filename = "accelCDT.csv";
                Log.d("accelCDT", "finished");

                StringBuilder s = new StringBuilder();
                for (double[] vals : accelTilts) {
                    double nanos = vals[0];
                    double pitch = vals[1];
                    double roll = vals[2];
                    s.append(nanos).append(",").append(pitch).append(",").append(roll).append("\n");
                }

                Log.d("accelCDT", "Recording in: " + filename);
                writeToFile(s.toString(), filename);
                accelTilts = null;
                TextView status = (TextView) findViewById(R.id.StatusText);
                status.setText(R.string.status_none);
                plottingAccelTilt = false;
            }
        }.start();
    }

    private void writeToFile(String data, String filename) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {

            //If it isn't mounted - we can't write into it.
            return;
        }
//        Log.d("Trying to find where the heck I am", );
        File f = new File(this.getExternalFilesDir(null), filename);
//        File f = new File(this.getFilesDir().getAbsolutePath(), filename);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(f, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    // Button for plotting tilt over the duration of 5 minutes (Gryo only)
    public void plotTiltGyro(View view) {
        if (plottingAccelTilt) {
            Log.d("plotTiltGyro", "Not plotting Gyro Tilt because currently plotting Accel Tilt");
            return;
        }
        if (plottingGyroTilt) {
            Log.d("plotTiltGyro", "Not plotting Gyro Tilt because already plotting Gyro Tilt");
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
        gyroTilts = new LinkedList<>();
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_gyro_tilt);
        // Set timer for 5 minutes to turn plottingGyroTilt off and save the file somewhere
        prevAngle = new double[]{0.0, 0.0};
        prevMeasureTime = Instant.now();
        gyroTilt = new double[]{0.0, 0.0};
        Log.d("plotTiltGyro", "Starting Countdown timer");
        CountDownTimer gyroCDT = new CountDownTimer(RECORD_TIME, COUNTDOWN_TIME) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("gyroCDT", "Time left: " + millisUntilFinished);
            }

            @Override
            public void onFinish() {
                String filename = "gyroCDT.csv";
                Log.d("gyroCDT", "finished");

                StringBuilder s = new StringBuilder();
                for (double[] vals : gyroTilts) {
                    double nanos = vals[0];
                    double pitch = vals[1];
                    double roll = vals[2];
                    s.append(nanos).append(",").append(pitch).append(",").append(roll).append("\n");
                }

                Log.d("gyroCDT", "Recording in: " + filename);
                writeToFile(s.toString(), filename);
                gyroTilts = null;
                TextView status = (TextView) findViewById(R.id.StatusText);
                status.setText(R.string.status_none);
                plottingGyroTilt = false;
            }
        }.start();
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
        if (plottingCompTilt) {
            Log.d("plotTiltComp", "Not plotting Comp Tilt because already plotting Comp Tilt");
            return;
        }
        if (continuousMeasurementComp) {
            Log.d("plotTiltComp", "Not plotting Comp Tilt because currently measuring CompTilt");
            return;
        }
        plottingCompTilt = true;
        compTilts = new LinkedList<>();
        TextView status = (TextView) findViewById(R.id.StatusText);
        status.setText(R.string.status_plot_comp_tilt);
        // Set timer for 5 minutes to turn plottingCompTilt off and save the file somewhere
        prevAngle = new double[]{0.0, 0.0};
        prevMeasureTime = Instant.now();
        compTilt = new double[]{0.0, 0.0};
        Log.d("plotTiltComp", "Starting Countdown timer");
        CountDownTimer compCDT = new CountDownTimer(RECORD_TIME, COUNTDOWN_TIME) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("compCDT", "Time left: " + millisUntilFinished);
            }

            @Override
            public void onFinish() {
                String filename = "compCDT.csv";
                Log.d("compCDT", "finished");

                StringBuilder s = new StringBuilder();
                for (double[] vals : compTilts) {
                    double nanos = vals[0];
                    double pitch = vals[1];
                    double roll = vals[2];
                    s.append(nanos).append(",").append(pitch).append(",").append(roll).append("\n");
                }

                Log.d("compCDT", "Recording in: " + filename);
                writeToFile(s.toString(), filename);
                compTilts = null;
                TextView status = (TextView) findViewById(R.id.StatusText);
                status.setText(R.string.status_none);
                plottingCompTilt = false;
            }
        }.start();
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
        TextView status = (TextView) findViewById(R.id.StatusText);
        if (continuousMeasurementComp) {
            continuousMeasurementComp = false;
            status.setText(R.string.status_none);
        } else {
            continuousMeasurementComp = true;
            status.setText(R.string.status_getting_comp_tilt);
            prevAngle = new double[]{0.0, 0.0};
            prevMeasureTime = Instant.now();
        }
    }

}
