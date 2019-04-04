/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, { Component } from "react";
import { Platform, StyleSheet, Text, View, TouchableOpacity } from "react-native";

import RNHeartBeat from "react-native-heart-beat";

const instructions = Platform.select({
  ios: "Press Cmd+R to reload,\n" + "Cmd+D or shake for dev menu",
  android: "Double tap R on your keyboard to reload,\n" + "Shake or press menu button for dev menu"
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
        <RNHeartBeat.View
          enabled={this.state.enabled}
          style={{
            marginTop: 40,
            marginHorizontal: 20,
            width: 260,
            height: 200
            // backgroundColor: "gray"
          }}
        />
        <TouchableOpacity
          style={{ width: 120, height: 60, backgroundColor: "rgba(0,0,0,0.2)" }}
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
