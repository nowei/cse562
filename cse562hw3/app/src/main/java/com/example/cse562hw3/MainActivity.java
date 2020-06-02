package com.example.cse562hw3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;

import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;

// https://medium.com/mobiwise-blog/unsatisfiedlinkerror-problem-on-some-android-devices-b77f2f83837d
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.cvtColor;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    // Have a static picture on the background of the screen (transmitter) and communicate with the
    // phone (receiver) by changing its alpha value versus time using BFSK modulation.
    // After successfully communicating, try to increase the data-rate by changing alpha more
    // frequently till your eyes can notice the changes. Do the test for different alpha
    // differences (delta alpha=0.1 and 0.5) and different distances (20cm,40cm,80cm,120cm).
    // Report data-rates and bit error rates vs alpha difference for different distances and
    // explain the results.

    // Requirements:
    // - Uses camera
    // - BFSK modulation
    CameraManager cameraManager;
    Integer cameraFacing;
    String cameraId;
    CameraDevice.StateCallback stateCallback;
    CameraDevice cameraDevice;

//    HandlerThread backgroundThread;
//    Handler backgroundHandler;
    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

    Size previewSize;
    TextureView textureView;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;
    private CaptureRequest captureRequest;

    ImageReader mImageReader;
    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    static{ System.loadLibrary("opencv_java"); }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private double currTime;
//    private ArrayList<Double> calibration = new ArrayList<>();
    private ArrayList<Double> preamble = new ArrayList<>();
    private ArrayList<Double> bitstream = new ArrayList<>();
    private double STOP_THRESHOLD;
    private final int STOP_FRAMES = 6;
    private final int CALIBRATION_NUM = 400;
    private boolean testing = false;
//    private boolean recalibrating;
    private boolean recalibratingPreamble;
    private boolean started;
    private boolean witnessed;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        Image image;
        @Override
        public void onImageAvailable(ImageReader reader) {
//            mBackgroundHandler.post(new ImageProcess(reader.acquireNextImage()));
            image = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null && (recalibratingPreamble || started || testing)) {
//                if (image != null && (recalibrating || recalibratingPreamble || started || testing)) {
//                if (image != null && (started || testing)) {
//                    byte[] nv21;
//                    ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
//                    ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
//                    ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
//
//                    int ySize = yBuffer.remaining();
//                    int uSize = uBuffer.remaining();
//                    int vSize = vBuffer.remaining();
//
//                    nv21 = new byte[ySize + uSize + vSize];
//
//                    //U and V are swapped
//                    yBuffer.get(nv21, 0, ySize);
//                    vBuffer.get(nv21, ySize, vSize);
//                    uBuffer.get(nv21, ySize + vSize, uSize);
                    byte[] nv21 = YUV_420_888toNV21(image);

                    Mat mRGB = getYUV2Mat(nv21);
//                    Scalar avgColor = Core.mean(mRGB);
                    Scalar totalColor = Core.sumElems(mRGB);
                    double colorIntensity = totalColor.val[0] + totalColor.val[1] + totalColor.val[2];
//                    double luminance = (0.2126*avgColor.val[0] + 0.7152*avgColor.val[1] + 0.0722*avgColor.val[2]);
//                    Log.d("luminance", "intensity: " + luminance);
//                    Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CV_8UC1);
//                    mYuv.put(0, 0, nv21);
//                    double[] mYuv = new double[]{nv21[0], nv21[1], nv21[2]};
//                    Log.d("mYuv", "Y = " + mYuv[0] + " U = " + mYuv[1] + " V = " + mYuv[2]);

//                    Mat mRGB = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CV_8UC1);
//                    mRGB.put(0, 0, nv21);

//                    Log.d("Size of frame", mRGB.size().toString());
//                    Log.d("Entries", Arrays.toString(mRGB.get(0, 0)));
//                    Log.d("Entries", Arrays.toString(mRGB.get((int) mRGB.size().width / 2, (int) mRGB.size().height / 2)));
                    if (recalibratingPreamble && preamble.size() < CALIBRATION_NUM) {
                        preamble.add(colorIntensity);
                        if (preamble.size() == CALIBRATION_NUM) {
                            recalibratingPreamble = false;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView statusText = (TextView) findViewById(R.id.status);
                                    statusText.setText(R.string.recalibrated_preamble);
                                }
                            });
                            for (int i = 0; i < CALIBRATION_NUM; i++) {
                                STOP_THRESHOLD = Math.max(STOP_THRESHOLD, preamble.get(i));
                            }
//                            STOP_THRESHOLD /= CALIBRATION_NUM;
                            STOP_THRESHOLD *= 10;
                            Log.d("onImageAvailable - preamble", "Done recalibrating, threshold = " + STOP_THRESHOLD);
                        }
                    }

