package com.reactlibrary;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.annotations.ReactProp;

public class RNHeartBeatViewManager extends SimpleViewManager<RNHeartBeatView> {

    @Override
    public String getName() {
        return "RNHeartBeatView";
    }

    @Override
    protected RNHeartBeatView createViewInstance(ThemedReactContext reactContext) {
        return new RNHeartBeatView(reactContext);
    }
}
