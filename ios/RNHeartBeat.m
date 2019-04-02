//
//  RNHeartBeat.m
//  RNHeartBeat
//
//  Created by Loi Relia on 4/02/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(RNHeartBeat, NSObject)
+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

RCT_EXTERN_METHOD(startDetection:(NSNumber *)seconds framePerSecond: (NSNumber *)framePerSecond)
RCT_EXTERN_METHOD(stopDetection)

@end
