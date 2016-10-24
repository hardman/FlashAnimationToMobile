//
//  FlashViewNode.h
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface FlashViewBaseNode: NSObject
-(void) updateToIndex:(NSInteger) index;
@end

//帧数据
@interface FlashViewFrameNode : FlashViewBaseNode
@property (nonatomic, unsafe_unretained) int frameIndex;
@property (nonatomic, unsafe_unretained) BOOL isEmpty;
@property (nonatomic, unsafe_unretained) BOOL isTween;
@property (nonatomic, unsafe_unretained) int duration;
@property (nonatomic, copy) NSString *imageName;

@property (nonatomic, unsafe_unretained) float x;
@property (nonatomic, unsafe_unretained) float y;
@property (nonatomic, unsafe_unretained) float scaleX;
@property (nonatomic, unsafe_unretained) float scaleY;
@property (nonatomic, unsafe_unretained) float skewX;
@property (nonatomic, unsafe_unretained) float skewY;
@property (nonatomic, copy) NSString *mark;
@property (nonatomic, unsafe_unretained) float alpha;
@property (nonatomic, unsafe_unretained) char r;
@property (nonatomic, unsafe_unretained) char g;
@property (nonatomic, unsafe_unretained) char b;
@property (nonatomic, unsafe_unretained) char a;

@property (nonatomic, weak) FlashViewFrameNode *nextFrameNode;
@end

//层数据
@interface FlashViewLayerNode : FlashViewBaseNode
//当前显示的imageName
@property (nonatomic, copy) NSString *imageName;
//层view
@property (nonatomic, readonly, strong) UIView *layerView;
@property (nonatomic,readonly, strong) NSArray<FlashViewFrameNode *> *keyFrames;
-(void) addKeyFrame:(FlashViewFrameNode *)keyFrame;
@end

//动画数据
@interface FlashViewAnimNode : FlashViewBaseNode
@property (nonatomic, copy) NSString *animName;
@property (nonatomic, unsafe_unretained) NSInteger frameCount;
@property (nonatomic, readonly, strong) NSArray<FlashViewLayerNode *> *layers;
-(void) addLayer:(FlashViewLayerNode *) layer;
@end

//所有动画数据
@interface FlashViewNode : FlashViewBaseNode
@property (nonatomic, unsafe_unretained) NSUInteger frameRate;
@property (nonatomic, unsafe_unretained) NSUInteger oneFrameDurationMs;
@property (nonatomic, readonly, strong) NSDictionary<NSString *, FlashViewAnimNode*> *anims;
-(void) addAnim:(FlashViewAnimNode *)anim;
@end

