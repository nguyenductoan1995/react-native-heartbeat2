// @flow
import React from "react";
import { NativeModules, NativeEventEmitter, requireNativeComponent } from "react-native";
import type { ViewStyleProp } from "react-native/Libraries/StyleSheet/StyleSheet";
const { RNHeartBeat } = NativeModules;
const RNHeartBeatView = requireNativeComponent("RNHeartBeatViewManager", RNHeartBeat);

function startDetection(seconds: number = 30, framePerSecond: number = 30) {
  if (!RNHeartBeat) {
    throw Error("RNHeartBeat not founded !");
  }
  const validSeconds = seconds < 10 ? 10 : seconds > 30 ? 30 : seconds;
  const validFps = framePerSecond < 30 ? 30 : framePerSecond > 60 ? 60 : framePerSecond;
  RNHeartBeat.startDetection(validSeconds, validFps);
}

function stopDetection() {
  if (!RNHeartBeat) {
    throw Error("RNHeartBeat not founded !");
  }
  RNHeartBeat.stopDetection();
}

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
  framePerSecond?: number
  onReady?: () => void,
  onStart?: () => void,
  onStop?: () => void,
  onError?: (errorCode: ErrorCodes, errorMessage: string) => void,
  onValueChanged?: (heartRate: number) => void
  onFinish?: (heartRate: number) => void
};

class View extends React.Component<Props> {
  render() {
    return <RNHeartBeatView {...this.props} />;
  }
}

export default {
  startDetection,
  stopDetection,
  addEventListener,
  removeAllListeners,
  View
};
