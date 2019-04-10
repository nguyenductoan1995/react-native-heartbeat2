package com.reactlibrary.Camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.reactlibrary.Camera.EventEmiter.emitOnFinish;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static Context mContext = null;
    private static CameraPreviewCallback cameraPreviewCallback;

    private int mCameraType = CameraManager.TYPE_CAMERA_BACK;
    private int mPreviewWidth = 0, mPreviewHeight;
    private boolean mIsRunning = false;
    private static final AtomicBoolean processing = new AtomicBoolean(false);

    private static int averageIndex = 0;
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];

    private static int sampleCount = 0;
    private static float totalHeartRate = 0;
    private static int measuretTime = 10;

    public static enum TYPE {
        GREEN, RED
    };

    private static TYPE currentType = TYPE.GREEN;

    public static TYPE getCurrent() {
        return currentType;
    }

    private static int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private static final int[] beatsArray = new int[beatsArraySize];
    private static double beats = 0;
    private static long startTime = 0;
    private static long lastTime = 0;
    private static PowerManager.WakeLock wakeLock = null;

    public CameraPreview(Context context, int initType, CameraPreviewCallback callback) {
        super(context);
        mContext = context;
        cameraPreviewCallback = callback;
        mCameraType = initType;
        initCamera();

        mHolder = getHolder();
        mHolder.addCallback(this);

    }

    public void setMeasureTime(int _measureTime) {
        measuretTime = _measureTime;
    }

    @SuppressLint("InvalidWakeLockTag")
    private void initCamera() {
        mCamera = CameraManager.getInstance().getCameraInstance(mCameraType);

        if(mCamera != null) {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(previewCallback);
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();

            //cause the camera display orientation is 90 degree.
            mPreviewWidth = previewSize.height;
            mPreviewHeight = previewSize.width;

            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");

            cameraPreviewCallback.onReady();


        } else {
            mPreviewHeight = 1;
            mPreviewWidth = 1;
            cameraPreviewCallback.onErrorOrcured(EventEmiter.Errors.CAMERA_DEVICE_NOT_AVAILABLE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mPreviewWidth || 0 == mPreviewHeight) {
            setMeasuredDimension(width, height);
        } else {
            int rWidth;
            int rHeight;
            if (width < height * mPreviewWidth / mPreviewHeight) {
                rWidth = height * mPreviewWidth / mPreviewHeight;
                rHeight = height;
            } else {
                rWidth = width;
                rHeight = width * mPreviewHeight / mPreviewWidth;
            };
            setMeasuredDimension(rWidth, rHeight);
        }
    }

    public void startPreview() {
        if(mCamera == null) {
            initCamera();
        }

        if(mIsRunning) {
            return;
        }
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            processing.set(false);
            sampleCount = 0;
            totalHeartRate = 0;
            wakeLock.acquire();
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

            mIsRunning = true;
        } catch (Exception e) {
            Log.d("Camera Preview", "Error setting camera preview: " + e.getMessage());
            mIsRunning = false;
            cameraPreviewCallback.onErrorOrcured(EventEmiter.Errors.CAMERA_PREVIEW_SETTINGS_FAILURE);
        }
    }

    public void stopCamera() {
        if (mCamera != null) {
            processing.set(false);
            sampleCount = 0;
            totalHeartRate = 0;
            wakeLock.release();
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
            mIsRunning = false;
            cameraPreviewCallback.onStop();
        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

    }

    public double getRatio() {
        if(mPreviewWidth == 0) {
            throw new RuntimeException("PreviewWidth cannot be zero");
        }

        return (float) mPreviewWidth / mPreviewHeight;
    }

    public void autoFocus() {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {

            }
        });
    }

        private static Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

            @Override
        public void onPreviewFrame(byte[] data, final Camera cam) {
            if(sampleCount > measuretTime) return;

            if (data == null) throw new NullPointerException();
            Camera.Parameters parameters = cam.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            cam.setParameters(parameters);
            if (size == null) throw new NullPointerException();

            if (!processing.compareAndSet(false, true)) return;


            int width = size.width;
            int height = size.height;

            int imgAvg = Utils.decodeYUV420SPtoRedAvg(data.clone(), height, width);
                if (imgAvg == 0 || imgAvg < 199) {
                cameraPreviewCallback.onErrorOrcured(EventEmiter.Errors.SKIN_DETECTION_FAILURE);
                processing.set(false);
                return;
            }

            int averageArrayAvg = 0;
            int averageArrayCnt = 0;
            for (int i = 0; i < averageArray.length; i++) {
                if (averageArray[i] > 0) {
                    averageArrayAvg += averageArray[i];
                    averageArrayCnt++;
                }
            }

            int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
            TYPE newType = currentType;
            if (imgAvg < rollingAverage) {
                newType = TYPE.RED;
                if (newType != currentType) {
                    beats++;
                }
            } else if (imgAvg > rollingAverage) {
                newType = TYPE.GREEN;
            }

            if (averageIndex == averageArraySize) averageIndex = 0;
            averageArray[averageIndex] = imgAvg;
            averageIndex++;

            // Transitioned from one state to another to the same
            if (newType != currentType) {
                currentType = newType;

            }


            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;

            if (totalTimeInSecs >= 1) {

                double bps = (beats / totalTimeInSecs);
                int dpm = (int) (bps * 60d);
                if (dpm < 30 || dpm > 180) {
                    startTime = endTime;//System.currentTimeMillis();
                    beats = 0;
                    processing.set(false);

                    return;
                }

                if (beatsIndex == beatsArraySize) beatsIndex = 0;
                beatsArray[beatsIndex] = dpm;
                beatsIndex++;

                int beatsArrayAvg = 0;
                int beatsArrayCnt = 0;
                for (int i = 0; i < beatsArray.length; i++) {
                    if (beatsArray[i] > 0) {
                        beatsArrayAvg += beatsArray[i];
                        beatsArrayCnt++;
                    }
                }
                final float beatsAvg = (float)(beatsArrayAvg / beatsArrayCnt);


                if(sampleCount == measuretTime) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    cam.setParameters(parameters);
                    cam.stopPreview();

                    float heartRate = totalHeartRate / sampleCount;
                    cameraPreviewCallback.onFinish(heartRate);
                    cameraPreviewCallback.onStop();
                    return;
                }
                cameraPreviewCallback.onValueChanged(beatsAvg,sampleCount + 1);
                sampleCount++;
                beats = 0;
                totalHeartRate += beatsAvg;
                startTime = endTime;
            }

            processing.set(false);
        }
    };
}
