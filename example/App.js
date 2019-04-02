/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, { Component } from "react";
import {
  Platform,
  StyleSheet,
  Text,
  View,
  TouchableOpacity
} from "react-native";
import RNHeartBeat from "react-native-heart-beat";

const instructions = Platform.select({
  ios: "Press Cmd+R to reload,\n" + "Cmd+D or shake for dev menu",
  android:
    "Double tap R on your keyboard to reload,\n" +
    "Shake or press menu button for dev menu"
});

type Props = {};

type State = {
  heartRate: number
};
export default class App extends Component<Props> {
  state = {
    heartRate: 0
  };
  componentDidMount() {
    RNHeartBeat.addEventListener("didUpdateHeartRate", ({ heartRate }) => {
      console.log("didUpdateHeartRate: ", heartRate);
      this.setState({ heartRate });
    });
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>To get started, edit App.js</Text>
        <Text style={styles.instructions}>{instructions}</Text>
        <Text style={styles.instructions}>{this.state.heartRate}</Text>
        <TouchableOpacity
          style={{ width: 120, height: 60, backgroundColor: "rgba(0,0,0,0.2)" }}
          onPress={() => RNHeartBeat.startDetection()}
        >
          <Text>Start Measure</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={{ width: 120, height: 60, backgroundColor: "rgba(0,0,0,0.2)" }}
          onPress={() => RNHeartBeat.stopDetection()}
        >
          <Text>Stop Measure</Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#F5FCFF"
  },
  welcome: {
    fontSize: 20,
    textAlign: "center",
    margin: 10
  },
  instructions: {
    textAlign: "center",
    color: "#333333",
    marginBottom: 5
  }
});
