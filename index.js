// @flow
import React from "react";
import {
  NativeModules,
  NativeEventEmitter,
  requireNativeComponent,
  DeviceEventEmitter,
  EmitterSubscription,
  Platform
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

const isAndroid = Platform.OS === "android";

export default class View extends React.Component<Props> {
  _subscriptions: EmitterSubscription[] = [];

  componentWillMount() {
    if (isAndroid) {
      const onReady = DeviceEventEmitter.addListener("onReady", this._onReady);
      const onStart = DeviceEventEmitter.addListener("onStart", this._onStart);
      const onStop = DeviceEventEmitter.addListener("onStop", this._onStop);
      const onValueChanged = DeviceEventEmitter.addListener(
        "onValueChanged",
        this._onValueChanged
      );
      const onFinish = DeviceEventEmitter.addListener(
        "onFinish",
        this._onFinish
      );
      const onErrorOccured = DeviceEventEmitter.addListener(
        "onErrorOccured",
        this._onErrorOccured
      );

      this._subscriptions.push(onReady);
      this._subscriptions.push(onStart);
      this._subscriptions.push(onReady);
      this._subscriptions.push(onStop);
      this._subscriptions.push(onValueChanged);
      this._subscriptions.push(onErrorOccured);
    }
  }

  componentWillUnmount() {
    if (isAndroid) {
      this._subscriptions.forEach(subscription => {
        if (subscription) {
          subscription.remove();
        }
      });
    }
  }

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
    if (onErrorOccured && event) {
      const { errorMessage, errorCode } = isAndroid ? event : event.nativeEvent;
      onErrorOccured(errorCode, errorMessage);
    }
  };

  _onValueChanged = event => {
    const { onValueChanged } = this.props;
    if (onValueChanged && event) {
      const { heartRate, displaySeconds } = isAndroid
        ? event
        : event.nativeEvent;
      onValueChanged(heartRate, displaySeconds);
    }
  };

  _onFinish = event => {
    const { onFinish } = this.props;
    if (onFinish && event) {
      const { heartRate } = isAndroid ? event : event.nativeEvent;
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
