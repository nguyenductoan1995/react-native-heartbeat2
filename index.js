// @flow
import { NativeModules, NativeEventEmitter } from "react-native";

const { RNHeartBeat } = NativeModules;

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

export default {
  startDetection,
  stopDetection,
  addEventListener,
  removeAllListeners
};
