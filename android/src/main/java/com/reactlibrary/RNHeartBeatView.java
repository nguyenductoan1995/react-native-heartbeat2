package com.reactlibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.reactlibrary.Camera.CameraManager;
import com.reactlibrary.Camera.CameraPreview;
import com.reactlibrary.Camera.CameraPreviewCallback;
import com.reactlibrary.Camera.EventEmiter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.Manifest.permission.CAMERA;

    public class RNHeartBeatView extends ViewGroup implements ActivityCompat.OnRequestPermissionsResultCallback, CameraPreviewCallback {
    private final static String TAG = "RNHeartBeatView";
    private final static int REQUEST_CODE = 100;

    private final static String PERMISSION_REQUIRED = "Camera Permission";
    private final static String PERMISSION_MESSAGE = "Please allow to access your camera";
    private final static String[] neededPermissions = new String[]{CAMERA};

    private static ThemedReactContext reactContext;

    private int measureTime = 10;
    private int framePerSecond = 30;
    private boolean enabled = false;


    public RNHeartBeatView(ThemedReactContext context) {
        super(context);
        reactContext = context;
    }


    public void setEnabled(boolean enabled) {
        boolean granted = this.checkPermission();
        if(!granted) {
            EventEmiter.emitOnErrorOccured(reactContext, EventEmiter.Errors.CAMERA_PERMISSION_DENIED);
            return;
        }
        if (this.enabled != enabled) {
            if (enabled) {
                this.startCamera();
            } else {
                this.stopCamera();
            }
            this.enabled = enabled;
        }
    }

    public void setMeasureTime(int measureTime) {
        this.measureTime = measureTime;
    }

    public void setFramePerSecond(int framePerSecond) {
        this.framePerSecond = framePerSecond;
    }


    private CameraPreview mPreview;
    private int mInitType = CameraManager.TYPE_CAMERA_BACK;


    public void startCamera() {
        if (null == this.mPreview) {
            mPreview = new CameraPreview(getContext(), mInitType,this);
            mPreview.setMeasureTime(measureTime);
            addView(mPreview);
            requestLayout();
        } else {
            mPreview.startPreview();
        }
    }

    public void stopCamera() {
        if (null != this.mPreview) {
            mPreview.stopCamera();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        startCamera();
        layoutViewFinder(l, t, r, b);
    }

    private void layoutViewFinder(int left, int top, int right, int bottom) {
        if (null == mPreview) {
            return;
        }
        float width = right - left;
        float height = bottom - top;
        int viewfinderWidth;
        int viewfinderHeight;
        double ratio;
        ratio = this.mPreview.getRatio();

        if (ratio == 0) {
            this.mPreview.layout(0, 0, (int) width, (int) height);
            return;
        }

        if (ratio * height < width) {
            viewfinderHeight = (int) (width / ratio);
            viewfinderWidth = (int) width;
        } else {
            viewfinderWidth = (int) (ratio * height);
            viewfinderHeight = (int) height;
        }

        int viewFinderPaddingX = (int) ((width - viewfinderWidth) / 2);
        int viewFinderPaddingY = (int) ((height - viewfinderHeight) / 2);

        this.mPreview.layout(viewFinderPaddingX, viewFinderPaddingY, viewFinderPaddingX + viewfinderWidth, viewFinderPaddingY + viewfinderHeight);
        this.postInvalidate(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }

    private void autoFocus() {
        mPreview.autoFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            autoFocus();
        }
        return true;
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

    @Override
    public void onStart() {
        EventEmiter.emitOnStart(reactContext);
    }

    @Override
    public void onStop() {
        EventEmiter.emitOnStop(reactContext);
    }

    @Override
    public void onReady() {
        EventEmiter.emitOnReady(reactContext);
    }

    @Override
    public void onErrorOrcured(EventEmiter.Errors error) {
        EventEmiter.emitOnErrorOccured(reactContext,error);
    }

    @Override
    public void onFinish(float heartRate) {
        EventEmiter.emitOnFinish(reactContext,heartRate);
    }

    @Override
    public void onValueChanged(float heartRate, float displaySeconds) {
        EventEmiter.emitOnValueChanged(reactContext,heartRate,displaySeconds);
    }
}
