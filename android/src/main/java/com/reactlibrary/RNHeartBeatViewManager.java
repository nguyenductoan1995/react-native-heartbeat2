package com.reactlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.CAMERA;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class RNHeartBeatViewManager extends SimpleViewManager<RNHeartBeatView> implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = "RNHeartBeatViewManager";
    private static ThemedReactContext reactContext;
    private String[] neededPermissions = new String[]{CAMERA};
    private static Camera camera = null;
    private static int currentCamera = 0;
    private static String PERMISSION_REQUIRED = "Camera Permission";
    private static String PERMISSION_MESSAGE = "Please allow to access your camera";
    public static final int REQUEST_CODE = 100;
    private static String flashMode = Camera.Parameters.FLASH_MODE_AUTO;
    private static Stack<RNHeartBeatView> cameraViews = new Stack<>();
    private static AtomicBoolean cameraReleased = new AtomicBoolean(false);

    private static int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private static final int[] beatsArray = new int[beatsArraySize];
    private static double beats = 0;
    private static long startTime = 0;

    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private static int averageIndex = 0;
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];

    public static enum TYPE {
        GREEN, RED
    };

    private static TYPE currentType = TYPE.GREEN;

    public static TYPE getCurrent() {
        return currentType;
    }

    public static Camera getCamera() {
        return camera;
    }

    static void setCameraView(RNHeartBeatView cameraView) {
        if (!cameraViews.isEmpty() && cameraViews.peek() == cameraView) return;
        RNHeartBeatViewManager.cameraViews.push(cameraView);
        connectHolder();
    }

    private static void connectHolder() {
        if (cameraViews.isEmpty() || cameraViews.peek().getHolder() == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera == null) {
                    initCamera();
                }

                if (cameraViews.isEmpty()) {
                    return;
                }

                cameraViews.peek().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            camera.stopPreview();
                            camera.setPreviewDisplay(cameraViews.peek().getHolder());
                            camera.startPreview();
                            camera.setPreviewCallback(previewCallback);
                            Log.d("ssss", "connectHolder");

                        } catch (IOException | RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    static void removeCameraView() {
        if (!cameraViews.isEmpty()) {
            cameraViews.pop();
        }
        if (!cameraViews.isEmpty()) {
            connectHolder();
        } else if (camera != null) {
            releaseCamera();
            camera = null;
        }
        if (cameraViews.isEmpty()) {

        }
    }

    private static void initCamera() {
        if (camera != null) {
            releaseCamera();
        }
        try {
            camera = Camera.open(currentCamera);
            updateCameraSize();
            cameraReleased.set(false);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static void releaseCamera() {
        camera.setOneShotPreviewCallback(null);
        cameraReleased.set(true);
        camera.release();
    }

    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.15;
        double targetRatio = (double) h / w;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    private static void updateCameraSize() {
        try {
            Camera camera = RNHeartBeatViewManager.getCamera();

            WindowManager wm = (WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            size.y = Utils.convertDeviceHeightToSupportedAspectRatio(size.x, size.y);
            if (camera == null) return;
            List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
            List<Camera.Size> supportedPictureSizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(supportedPreviewSizes, size.x, size.y);
            Camera.Size optimalPictureSize = getOptimalPreviewSize(supportedPictureSizes, size.x, size.y);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
            parameters.setFlashMode(flashMode);
            camera.setParameters(parameters);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public String getName() {
        return "RNHeartBeatView";
    }

    @Override
    protected RNHeartBeatView createViewInstance(ThemedReactContext reactContext) {
        RNHeartBeatViewManager.reactContext = reactContext;
        return new RNHeartBeatView(reactContext);
    }

    @ReactProp(name = "enabled", defaultBoolean = false)
    public void setEnabled(RNHeartBeatView view, boolean enabled) {
        boolean granted = checkPermission();
        Log.d(getName(),"permission granted: " + granted);
        if (granted) {
            initCamera();
            view.setEnabled(enabled);
        }
    }

    @ReactProp(name = "framePerSecond", defaultInt = 30)
    public void setFramePerSecond(RNHeartBeatView view, @Nullable int framePerSecond) {
        view.setFramePerSecond(framePerSecond);
    }

    @ReactProp(name = "measureTime", defaultInt = 10)
    public void setMeasureTime(RNHeartBeatView view, @Nullable int measureTime) {
        view.setMeasureTime(measureTime);
    }

    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        for (RNHeartBeatView.Events event : RNHeartBeatView.Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    private boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            ArrayList<String> permissionsNotGranted = new ArrayList<>();
            for (String permission : neededPermissions) {
                if (ContextCompat.checkSelfPermission(reactContext, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNotGranted.add(permission);
                }
            }
            if (permissionsNotGranted.size() > 0) {
                boolean shouldShowAlert = false;
                for (String permission : permissionsNotGranted) {
                    shouldShowAlert = ActivityCompat.shouldShowRequestPermissionRationale(reactContext.getCurrentActivity(), permission);
                }
                if (shouldShowAlert) {
                    showPermissionAlert(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));
                } else {
                    requestPermissions(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));
                }
                return false;
            }
        }
        return true;
    }

    private void showPermissionAlert(final String[] permissions) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(reactContext);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle(PERMISSION_REQUIRED);
        alertBuilder.setMessage(PERMISSION_MESSAGE);
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(permissions);
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(reactContext.getCurrentActivity(), permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        // Not all permissions granted. Show message to the user.
                        return;
                    }
                }

                // All permissions are granted. So, do the appropriate work now.
                break;
        }

    }


    private static Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            Log.d(TAG,"onPreviewFrame 1 ");
            if (data == null) throw new NullPointerException();
            Camera.Size size = cam.getParameters().getPreviewSize();
            Log.d(TAG,"onPreviewFrame 2");
            if (size == null) throw new NullPointerException();
            Log.d(TAG,"onPreviewFrame 3");
            if (!processing.compareAndSet(false, true)) return;

            Log.d(TAG,"onPreviewFrame 4");
            int width = size.width;
            int height = size.height;

            int imgAvg = Utils.decodeYUV420SPtoRedAvg(data.clone(), height, width);
            // Log.i(TAG, "imgAvg="+imgAvg);
//            if (imgAvg == 0 || imgAvg == 255) {
//                processing.set(false);
//                return;
//            }

            int averageArrayAvg = 0;
            int averageArrayCnt = 0;
            for (int i = 0; i < averageArray.length; i++) {
                if (averageArray[i] > 0) {
                    averageArrayAvg += averageArray[i];
                    averageArrayCnt++;
                }
            }

            int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
            RNHeartBeatViewManager.TYPE newType = currentType;
            if (imgAvg < rollingAverage) {
                newType = RNHeartBeatViewManager.TYPE.RED;
                if (newType != currentType) {
                    beats++;
                    // Log.d(TAG, "BEAT!! beats="+beats);
                }
            } else if (imgAvg > rollingAverage) {
                newType = RNHeartBeatViewManager.TYPE.GREEN;
            }

            if (averageIndex == averageArraySize) averageIndex = 0;
            averageArray[averageIndex] = imgAvg;
            averageIndex++;

            // Transitioned from one state to another to the same
            if (newType != currentType) {
                currentType = newType;

            }

            Log.d(TAG,"onPreviewFrame 5");

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 10) {
                double bps = (beats / totalTimeInSecs);
                Log.d(TAG,"onPreviewFrame 66" + bps);
                int dpm = (int) (bps * 60d);
                if (dpm < 30 || dpm > 180) {
                    startTime = System.currentTimeMillis();
                    beats = 0;
                    processing.set(false);

                    return;
                }

                Log.d(TAG,"onPreviewFrame 77" + bps);
                // Log.d(TAG,
                // "totalTimeInSecs="+totalTimeInSecs+" beats="+beats);

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
                int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                startTime = System.currentTimeMillis();
                beats = 0;
                Log.d(TAG,"Heartrate: " + beatsAvg);
            }
            Log.d(TAG,"averageArrayAvg: " + averageArrayAvg);

            processing.set(false);
        }
    };

}
