package com.reactlibrary.Camera;

public interface CameraPreviewCallback {
    void onStart();
    void onStop();
    void onReady();
    void onErrorOrcured(EventEmiter.Errors error);
    void onFinish(float heartRate);
    void onValueChanged(float heartRate, float displaySeconds);
}
