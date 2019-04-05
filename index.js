// @flow
import React from "react";
import {
  NativeModules,
  NativeEventEmitter,
  requireNativeComponent
} from "react-native";
import type { ViewStyleProp } from "react-native/Libraries/StyleSheet/StyleSheet";
const { RNHeartBeat } = NativeModules;
const RNHeartBeatView = requireNativeComponent("RNHeartBeatView", RNHeartBeat);

type ErrorCodes = {
  CAMERA_PERMISSION_DENIED: 2000,
  CAMERA_DEVICE_NOT_AVAILABLE: 2001,
  CAMERA_INPUT_NOT_AVAILABLE: 2002,
  CAMERA_OUTPUT_NOT_AVAILABLE: 2003,
  CAMERA_CONNECTION_NOT_AVAILABLE: 2004
};

type Props = {
  enabled: number,
  style?: ViewStyleProp,
  measureTime?: number,
  framePerSecond?: number,
  onReady?: () => void,
  onStart?: () => void,
  onStop?: () => void,
  onErrorOccured?: (errorCode: ErrorCodes, errorMessage: string) => void,
  onValueChanged?: (heartRate: number, displaySeconds: Number) => void,
  onFinish?: (heartRate: number) => void
};

export default class View extends React.Component<Props> {
  _onReady = () => {
    const { onReady } = this.props;
    if (onReady) {
      onReady();
    }
  };

  _onStart = () => {
    const { onStart } = this.props;
    if (onStart) {
      onStart();
    }
  };

  _onStop = () => {
    const { onStop } = this.props;
    if (onStop) {
      onStop();
    }
  };

  _onErrorOccured = event => {
    const { onErrorOccured } = this.props;
    if (onErrorOccured) {
      const { errorMessage, errorCode } = event.nativeEvent;
      onErrorOccured(errorCode, errorMessage);
    }
  };

  _onValueChanged = event => {
    const { onValueChanged } = this.props;
    if (onValueChanged) {
      const { heartRate, displaySeconds } = event.nativeEvent;
      onValueChanged(heartRate, displaySeconds);
    }
  };

  _onFinish = event => {
    const { onFinish } = this.props;
    if (onFinish) {
      const { heartRate } = event.nativeEvent;
      onFinish(heartRate);
    }
  };

  render() {
    const { measureTime = 10, framePerSecond = 30, ...other } = this.props;
    const validSeconds =
      measureTime < 10 ? 10 : measureTime > 30 ? 30 : measureTime;
    const validFps =
      framePerSecond < 30 ? 30 : framePerSecond > 60 ? 60 : framePerSecond;
    return (
      <RNHeartBeatView
        {...other}
        onStart={this._onStart}
        onStop={this._onStop}
        onReady={this._onReady}
        onValueChanged={this._onValueChanged}
        onFinish={this._onFinish}
        onErrorOccured={this._onErrorOccured}
        measureTime={measureTime}
        framePerSecond={framePerSecond}
      />
    );
  }
}
