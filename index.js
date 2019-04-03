// @flow
import React from "react";
import {
  NativeModules,
  NativeEventEmitter,
  requireNativeComponent
} from "react-native";

const { RNHeartBeat } = NativeModules;
const RNHeartBeatView = requireNativeComponent(
  "RNHeartBeatViewManager",
  RNHeartBeat
);

const RNHeartBeatEventEmitter = new NativeEventEmitter(RNHeartBeat);

export type EventTypes = "didUpdateHeartRate";

const eventHandlers = {
  didUpdateHeartRate: new Map()
};

const addEventListener = (
  type: EventTypes,
  handler: (heartRate: number) => void
) => {
  switch (type) {
    case "didUpdateHeartRate":
      eventHandlers[type].set(
        handler,
        RNHeartBeatEventEmitter.addListener(type, handler)
      );
      break;
    default:
      console.log(`Event with type ${type} does not exist.`);
  }
};

function startDetection(seconds: number = 30, framePerSecond: number = 30) {
  if (!RNHeartBeat) {
    throw Error("RNHeartBeat not founded !");
  }
  const validSeconds = seconds < 10 ? 10 : seconds > 30 ? 30 : seconds;
  const validFps =
    framePerSecond < 30 ? 30 : framePerSecond > 60 ? 60 : framePerSecond;
  RNHeartBeat.startDetection(validSeconds, validFps);
}

function stopDetection() {
  if (!RNHeartBeat) {
    throw Error("RNHeartBeat not founded !");
  }
  RNHeartBeat.stopDetection();
}

const removeAllListeners = () => {
  RNHeartBeatEventEmitter.removeAllListeners("didUpdateHeartRate");
};

class View extends React.Component {
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