//                    if (recalibrating && calibration.size() < CALIBRATION_NUM) {
//                        calibration.add(colorIntensity);
//                        if (calibration.size() == CALIBRATION_NUM) {
//                            recalibrating = false;
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    TextView statusText = (TextView) findViewById(R.id.status);
//                                    statusText.setText(R.string.recalibrated);
//                                }
//                            });
//                            Log.d("onImageAvailable - recalibration", "Done recalibrating");
//                            double minimum = Double.MAX_VALUE;
//                            double total = 0.0;
//                            for (int i = 0; i < CALIBRATION_NUM; i++) {
//                                minimum = Math.min(minimum, calibration.get(i));
//                                total += calibration.get(i);
//                            }
//                            double average = total / CALIBRATION_NUM;
//                            Log.d("onImageAvailable - recalibration", "average: " + average + ", minimum: " + minimum);
//                        }
                    if (started) {
//                    if (started) {
                        if (bitstream.isEmpty()) {
                            Log.d("onImageAvailable - streaming", "Starting");
                        }
                        if (colorIntensity > STOP_THRESHOLD) {
                            Log.d("onImageAvailable - streaming", "Witnessed a color " + colorIntensity);
                            witnessed = true;
                        }

                        bitstream.add(colorIntensity);
                        if (witnessed && bitstream.size() > STOP_FRAMES) {
                            boolean stop = true;
                            for (int i = bitstream.size() - STOP_FRAMES; i < bitstream.size(); i++) {
                                if (bitstream.get(i) > STOP_THRESHOLD) {
                                    stop = false;
                                    break;
                                }
                            }
                            if (stop) {
                                started = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView statusText = (TextView) findViewById(R.id.status);
                                        statusText.setText(R.string.done);
                                    }
                                });
                                Log.d("onImageAvailable - streaming", "Done parsing stream");
                                analyzeStream();
                            }
                        }

                    }
//                    if (testing || recalibrating || recalibratingPreamble || started) {
////                    if (testing || started) {
//                        if (testing)
//                            Log.d("onImageAvailable - testing", 1.0 / (System.nanoTime() / 10e9 - currTime) + " color intensity " + colorIntensity + " size " + mRGB.size().toString());
//                        else if (recalibrating)
//                            Log.d("onImageAvailable - recalibrating", 1.0 / (System.nanoTime() / 10e9 - currTime) + " color intensity " + colorIntensity + " size " + mRGB.size().toString());
//                        else if (recalibratingPreamble)
//                            Log.d("onImageAvailable - recalibrating preamble", 1.0 / (System.nanoTime() / 10e9 - currTime) + " color intensity " + colorIntensity + " size " + mRGB.size().toString());
//                        else if (started)
//                            Log.d("onImageAvailable - started streaming", 1.0 / (System.nanoTime() / 10e9 - currTime) + " color intensity " + colorIntensity + " size " + mRGB.size().toString());
//                    }
//                    currTime = System.nanoTime()/10e9;
                }
            } catch (Exception e) {
                Log.w("Dying in onImageAvailable", e.getMessage());
            } finally {
                if (image != null) image.close();// don't forget to close
            }
        }

        Mat getYUV2Mat(byte[] data) {
            Mat mYuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CV_8UC1);
            mYuv.put(0, 0, data);
            Mat mRGB = new Mat();
            cvtColor(mYuv, mRGB, Imgproc.COLOR_YUV2RGB_NV21, 3);
            return mRGB;
        }
    };


    // https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776
    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }
        else {
            long yBufferPos = width - rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride - width;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert(rowStride == image.getPlanes()[1].getRowStride());
        assert(pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.get(nv21, ySize, uvSize);

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request camera permissions
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);

        textureView = findViewById(R.id.texture_view);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        currTime = System.nanoTime()/10e9;

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                MainActivity.this.cameraDevice = camera;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                MainActivity.this.cameraDevice = null;
            }
        };
    }

