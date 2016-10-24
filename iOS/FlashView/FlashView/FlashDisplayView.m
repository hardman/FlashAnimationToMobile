//
//  FlashDisplayView.m
//  FlashView
//
//  Created by kaso on 24/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashDisplayView.h"

@interface FlashDisplayView ()
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, unsafe_unretained) BOOL isPlaying;

@property (nonatomic, unsafe_unretained) NSInteger startTimeMs;
@property (nonatomic, unsafe_unretained) NSInteger lastFrameTimeMs;

@property (nonatomic, unsafe_unretained) NSUInteger fromIndex;
@property (nonatomic, unsafe_unretained) NSUInteger toIndex;

@property (nonatomic, unsafe_unretained) NSUInteger loopTimes;

@property (nonatomic, unsafe_unretained) NSInteger lastPlayIndex;
@property (nonatomic, unsafe_unretained) NSTimeInterval lastPlayTime;
@property (nonatomic, copy) NSString *playingAnimName;
@end

@implementation FlashDisplayView

-(void)updateToFrameIndex:(NSInteger)frameIndex{
    if (!self.isPlaying) {
        NSLog(@"updateToFrameIndex当前没有正在播放的动画");
        return;
    }
    FlashViewAnimNode *animNode = self.flashViewNode.anims[self.playingAnimName];
}

-(void)trigerEventWithIndex:(NSInteger)frameIndex{
    if (!self.isPlaying) {
        NSLog(@"trigerEventWithIndex当前没有正在播放的动画");
        return;
    }
    FlashViewAnimNode *animNode = self.anims[self.playingAnimName];
    [animNode trigerEventWithIndex:frameIndex];
}

-(NSInteger) currentTimeMs{
    return [NSDate date].timeIntervalSince1970;
}

-(FlashViewAnimNode *) currAnimNode{
    if (self.isPlaying) {
        return self.flashViewNode.anims[self.playingAnimName];
    }
    return nil;
}

//触发事件
-(void) triggerEventWithCurrTime:(NSTimeInterval) currTime{
    if (!self.isPlaying || self.lastPlayIndex < 0) {
        return;
    }
    
    //从上次update之后，过了几帧，每一帧都要检查是否有事件
    NSInteger passedFrames = floor((currTime - self.lastPlayTime) / self.oneFrameDurationMs);
    for (NSInteger i = 0; i < passedFrames; i++) {
        NSInteger frameIndex = self.lastPlayIndex + 1 + i;
        if (frameIndex > self.toIndex) {
            frameIndex = frameIndex - self.toIndex + self.fromIndex;
        }
        [self trigerEventWithIndex:frameIndex];
    }
}

-(void) updateAnim:(CADisplayLink *)displayLink{
    NSTimeInterval currTime = self.currentTimeMs;
    NSTimeInterval passedTime = currTime - self.startTimeMs;
    NSTimeInterval passedCount = passedTime / self.oneFrameDurationMs;
    NSUInteger animLen = self.toIndex - self.fromIndex + 1;
    NSUInteger currIndex = (NSUInteger)passedCount % animLen;
    
    //播放
    if (currIndex != self.lastPlayIndex) {
        [self updateToFrameIndex:currIndex];
    }
    
    //触发事件
    [self triggerEventWithCurrTime:currTime];
    
    //重置状态
    self.lastPlayIndex = currIndex;
    //向前对齐
    if (passedCount != (NSUInteger) passedCount) {
        currTime = ceil(passedCount) * self.oneFrameDurationMs;
    }
    self.lastPlayTime = currTime;
}

-(CADisplayLink *)displayLink{
    if (!_displayLink) {
        _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateAnim:)];
    }
    return _displayLink;
}

-(void) playAnimWithName:(NSString *) animName fromIndex:(NSUInteger) fromIndex toIndex:(NSUInteger) toIndex loopTimes:(NSUInteger) loopTimes{
    if (self.isPlaying) {
        [self stopAnim];
    }
    self.fromIndex = fromIndex;
    self.toIndex = toIndex;
    self.loopTimes = loopTimes;
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
    self.startTimeMs = self.currentTimeMs;
}

-(void) stopAnim{
    self.isPlaying = NO;
    self.playingAnimName = nil;
    self.startTimeMs = 0;
    self.fromIndex = 0;
    self.toIndex = 0;
    self.loopTimes = 0;
    self.lastPlayIndex = -1;
    [self.displayLink invalidate];
}


@end
