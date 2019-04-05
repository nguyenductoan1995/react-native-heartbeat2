//
//  RNHeartBeat.swift
//  RNHeartBeat
//
//  Created by Loi Relia on 4/2/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

import UIKit
import AVFoundation


let kDidUpdateHeartRate = "didUpdateHeartRate";
let kDidStartDetection = "didStartDetection";
let kDidStopDetection = "didStopDetection";
let kDidFinishDetection = "didFinishDetection";

let CAMERA_PERMISSION_DENIED = 2000;
let CAMERA_DEVICE_NOT_AVAILABLE = 2001;
let CAMERA_INPUT_NOT_AVAILABLE = 2002;
let CAMERA_OUTPUT_NOT_AVAILABLE = 2003;
let CAMERA_CONNECTION_NOT_AVAILABLE = 2004;
let ERROR_WHILE_CALCULATION = 2005;
let SKIN_DETECTION_FAILURE = 2006;

@objc(RNHeartBeatViewManager)
class RNHeartBeatViewManager: RCTViewManager {
    override func view() -> UIView! {
        print("Create View Manager")
        return RNHeartBeat();
    }
}

@objc(RNHeartBeat)
class RNHeartBeat: UIView {
    private var dataPointsHue: [Double] = []
    private var isDetecting = false
    private var captureDevice: AVCaptureDevice!
    private var previewLayer: AVCaptureVideoPreviewLayer!
    
    private var position = AVCaptureDevice.Position.back
    private let quality = AVCaptureSession.Preset.high
    
    private var permissionGranted = false
    private let sessionQueue = DispatchQueue(label: "session queue")
    private let captureSession = AVCaptureSession()
    private let context = CIContext()
   
    private var xv: [Double] = Array(repeating: 0, count: 9)
    private var yv: [Double] = Array(repeating: 0, count: 9)
    private var sampleCount = 0
    private var fps = 30
    private var seconds = 30
    private var heartRateSum: Float = 0
    
    @objc var onReady: RCTBubblingEventBlock?
    @objc var onStart: RCTBubblingEventBlock?
    @objc var onStop: RCTBubblingEventBlock?
    @objc var onErrorOccured: RCTBubblingEventBlock?
    @objc var onFinish: RCTBubblingEventBlock?
    @objc var onValueChanged: RCTBubblingEventBlock?
    