//    public void recalibrateButtonHandler(View view) {
//        calibration.clear();
//        TextView statusText = (TextView) findViewById(R.id.status);
//        statusText.setText(R.string.recalibrating);
//        Log.d("recalibrationHandler", "Starting to recalibrate");
////        recalibrating = true;
//    }

    public void recalibratePreambleButtonHandler(View view) {
        preamble.clear();
        STOP_THRESHOLD = 0.0;
        TextView statusText = (TextView) findViewById(R.id.status);
        statusText.setText(R.string.recalibrate_preamble);
        Log.d("recalibratePreambleButtonHandler", "Starting to recalibrate");
        recalibratingPreamble = true;
    }

    public void restartButtonHandler(View view) {
        TextView statusText = (TextView) findViewById(R.id.status);
//        if (calibration.isEmpty()) {
//            Log.d("restartHandler", "Rejected because not calibrated");
//            statusText.setText(R.string.needs_recalibration);
//        } else {
            if (started) {
                started = false;
                Log.d("restartHandler", "Canceling start");
                statusText.setText(R.string.none);
                if (!bitstream.isEmpty()) {
                    analyzeStream();
                }
            } else {
                bitstream.clear();
                statusText.setText(R.string.started);
                Log.d("restartHandler", "Starting to parse stream");
                started = true;
                witnessed = false;
            }
//        }
    }

    private void analyzeStream() {
        Log.d("analyzeStream", bitstream.toString());
        Log.d("analyzeStream", "Size of bitstream " + bitstream.size());
        Log.d("analyzeStream", "Filtering out stops");
        ArrayList<Double> filteredStream = new ArrayList<>(bitstream);
        double total = 0.0;
        for (int i = filteredStream.size() - 1; i >= 0; i--) {
            if (filteredStream.get(i) < STOP_THRESHOLD) {
                Log.d("analyzeStream - end", "Removing " + i);
                filteredStream.remove(i);
            } else {
                Log.d("analyzeStream - end", "breaking " + filteredStream.get(i) + " vs. " + STOP_THRESHOLD);
                break;
            }
        }
        for (int i = 0; i < filteredStream.size(); i++) {
            if (filteredStream.get(i) < STOP_THRESHOLD) {
                Log.d("analyzeStream - beginning", "Removing " + i);
                filteredStream.remove(i);
                i--;
            } else {
                Log.d("analyzeStream - beginning", "breaking " + filteredStream.get(i) + " vs. " + STOP_THRESHOLD);
                break;
            }
        }

        // Average of readings
        for (int i = 0; i < filteredStream.size(); i++) {
            total += filteredStream.get(i);
        }

        double average2 = total / filteredStream.size();

        // Average of calibration
//        double minimum = Double.MAX_VALUE;
//        for (int i = 0; i < calibration.size(); i++) {
//            total += calibration.get(i);
//            minimum = Math.min(minimum, calibration.get(i));
//        }
//        double average = total / calibration.size();

//        Log.d("analyzeStream - average", "Average " + average + ", minimum " + minimum);

//        ArrayList<Integer> onOffMapping = new ArrayList<>();
        ArrayList<Double> alteredMapping = new ArrayList<>();

        for (int i = 0; i < filteredStream.size(); i++) {
//            if (filteredStream.get(i) < average * 0.9) {
//            if (filteredStream.get(i) < minimum) {
            alteredMapping.add(filteredStream.get(i) - average2);
//            if (filteredStream.get(i) < average) {
//                onOffMapping.add(-1);
//            } else {
//                onOffMapping.add(1);
//            }
        }



//        Log.d("analyzeStream", "mapping " + onOffMapping.toString());

        Log.d("analyzeStream", "Getting fft");

//        // Repeat the sequence of 12 so we have more buckets for ffts
        int numEntries = 32;
        FFT fft = new FFT(numEntries);
        int stepSize = 6;
        Complex[] x = new Complex[numEntries];

//        ArrayList<Integer> maxIndices = new ArrayList<>();
//        int totalInds = 0;
//        for (int i = 0; i < onOffMapping.size(); i += stepSize) {
//            if (i + stepSize <= onOffMapping.size()) {
//                for (int j = 0; j < numEntries; j++) {
//                    x[j] = new Complex(onOffMapping.get(i + (j % stepSize)), 0.0);
//                }
//            } else {
//                int remainder = onOffMapping.size() - i;
//                if (remainder == 1) continue;
//                for (int j = 0; j < numEntries; j++) {
//                    x[j] = new Complex(onOffMapping.get(i + (j % remainder)), 0.0);
//                }
//            }
//            Complex[] res = fft.fft(x);
//            double[] ampl = new double[numEntries];
//            int maxIndex = 1;
//            double maxValue = 0.0;
//            Log.d("analyzeStream - FFT", "x = " + Arrays.toString(x));
//            Log.d("analyzeStream - FFT", "res = " + Arrays.toString(res));
//            for (int j = 0; j < res.length; j++) {
//                ampl[j] = res[j].abs();
//                if (j != 0) {
//                    if (ampl[j] > maxValue) {
//                        maxValue = ampl[j];
//                        maxIndex = j;
//                    }
//                }
//            }
//            maxIndices.add(maxIndex);
//            totalInds += maxIndex;
//        }


        ArrayList<Integer> maxIndices2 = new ArrayList<>();
        int totalInds2 = 0;
        for (int i = 0; i < alteredMapping.size(); i += stepSize) {
            if (i + stepSize <= alteredMapping.size()) {
                for (int j = 0; j < numEntries; j++) {
                    x[j] = new Complex(alteredMapping.get(i + (j % stepSize)), 0.0);
                }
            } else {
                int remainder = alteredMapping.size() - i;
                if (remainder == 1) continue;
                for (int j = 0; j < numEntries; j++) {
                    x[j] = new Complex(alteredMapping.get(i + (j % remainder)), 0.0);
                }
            }
            Complex[] res = fft.fft(x);
            double[] ampl = new double[numEntries];
            int maxIndex = 1;
            double maxValue = 0.0;
            Log.d("analyzeStream - FFT2", "x = " + Arrays.toString(x));
            Log.d("analyzeStream - FFT2", "res = " + Arrays.toString(res));
            for (int j = 0; j < res.length; j++) {
                ampl[j] = res[j].abs();
                if (j != 0) {
                    if (ampl[j] > maxValue) {
                        maxValue = ampl[j];
                        maxIndex = j;
                    }
                }
            }
            maxIndices2.add(maxIndex);
            totalInds2 += maxIndex;
        }


//        Log.d("analyzeStream", maxIndices.toString());
//        float averageInd = ((float) totalInds) / maxIndices.size();
//        Log.d("analyzeStream", "Average index " + averageInd);
//        final StringBuilder classifications = new StringBuilder(maxIndices.size());
//        for (int i = 0; i < maxIndices.size(); i++) {
//            if (maxIndices.get(i) > averageInd) {
//                classifications.append(1);
//            } else {
//                classifications.append(0);
//            }
//        }

        Log.d("analyzeStream2", maxIndices2.toString());
        float averageInd2 = ((float) totalInds2) / maxIndices2.size();
        Log.d("analyzeStream2", "Average index " + averageInd2);
        final StringBuilder classifications2 = new StringBuilder(maxIndices2.size());
        for (int i = 0; i < maxIndices2.size(); i++) {
            if (maxIndices2.get(i) > averageInd2) {
                classifications2.append(1);
            } else {
                classifications2.append(0);
            }
        }


//        final String s = classifications.toString();
//        Log.d("analyzeStream", "Final result " + s);
        final String s2 = classifications2.toString();
        Log.d("analyzeStream2", "Final result " + s2);

//        final StringBuilder result = new StringBuilder();
//        for (int i = 0; i + 8 <= s.length(); i += 8) {
//            int charCode = Integer.parseInt(s.substring(i, i + 8), 2);
//            result.append(Character.valueOf((char)charCode));
//        }

        final StringBuilder result2 = new StringBuilder();
        for (int i = 0; i + 8 <= s2.length(); i += 8) {
            int charCode = Integer.parseInt(s2.substring(i, i + 8), 2);
            result2.append(Character.valueOf((char)charCode));
        }


//        Log.d("analyzeStream", "Decoded string " + result.toString());
        Log.d("analyzeStream", "Decoded string " + result2.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView bitsText = (TextView) findViewById(R.id.received_bits);
                bitsText.setText(s2);
//                bitsText.setText(s + "\n" + sMajority);
                TextView decodeText = (TextView) findViewById(R.id.decoded_message);
                decodeText.setText(result2.toString());
            }
        });

