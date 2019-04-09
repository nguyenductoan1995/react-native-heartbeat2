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
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

public class RNHeartBeatViewManager extends SimpleViewManager<RNHeartBeatView>  {
    private final static String TAG = "RNHeartBeatViewManager";

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


    @Override
    public String getName() {
        return "RNHeartBeatView";
    }

    @Override
    protected RNHeartBeatView createViewInstance(ThemedReactContext reactContext) {
        return new RNHeartBeatView(reactContext);
    }

    @ReactProp(name = "enabled", defaultBoolean = false)
    public void setEnabled(RNHeartBeatView view, boolean enabled) {
        view.setEnabled(enabled);
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
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }
}