    override init(frame: CGRect) {
        super.init(frame: frame)
        previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
        previewLayer.needsDisplayOnBoundsChange = true
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds;
        previewLayer.backgroundColor = UIColor.black.withAlphaComponent(0.1).cgColor
        layer.insertSublayer(previewLayer, at: 0)
    
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    @objc var enabled: Bool {
        set(newValue) {
            if newValue {
                self.startDetection();
                return
            }
            self.stopDetection();
        }
        get {
            return self.isDetecting;
        }
    }
    
    @objc var measureTime: NSNumber {
        set(newValue) {
            self.seconds = Int(truncating: newValue)
        }
        get {
            return NSNumber(value: self.seconds);
        }
    }
    
    @objc var framePerSecond: NSNumber {
        set(newValue) {
            self.fps = Int(truncating: newValue)
        }
        get {
            return NSNumber(value: self.fps);
        }
    }

    private func checkPermission() {
        switch AVCaptureDevice.authorizationStatus(for: AVMediaType.video) {
        case .authorized:
            permissionGranted = true
        case .notDetermined:
            requestPermission()
        default:
            permissionGranted = false
        }
    }
    
    private func requestPermission() {
        sessionQueue.suspend()
        AVCaptureDevice.requestAccess(for: AVMediaType.video) { [unowned self] granted in
            self.permissionGranted = granted
            self.sessionQueue.resume()
        }
    }
    

    private func setTorch(isOn: Bool) {
        guard let captureDevice = selectCaptureDevice() else {
            print("RNHeartBeat::configureSession:: Fail to selectCaptureDevice")
            return
        }

        if captureDevice.hasFlash, captureDevice.isTorchAvailable {
            do {
                try captureDevice.lockForConfiguration()
                try captureDevice.setTorchModeOn(level: 1)
                captureDevice.torchMode = isOn ? .on : .off
                captureDevice.unlockForConfiguration()
                print("RNHeartBeat::configureSession:: Torch is turned on")
            } catch {
                print("RNHeartBeat::configureSession:: Torch could not be used")
            }
        } else {
            print("RNHeartBeat::configureSession:: Camera didn't support torch mode")
        }
    }
    
    private func setDesiredFrame(_ desiredFrameRate: Int) {
        guard let captureDevice = self.captureDevice else {
            print("RNHeartBeat::setDesiredFrame:: Camera device not available")
            return
        }
        var isFPSSupported = false
        do {
            for range in captureDevice.activeFormat.videoSupportedFrameRateRanges {
                if (range.maxFrameRate >= Double(desiredFrameRate) && range.minFrameRate <= Double(desiredFrameRate)) {
                    isFPSSupported = true
                    break
                }
            }
            
            if isFPSSupported {
                try captureDevice.lockForConfiguration()
                captureDevice.activeVideoMaxFrameDuration = CMTimeMake(value: 1, timescale: Int32(desiredFrameRate))
                captureDevice.activeVideoMinFrameDuration = CMTimeMake(value: 1, timescale: Int32(desiredFrameRate))
                captureDevice.unlockForConfiguration()
            }
            
        } catch {
            print("lockForConfiguration error: \(error.localizedDescription)")
        }
    }
    
    private func configureSession() {
        guard permissionGranted else {
            enabled = false
            onErrorOccured?(["errorCode": CAMERA_PERMISSION_DENIED,"errorMessage": "Camera perssion denied"])
            return
        }
        
        captureSession.sessionPreset = quality
        
        guard let captureDevice = selectCaptureDevice() else {
            onErrorOccured?(["errorCode": CAMERA_DEVICE_NOT_AVAILABLE,"errorMessage": "Camera device not available"])
            return
        }
        self.captureDevice = captureDevice
    
        guard let captureDeviceInput = try? AVCaptureDeviceInput(device: captureDevice) else {
            onErrorOccured?(["errorCode": CAMERA_INPUT_NOT_AVAILABLE,"errorMessage": "Camera input not available"])
            return
        }
        
        guard captureSession.canAddInput(captureDeviceInput) else {
            onErrorOccured?(["errorCode": CAMERA_INPUT_NOT_AVAILABLE,"errorMessage": "Camera input not available"])
            return
            
        }
        
        captureSession.addInput(captureDeviceInput)
        
        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.setSampleBufferDelegate(self, queue: sessionQueue)
        
        guard captureSession.canAddOutput(videoOutput) else {
            onErrorOccured?(["errorCode": CAMERA_OUTPUT_NOT_AVAILABLE,"errorMessage": "Camera output not available"])
            return
            
        }
        
        captureSession.addOutput(videoOutput)
        guard let connection = videoOutput.connection(with: AVFoundation.AVMediaType.video) else {
            onErrorOccured?(["errorCode": CAMERA_CONNECTION_NOT_AVAILABLE,"errorMessage": "Camera connection not available"])
            return
        }
        
        guard connection.isVideoOrientationSupported else { return }
        guard connection.isVideoMirroringSupported else { return }
        connection.videoOrientation = .portrait
        connection.isVideoMirrored = position == .front
        previewLayer.connection?.videoOrientation = .portrait
        
        if let onReady = self.onReady {
            onReady(nil)
        }
    }
    
    private func selectCaptureDevice() -> AVCaptureDevice? {
        return AVCaptureDevice.devices().filter {
            ($0 as AnyObject).hasMediaType(AVMediaType.video) &&
                ($0 as AnyObject).position == position
            }.first
    }
    
    private func imageFromSampleBuffer(sampleBuffer: CMSampleBuffer) -> UIImage? {
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return nil }
        let ciImage = CIImage(cvPixelBuffer: imageBuffer)
        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
    
    
    
    private func butterworthBandpassFilter(input data: [Double]) -> [Double]? {
        if data.count == 0 {
            return nil
        }
        
        // http://www-users.cs.york.ac.uk/~fisher/cgi-bin/mkfscript
        // Butterworth Bandpass filter
        // 4th order
        // sample rate - varies between possible camera frequencies. Either 30, 60, 120, or 240 FPS
        // corner1 freq. = 0.667 Hz (assuming a minimum heart rate of 40 bpm, 40 beats/60 seconds = 0.667 Hz)
        // corner2 freq. = 4.167 Hz (assuming a maximum heart rate of 250 bpm, 250 beats/60 secods = 4.167 Hz)
        // Bandpass filter was chosen because it removes frequency noise outside of our target range (both higher and lower)
        let dGain: Double = 1.232232910e+02
        
        var outputData: [Double] = []
        for input in data {
           
            xv[0] = xv[1]
            xv[1] = xv[2]
            xv[2] = xv[3]
            xv[3] = xv[4]
            xv[4] = xv[5]
            xv[5] = xv[6]
            xv[6] = xv[7]
            xv[7] = xv[8]
            xv[8] = input / dGain
            yv[0] = yv[1]
            yv[1] = yv[2]
            yv[2] = yv[3]
            yv[3] = yv[4]
            yv[4] = yv[5]
            yv[5] = yv[6]
            yv[6] = yv[7]
            yv[7] = yv[8]
            yv[8] = (xv[0] + xv[8]) - 4 * (xv[2] + xv[6]) + 6 * xv[4] + (-0.1397436053 * yv[0]) + (1.2948188815 * yv[1]) + (-5.4070037946 * yv[2]) + (13.2683981280 * yv[3]) + (-20.9442560520 * yv[4]) + (21.7932169160 * yv[5]) + (-14.5817197500 * yv[6]) + (5.7161939252 * yv[7])
            
            outputData.append(yv[8])
        }
        
        return outputData
    }
    
    private func medianSmoothing(input data: [Double]?) -> [Double]? {
        guard let inputData = data else { return nil }
        if inputData.count == 0 {
            return nil
        }
        
        var newData: [Double] = []
        
        for i in 0..<inputData.count {
            if i == 0 || i == 1 || i == 2 || i == inputData.count - 1 || i == inputData.count - 2 || i == inputData.count - 3 {
                newData.append(inputData[i])
            } else {
                let items = ([
                    inputData[i - 2],
                    inputData[i - 1],
                    inputData[i],
                    inputData[i + 1],
                    inputData[i + 2]
                    ] as NSArray).sortedArray(using: [NSSortDescriptor(key: "self", ascending: true)])
                
                newData.append(items[2] as! Double)
            }
        }
        
        return newData
    }

    
    private func peakCount(input data: [Double]?) -> Int {
        guard let inputData = data else { return 0 }
        if inputData.count == 0 {
            return 0
        }
        
        var count = 0
        var i = 3
        
        while i < inputData.count - 3 {
            if inputData[i] > 0 && inputData[i] > inputData[i - 1]
                && inputData[i] > inputData[i - 2] && inputData[i] > inputData[i - 3] && inputData[i] >= inputData[i + 1] && inputData[i] >= inputData[i + 2]
                && inputData[i] >= inputData[i + 3] {
                count = count + 1
                i = i + 4
            } else {
                i = i + 1
            }
        }
        
        return count
    }
    
    private func startDetection() {
        if isDetecting { return }
        checkPermission()
        isDetecting = true
        sampleCount = 0
       
        sessionQueue.async { [unowned self] in
            self.configureSession()
            self.captureSession.startRunning()
            
        }
        onStart?(nil)
    }
    
    private func stopDetection() {
        dataPointsHue.removeAll()
        previewLayer.removeFromSuperlayer()
        onStop?(nil)
        isDetecting = false
        sampleCount = 0
        heartRateSum = 0
        
        sessionQueue.async { [unowned self] in
            self.captureSession.stopRunning()
        }
        
    }
  
}

extension RNHeartBeat: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ captureOutput: AVCaptureOutput!, didOutputSampleBuffer sampleBuffer: CMSampleBuffer!, from connection: AVCaptureConnection!) {
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return  }
        
        guard let cvimgRef = CMSampleBufferGetImageBuffer(sampleBuffer) else { return  }
        
        // Lock the image buffer
        CVPixelBufferLockBaseAddress(cvimgRef,CVPixelBufferLockFlags(rawValue: 0));
        
        setTorch(isOn: true)
        
        let inputImage = CIImage(cvPixelBuffer: imageBuffer)
        let extentVector = CIVector(x: inputImage.extent.origin.x, y: inputImage.extent.origin.y, z: inputImage.extent.size.width, w: inputImage.extent.size.height)
        
        guard let filter = CIFilter(name: "CIAreaAverage", parameters: [kCIInputImageKey: inputImage, kCIInputExtentKey: extentVector]) else { return }
        guard let outputImage = filter.outputImage else { return }
        
        var bitmap = [UInt8](repeating: 0, count: 4)
        let context = CIContext(options: [.workingColorSpace: kCFNull])
        context.render(outputImage, toBitmap: &bitmap, rowBytes: 4, bounds: CGRect(x: 0, y: 0, width: 1, height: 1), format: .RGBA8, colorSpace: nil)
        
        let color =  UIColor(red: CGFloat(bitmap[0]) / 255, green: CGFloat(bitmap[1]) / 255, blue: CGFloat(bitmap[2]) / 255, alpha: CGFloat(bitmap[3]) / 255)
        
        var hue: CGFloat = 0.0
        var sat: CGFloat = 0.0
        var bright: CGFloat = 0.0
        
        
        color.getHue(&hue, saturation: &sat, brightness: &bright, alpha: nil)
        if hue < 0.8, sat < 0.8 {
            onErrorOccured?(["errorCode": SKIN_DETECTION_FAILURE,"errorMessage": "Skin detection error"])
            return
        }
        dataPointsHue.append(Double(hue))
        
        print("Hue value: ", hue)
        print("Saturation value: ", sat)
        
        if dataPointsHue.count % fps == 0 {
            let displaySeconds = Float(dataPointsHue.count / fps)
            
            guard let bandpassFilteredItems = butterworthBandpassFilter(input: dataPointsHue) else {
                onErrorOccured?(["errorCode": ERROR_WHILE_CALCULATION,"errorMessage": "BandpassFilteredItems is nil"])
                return
            }
            guard let smoothedBandpassItems = medianSmoothing(input: bandpassFilteredItems) else {
                onErrorOccured?(["errorCode": ERROR_WHILE_CALCULATION,"errorMessage": "SmoothedBandpassItems is nil"])
                return
            }
            let peak = peakCount(input: smoothedBandpassItems)
            
            let secondsPassed = Float(smoothedBandpassItems.count / fps)
            let percentage: Float = secondsPassed / 60
            let heartRate = Float(peak) / percentage
            heartRateSum += heartRate
            sampleCount += 1
            onValueChanged?(["heartRate": heartRate, "displaySeconds": displaySeconds])
        }

        if dataPointsHue.count == (seconds * fps) {
            print("captureOutput:: heartRateSum = \(heartRateSum),sampleCount = \(sampleCount)")
            if sampleCount > 0 && heartRateSum > 0 {
                let heartRate = heartRateSum / Float(sampleCount)
                onFinish?(["heartRate": heartRate])
                enabled = false
                sampleCount = 0
                heartRateSum = 0
            }
        }
        
        CVPixelBufferUnlockBaseAddress(cvimgRef, CVPixelBufferLockFlags(rawValue: 0))
    }
}

