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
    private int FREQUENCY = 18000; // In Hz
    private int fsmState = 0;
    final Runnable detector = new Runnable() { public void run() { detection(); } };
    final int BUFFER_SIZE = 8192;
    final AudioRecord recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
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
            int length = buffer.length;
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
//        ByteBuffer.wrap(bytesBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);

        // Convert into FFT inputs
        double max = Math.pow(2, 16 - 1);
        Complex[] x = new Complex[buffer.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = new Complex((double)buffer[i] / max, 0.0);
        }

        // Perform FFT
        Complex[] res = fft.fft(x);
        double[] ampl = new double[buffer.length];
        for (int i = 0; i < x.length; i++) {
            ampl[i] = res[i].abs();
//            ampl[i] = res[i].r;
        }

        // Extract expected peak
        int offset = 15;
        int focusedFreq = (int)(((double)FREQUENCY / (44100.0 / 2.0)) * (buffer.length / 2.0)) + 1;
        int observer = focusedFreq - offset;
        double[] target = Arrays.copyOfRange(ampl, observer, observer + offset * 2);
//        try {
            Log.d("FFTres", (ampl[focusedFreq - 3] + " " + ampl[focusedFreq - 2] + " " + ampl[focusedFreq - 1] + " " + ampl[focusedFreq] + " " + ampl[focusedFreq + 1]) + " " + ampl[focusedFreq + 2] + ampl[focusedFreq + 3]);
//            Log.d("FFTres", (ampl[focusedFreq - 1] + " " + ampl[focusedFreq] + " " + ampl[focusedFreq + 1]));
//            Log.d("FFTres", (Arrays.toString(target)));
//        }catch (Exception e) {
//            Log.d("FFTres", e.toString());
//        }
//        testAudio(buffer);

        // Update FSM to determine if push or pull
        int[] rel_drops = findRelativeDrops(ampl, focusedFreq);
        int fsmUpdate = rel_drops[0];
        double bucketFreq = (focusedFreq + rel_drops[1] - 1) / (buffer.length / 2.0) * (44100.0 / 2.0);
//        findRelativeDrops(ampl, focusedFreq);
//        int fsmUpdate = 0;
        int fsmBefore = fsmState;
        if (fsmUpdate != 0) {
            fsmState += fsmUpdate * 2;
        } else {
            if (fsmState > 0) {
                fsmState -= 1;
            } else if (fsmState < 0) {
                fsmState += 1;
            }
//        } else if (fsmUpdate < 0) {
//            if (fsmState > 0) {
//                fsmState += 1;
//            } else {
//                if (fsmState <= 1)
//                    fsmState += fsmUpdate;
//            }
//        } else {
//            if (fsmState < 0) {
//                fsmState += 1;
//            } else if (fsmState > 0) {
//                if (fsmState >= -1)
//                    fsmState -= 1;
//            }
        }

        TextView s = (TextView) findViewById(R.id.detection);
        TextView f = (TextView) findViewById(R.id.freq_string);
        if (fsmState >= 1) {
            s.setText(R.string.detection_push);
            f.setText(String.format("%f Hz", bucketFreq));
        } else if (fsmState <= -1) {
            s.setText(R.string.detection_pull);
            f.setText(String.format("%f Hz", bucketFreq));
        } else if (-1 < fsmState && fsmState < 1) {
            s.setText(R.string.detection_none);
            f.setText(String.format("%f Hz", bucketFreq));
        }

        Log.d("FSMUpdate", "Before: " + fsmBefore + ", After: " + fsmState + ", Update: " + fsmUpdate);

        if (enabled) {
            detectionHandle = scheduler.schedule(detector, -1, MILLISECONDS);
        } else {
            f.setText(R.string.freq_none);
        }
    }

    private int[] findRelativeDrops(double[] ampl, int peakInd) {
        double peakAmp = ampl[peakInd];

        int window = 10;

        // Look for 0.1 of peakAmp
        int candLeft = peakInd - 1;
        int candRight = peakInd + 1;
        double firstThresh = 0.2 * peakAmp;
        int leftWindow = peakInd - window;
        int rightWindow = peakInd + window;
        while (candLeft > leftWindow && ampl[candLeft] > firstThresh) {
            candLeft--;
        }
        while (candRight < rightWindow && ampl[candRight] > firstThresh) {
            candRight++;
        }
//        Log.d("StartingCands", (peakInd - candLeft) + " " + (candRight - peakInd));
        int extendedLeft = candLeft - 1;
        int extendedRight = candRight + 1;

        // Look for extended peak of wave
        double secondThresh = 0.4 * peakAmp;
        while (extendedLeft > leftWindow && ampl[extendedLeft] < secondThresh) {
            extendedLeft--;
        }
        while (extendedRight < rightWindow && ampl[extendedRight] < secondThresh) {
            extendedRight++;
        }

        // If we found thresholds that are beyond the window we care about
        if (ampl[extendedLeft] > secondThresh && extendedLeft > leftWindow) {
            double thirdThresh = ampl[extendedLeft] * 0.1;
            int extendedCandLeft = extendedLeft - 1;
            while (extendedCandLeft > leftWindow && ampl[extendedCandLeft] > thirdThresh) {
                extendedCandLeft--;
            }
            candLeft = extendedCandLeft;
        }
        if (ampl[extendedRight] > secondThresh && extendedRight < rightWindow) {
            double thirdThresh = ampl[extendedRight] * 0.1;
            int extendedCandRight = extendedRight + 1;
            while (extendedCandRight < rightWindow && ampl[extendedCandRight] > thirdThresh) {
                extendedCandRight++;
            }
            candRight = extendedCandRight;
        }
        int update = 0;
        candLeft = peakInd - candLeft;
        candRight = candRight - peakInd;
        int diff = candRight - candLeft;
        if (candLeft >= 4) update -= 1;
        if (candRight >= 3) update += 1;
        Log.d("Candidates", "Left: " + candLeft + ", Right: " + candRight + " diff: " + (candRight - candLeft) + " update: " + update);
        if (update == 0) {
            return new int[]{update, 0};
        } else if (update == 1) {
            return new int[]{update, candRight};
        } else {
            return new int[]{update, -candLeft};
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
                if (!enabled) {
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
                TextView f = (TextView) findViewById(R.id.freq_string);
                f.setText(R.string.freq_none);
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
        final double[] sample = new double[NUM_SAMPLES];
        final byte[] generatedSound = new byte[2 * NUM_SAMPLES];
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
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
