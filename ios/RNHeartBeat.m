//
//  RNHeartBeat.m
//  RNHeartBeat
//
//  Created by Loi Relia on 4/02/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(RNHeartBeatViewManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(enabled, BOOL)
RCT_EXPORT_VIEW_PROPERTY(measureTime, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(framePerSecond, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(onReady, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onErrorOccured, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onStart, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onStop, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onFinish, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onValueChanged, RCTBubblingEventBlock)

+ (BOOL)requiresMainQueueSetup
{
    return FALSE;
}

@end
