
# react-native-heart-beat

## Getting started

`$ npm install react-native-heart-beat --save`

### Mostly automatic installation

`$ react-native link react-native-heart-beat`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-heart-beat` and add `RNHeartBeat.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNHeartBeat.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNHeartBeatPackage;` to the imports at the top of the file
  - Add `new RNHeartBeatPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-heart-beat'
  	project(':react-native-heart-beat').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-heart-beat/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-heart-beat')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNHeartBeat.sln` in `node_modules/react-native-heart-beat/windows/RNHeartBeat.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Heart.Beat.RNHeartBeat;` to the usings at the top of the file
  - Add `new RNHeartBeatPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNHeartBeat from 'react-native-heart-beat';

// TODO: What to do with the module?
RNHeartBeat;
```
  