//
//  RNHeartBeat.swift
//  RNHeartBeat
//
//  Created by Loi Relia on 4/2/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

import Foundation
import AVFoundation


let kDidUpdateHeartRate = "didUpdateHeartRate";
let kDidStartDetection = "didStartDetection";
let kDidStopDetection = "didStopDetection";
let kDidFinishDetection = "kDidFinishDetection";


protocol FrameExtractorDelegate: class {
    func captured(image: UIImage)
}

@objc(RNHeartBeat)
class RNHeartBeat: RCTEventEmitter {
    private var dataPointsHue: [Double] = []
    private var isDetecting = false
    private var captureDevice: AVCaptureDevice!
    
    private var position = AVCaptureDevice.Position.back
    private let quality = AVCaptureSession.Preset.high
    
    private var permissionGranted = false
    private let sessionQueue = DispatchQueue(label: "session queue")
    private let captureSession = AVCaptureSession()
    private let context = CIContext()
   
    private var xv: [Double] = Array(repeating: 0, count: 9)
    private var yv: [Double] = Array(repeating: 0, count: 9)
    private var sampleCount = 0
    private var framePerSecond = 30
    private var seconds = 30
    
    weak var delegate: FrameExtractorDelegate?
    
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
    
    private func turnOnFlash() {
        guard let captureDevice = selectCaptureDevice() else {
            print("RNHeartBeat::configureSession:: Fail to selectCaptureDevice")
            return
        }
        
        if captureDevice.hasFlash {
            do {
                try captureDevice.lockForConfiguration()
                captureDevice.flashMode = .on
                captureDevice.unlockForConfiguration()
            } catch {
                print("RNHeartBeat::configureSession:: Flash could not be used")
            }
        } else {
            print("RNHeartBeat::configureSession:: Camera didn't support flash mode")
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
        guard permissionGranted else { return }
        captureSession.sessionPreset = quality
        
        guard let captureDevice = selectCaptureDevice() else {
            print("RNHeartBeat::configureSession:: Fail to selectCaptureDevice")
            return
        }
        self.captureDevice = captureDevice
        if captureDevice.hasFlash {
            do {
                try captureDevice.lockForConfiguration()
                captureDevice.flashMode = .on
                captureDevice.unlockForConfiguration()
                print("RNHeartBeat::configureSession:: Flash turned on")
            } catch {
                print("RNHeartBeat::configureSession:: Flash could not be used")
            }
        } else {
            print("RNHeartBeat::configureSession:: Camera didn't support flash mode")
        }
        
        guard let captureDeviceInput = try? AVCaptureDeviceInput(device: captureDevice) else { return }
        guard captureSession.canAddInput(captureDeviceInput) else { return }
        captureSession.addInput(captureDeviceInput)
        
        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue(label: "sample buffer"))
        
        guard captureSession.canAddOutput(videoOutput) else { return }
        captureSession.addOutput(videoOutput)
        guard let connection = videoOutput.connection(with: AVFoundation.AVMediaType.video) else { return }
        guard connection.isVideoOrientationSupported else { return }
        guard connection.isVideoMirroringSupported else { return }
        connection.videoOrientation = .portrait
        connection.isVideoMirrored = position == .front
        
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
    
    @objc func startDetection(_ seconds: Int = 30, framePerSecond: Int = 30) {
        print("RNHeartBeat::startDetection:: startDetection --- isDetecting: ", isDetecting)
        if isDetecting { return }
        
        checkPermission()
        sessionQueue.async { [unowned self] in
            self.configureSession()
            self.captureSession.startRunning()
            self.isDetecting = true
            self.sampleCount = 0
            self.setDesiredFrame(framePerSecond)
            self.turnOnFlash()
            self.seconds = seconds
            self.framePerSecond = framePerSecond
        }
    }
    
    @objc func stopDetection() {
        print("RNHeartBeat::startDetection:: stopDetection")
        dataPointsHue.removeAll()
        sessionQueue.async { [unowned self] in
            self.configureSession()
            self.captureSession.stopRunning()
            self.isDetecting = false
            self.sampleCount = 0
        }
    }
    
    override func supportedEvents() -> [String]! {
        return [
            kDidUpdateHeartRate,
            kDidStartDetection,
            kDidStopDetection,
        ]
    }
}

extension RNHeartBeat: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ captureOutput: AVCaptureOutput!, didOutputSampleBuffer sampleBuffer: CMSampleBuffer!, from connection: AVCaptureConnection!) {
        guard let uiImage = imageFromSampleBuffer(sampleBuffer: sampleBuffer) else { return }
        DispatchQueue.main.async { [unowned self] in
            self.delegate?.captured(image: uiImage)
        }
    }
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return  }
        
        guard let cvimgRef = CMSampleBufferGetImageBuffer(sampleBuffer) else { return  }
        
        // Lock the image buffer
        CVPixelBufferLockBaseAddress(cvimgRef,CVPixelBufferLockFlags(rawValue: 0));
        
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
        print("Hue value: ", hue)
        print("Saturation value: ", sat)
        
        color.getHue(&hue, saturation: &sat, brightness: &bright, alpha: nil)
        dataPointsHue.append(Double(hue))
        
        var heartRateSum: Float = 0
        if dataPointsHue.count % framePerSecond == 0 {
            let displaySeconds = Float(dataPointsHue.count / framePerSecond)
            
            guard let bandpassFilteredItems = butterworthBandpassFilter(input: dataPointsHue) else {
                print("captureOutput::bandpassFilteredItems:: bandpassFilteredItems is nil")
                return
            }
            guard let smoothedBandpassItems = medianSmoothing(input: bandpassFilteredItems) else {
                print("captureOutput::bandpassFilteredItems:: bandpassFilteredItems is nil")
                return
            }
            let peak = peakCount(input: smoothedBandpassItems)
            
            let secondsPassed = Float(smoothedBandpassItems.count / framePerSecond)
            let percentage: Float = secondsPassed / 60
            let heartRate = Float(peak) / percentage
            heartRateSum += heartRate
            sampleCount += 1
            sendEvent(withName: kDidUpdateHeartRate, body: ["heartRate": heartRate, "displaySeconds": displaySeconds])
            print("captureOutput:: heartRate = \(heartRate),displaySeconds = \(displaySeconds)")
            
        }
        
        // If we have enough data points, start the analysis
        if dataPointsHue.count == (seconds * framePerSecond) {
            if sampleCount > 0 && heartRateSum > 0 {
                let heartRate = heartRateSum / Float(sampleCount)
                sendEvent(withName: kDidFinishDetection, body: ["heartRate": heartRate, "sampleCount": sampleCount])
            }
            stopDetection()
        }
        
        // Unlock the image buffer
        CVPixelBufferUnlockBaseAddress(cvimgRef, CVPixelBufferLockFlags(rawValue: 0))
    }
}
