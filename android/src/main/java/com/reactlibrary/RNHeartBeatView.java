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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.support.annotation.Nullable;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.Manifest.permission.CAMERA;

public class RNHeartBeatView extends FrameLayout implements SurfaceHolder.Callback  {
    private final static String TAG = "RNHeartBeatView";

    private SurfaceView surface;


    private int measureTime = 10;
    private int framePerSecond = 30;
    private boolean enabled = false;
    private boolean previewing = false;
    private Camera mCamera;

    private final static int CAMERA_PERMISSION_DENIED = 2000;
    private final static int CAMERA_DEVICE_NOT_AVAILABLE = 2001;
    private final static int CAMERA_INPUT_NOT_AVAILABLE = 2002;
    private final static int CAMERA_OUTPUT_NOT_AVAILABLE = 2003;
    private final static int CAMERA_CONNECTION_NOT_AVAILABLE = 2004;
    private final static int ERROR_WHILE_CALCULATION = 2005;
    private final static int SKIN_DETECTION_FAILURE = 2006;


    public static enum Events {
        EVENT_CAMERA_READY("onReady"),
        EVENT_ON_STARRT("onStart"),
        EVENT_ON_STOP("onStop"),
        EVENT_ON_ERROR_OCCURED("onErrorOccured"),
        EVENT_ON_VALUE_CHANGED("onValueChanged"),
        EVENT_ON_FINISH("onFinish");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }



    public RNHeartBeatView(ThemedReactContext context) {
        super(context);
        surface = new SurfaceView(context);
        setBackgroundColor(Color.BLACK);
        addView(surface, MATCH_PARENT, MATCH_PARENT);
        surface.getHolder().addCallback(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        int actualPreviewWidth = getResources().getDisplayMetrics().widthPixels;
//        int actualPreviewHeight = getResources().getDisplayMetrics().heightPixels;
//        int height = Utils.convertDeviceHeightToSupportedAspectRatio(actualPreviewWidth, actualPreviewHeight);
//        surface.layout(0, 0, actualPreviewWidth, height);
    }

    public void setEnabled(boolean enabled) {
        if(this.enabled != enabled) {
            if(enabled) {
//                this.startCamera();
            } else {
//                this.stopCamera();
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


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        RNHeartBeatViewManager.setCameraView(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        RNHeartBeatViewManager.setCameraView(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        RNHeartBeatViewManager.removeCameraView();
    }


    public SurfaceHolder getHolder() {
        return surface.getHolder();
    }




//    private void startCamera() {
//        if(!previewing){
//            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//            final ReactContext context = (ReactContext) getContext();
//
//            if (mCamera != null){
//                try {
//                    mCamera.setPreviewCallback(previewCallback);
//                    mCamera.setPreviewDisplay(surface.getHolder());
//                    mCamera.startPreview();
//                    previewing = true;
//                    context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), Events.EVENT_ON_STARRT.toString(), null);
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            } else {
//                WritableMap map = Arguments.createMap();
//                map.putInt("errorCode",CAMERA_CONNECTION_NOT_AVAILABLE);
//                map.putString("errorMessage","Camera connection not available");
//                context.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), Events.EVENT_ON_STARRT.toString(), map);
//            }
//        }
//    }
//
//    private void stopCamera() {
//        if(mCamera != null && previewing){
//            surface.getHolder().removeCallback(this);
//            mCamera.stopPreview();
//            mCamera.release();
//            mCamera = null;
//            previewing = false;
//            Log.d(TAG,"Camera stopped");
//        }
//    }
//

}