//        for (int i = (onOffMapping.size() / 6) * 6; i < onOffMapping.size(); i++) {
//
//        }
    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING).equals(cameraFacing)) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int height = displayMetrics.heightPixels;
                    int width = displayMetrics.widthPixels;

                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height);
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, mBackgroundHandler);
//                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
//        backgroundThread = new HandlerThread("camera_background_thread");
//        backgroundThread.start();
//        backgroundHandler = new Handler(backgroundThread.getLooper());
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
//        if (backgroundHandler != null) {
//            backgroundThread.quitSafely();
//            backgroundThread = null;
//            backgroundHandler = null;
//        }
        if (mBackgroundHandler != null) {
            mBackgroundThread.quitSafely();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
//            surfaceTexture.setDefaultBufferSize(previewSize.getWidth() / 16, previewSize.getHeight() / 16);
            Surface previewSurface = new Surface(surfaceTexture);
//            mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReader = ImageReader.newInstance(previewSize.getWidth() / 4, previewSize.getHeight() / 4, ImageFormat.YUV_420_888, 2);
//            mImageReader = ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            Range<Integer>[] fps = new Range[1];
            fps[0] = Range.create(30, 30);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps[0]);

//            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
//            cameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
            cameraDevice.createCaptureSession(new ArrayList<Surface>(Arrays.asList(previewSurface, mImageReader.getSurface())),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();

                                MainActivity.this.cameraCaptureSession = cameraCaptureSession;

//                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
//                                            null, backgroundHandler);
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                            null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }
}
