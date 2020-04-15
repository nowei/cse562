package com.example.cse562hw1;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends AppCompatActivity {

    private boolean enabled = false;
    private AudioTrack wave = null;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> detectionHandle = null;

    public void updateDetection() {
        final Runnable detector = new Runnable() {
            public void run() {
                TextView s = (TextView) findViewById(R.id.detection);
                if (s.getText().toString().equals("None")) {
                    s.setText(R.string.detection_push);
                } else {
                    s.setText(R.string.detection_none);
                }
            }
        };
        detectionHandle = scheduler.scheduleAtFixedRate(detector, -1, 10, MILLISECONDS);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wave = generateTone();
    }

    /** Called to turn on and off the sound */
    public void toggleSoundOnOff(View view) {
        TextView statusText = (TextView) findViewById(R.id.statusText);
        try {
            if (enabled) {
                enabled = false;
                wave.pause();
                wave.flush();
                Log.d("Toggling", "Turning off");
                statusText.setText(R.string.status_off);
                statusText.setTextColor(Color.RED);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                scheduler.schedule(new Runnable() {
                    public void run() {
                        try {
                            detectionHandle.cancel(true);
                        } catch (Exception e) {
                            Log.d("scheulderRunnable", e.toString());
                        }
                    }
                }, 1, SECONDS);
            } else {
                enabled = true;
                wave.play();
                statusText.setText(R.string.status_on);
                statusText.setTextColor(Color.GREEN);
                Log.d("Toggling", "Turning on");
                updateDetection();
            }
        }catch (Exception e) {
            Log.d("Toggling error", e.toString());
        }
    }

    private AudioTrack generateTone() {
        final int FREQUENCY = 18000;                     // in Hz
        final int DURATION = 1;                          // in s
        final float SAMPLE_RATE = 44100.0f;              // in Hz
        final int NUM_SAMPLES = DURATION * (int)SAMPLE_RATE;  // number of samples
        final double sample[] = new double[NUM_SAMPLES];
        final byte generatedSound[] = new byte[2 * NUM_SAMPLES];
        for (int i = 0; i < NUM_SAMPLES; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / FREQUENCY));
        }
        for (int i = 0; i < NUM_SAMPLES * 2; i += 2) {
            final short val = (short) (sample[i / 2] * 32767);
            generatedSound[i] = (byte) (val & 0x00ff);
            generatedSound[i + 1] = (byte) ((val & 0xff00) >>> 8);
        }
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate((int)SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(generatedSound.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.setLoopPoints(0, NUM_SAMPLES, -1);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        return audioTrack;
    }

}
