package com.example.cse562hw1;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
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
    private int FREQUENCY = 500; // In Hz
    private int fsmState = 0;
    final Runnable detector = new Runnable() { public void run() { detection(); } };
    final int BUFFER_SIZE = 4096;
    final AudioRecord recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(44100)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build())
                    .setBufferSizeInBytes(BUFFER_SIZE)
                    .build();
    final FFT fft = new FFT(BUFFER_SIZE);


    public void updateDetection() {
        detectionHandle = scheduler.schedule(detector, -1, MILLISECONDS);
    }

    public void detection() {
//        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        long start = System.currentTimeMillis();
        final short[] buffer = new short[BUFFER_SIZE];

        try {
            recorder.startRecording();

            // Read until buffer is full
            int offset = 0;
            int length = BUFFER_SIZE;
            int read;
            while (length > 0) {
                read = recorder.read(buffer, offset, length);
                length -= read;
                offset += read;
            }
            recorder.stop();
        } catch (Exception e) {
            Log.d("In thread", e.toString());
        }

        double max = Math.pow(2, 16 - 1);
        double[] realPart = new double[buffer.length];
        for (int i = 0; i < realPart.length; i++) {
            realPart[i] = ((double) buffer[i] / max);
        }

        // Perform FFT
        double[] imagPart = new double[buffer.length];
        fft.fft(realPart, imagPart);

//        s.setText(("Recordings done: " + fsmState++));
//        835.918367
        int freq = FREQUENCY;
        int offset = 25;
        int observer = (int)(((double)freq / (44100.0 / 2.0)) * BUFFER_SIZE) - offset;
        try {
            Log.d("FFTres", (Arrays.toString(Arrays.copyOfRange(realPart, 0, observer + offset * 2))));
        }catch (Exception e) {
            Log.d("FFTres", e.toString());
        }
//        testAudio(buffer);
//        Log.d("FFTres","What the heck");
        TextView s = (TextView) findViewById(R.id.detection);
        s.setText(("Time taken " + (System.currentTimeMillis() - start)));
        if (enabled) {
            detectionHandle = scheduler.schedule(detector, -1, MILLISECONDS);
        }
    }

    private void testAudio(short[] buffer) {

        final byte[] generatedSound = new byte[2 * BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            final short val = buffer[i];
            generatedSound[i * 2] = (byte) (val & 0x00ff);
            generatedSound[i * 2 + 1] = (byte) ((val & 0xff00) >>> 8);
        }
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(generatedSound.length)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.setLoopPoints(0, BUFFER_SIZE, -1);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        wave.stop();
        audioTrack.play();
        while (true) {

            if (false) break;

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wave = generateTone();
        wave.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                if (enabled) {
                    wave.pause();
                    wave.flush();
                }
                Log.d("MarkerReached", "It's not turning off " + enabled);
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {

            }
        });
    }

    /** Called to turn on and off the sound */
    public void toggleSoundOnOff(View view) {
        TextView statusText = (TextView) findViewById(R.id.statusText);
        try {
            if (enabled) {
                enabled = false;
                Log.d("Toggling", "Turning off");
                statusText.setText(R.string.status_off);
                statusText.setTextColor(Color.RED);
//                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//                scheduler.schedule(new Runnable() { public void run() { detectionHandle.cancel(true); } }, 1, SECONDS);
                wave.pause();
                wave.flush();
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
                .setBufferSizeInBytes(generatedSound.length)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.setLoopPoints(0, NUM_SAMPLES, -1);
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        return audioTrack;
    }

    @Override
    protected void onStop() {
        super.onStop();
        wave.pause();
        wave.flush();
    }

}
