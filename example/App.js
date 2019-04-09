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
  heartRate: number,
  enabled: boolean
};
export default class App extends Component<Props> {
  state = {
    heartRate: 0,
    enabled: false
  };

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.instructions}>{this.state.heartRate}</Text>
        <RNHeartBeat
          enabled={this.state.enabled}
          measureTime={5}
          framePerSecond={30}
          onErrorOccured={(errorCode, errorMessage) => {
            console.log("onError: ", errorCode, errorMessage);
          }}
          onReady={() => {
            console.log("onReady");
          }}
          onFinish={heartRate => {
            console.log("onFinish: ", heartRate);
          }}
          onStart={() => {
            console.log("onStart: ");
          }}
          onStop={() => {
            console.log("onStop: ");
          }}
          onValueChanged={(heartRate, displaySecond) => {
            console.log("onValueChanged: ", heartRate, displaySecond);
          }}
          style={{
            marginTop: 40,
            marginHorizontal: 20,
            width: 260,
            height: 200,

            backgroundColor: "gray"
          }}
        />
        <TouchableOpacity
          style={{
            marginTop: 40,
            width: 120,
            height: 60,
            backgroundColor: "rgba(0,0,0,0.2)"
          }}
          onPress={() => this.setState({ enabled: true })}
        >
          <Text>Start Measure</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={{ width: 120, height: 60, backgroundColor: "rgba(0,0,0,0.2)" }}
          onPress={() => this.setState({ enabled: false })}
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
