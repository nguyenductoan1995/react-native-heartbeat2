package com.reactlibrary.Camera;

import android.content.Context;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class EventEmiter {
    public static enum Errors {
        CAMERA_PERMISSION_DENIED(2000, "Camera permission denied."),
        CAMERA_DEVICE_NOT_AVAILABLE(2001, "Camera device not available."),
        CAMERA_PREVIEW_SETTINGS_FAILURE(2002, "Setting camera preview failure."),
        CAMERA_OUTPUT_NOT_AVAILABLE(2003, "Camera device not available."),
        CAMERA_CONNECTION_NOT_AVAILABLE(2004, "Camera device not available."),
        ERROR_WHILE_CALCULATION(2005, "Error while calculation."),
        SKIN_DETECTION_FAILURE(2006, "Skin detection failure.");

        private final int id;
        private final String message;

        Errors(int id, String message) {
            this.id = id;
            this.message = message;
        }

        public int getId() { return id; }
        public String getMessage() { return message; }
    }

    public static void emitOnStart(Context context) {
        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onStart", null);
    }

    public static void emitOnStop(Context context) {
        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onStop", null);
    }

    public static void emitOnReady(Context context) {
        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onReady", null);
    }

    public static void emitOnErrorOccured(Context context, Errors error) {
        WritableMap payload = Arguments.createMap();
        int errorCode = error.getId();
        String errorMessage = error.getMessage();
        payload.putInt("errorCode", errorCode);
        payload.putString("errorMessage", errorMessage);

        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onErrorOccured", payload);
    }

    public static void emitOnValueChanged(Context context, float heartRate,float displaySeconds) {
        WritableMap payload = Arguments.createMap();
        payload.putDouble("heartRate",heartRate);
        payload.putDouble("displaySeconds",displaySeconds);
        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onValueChanged", payload);
    }

    public static void emitOnFinish(Context context, float heartRate) {
        WritableMap payload = Arguments.createMap();
        payload.putDouble("heartRate",heartRate);
        ReactContext reactContext = (ReactContext) context;
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onFinish", payload);
    }

}
