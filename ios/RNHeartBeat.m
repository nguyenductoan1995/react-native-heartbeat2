//
//  RNHeartBeat.m
//  RNHeartBeat
//
//  Created by Loi Relia on 4/02/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(RNHeartBeat, UIView)
+ (BOOL)requiresMainQueueSetup
{
    return FALSE;
}

RCT_EXTERN_METHOD(startDetection:(nonnull NSNumber *)seconds framePerSecond: (nonnull NSNumber *)framePerSecond)
RCT_EXTERN_METHOD(stopDetection)

@end

@interface RCT_EXTERN_MODULE(RNHeartBeatViewManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(enabled, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onReady, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onStart, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onStop, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onValueChanged, RCTDirectEventBlock)

+ (BOOL)requiresMainQueueSetup
{
    return FALSE;
}

@end
