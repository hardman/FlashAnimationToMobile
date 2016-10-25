//
//  FlashViewNode.h
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "FlashViewCommon.h"
#import "FlashViewTool.h"

@interface FlashViewBaseNode: NSObject
-(void) updateToIndex:(NSInteger) index;
-(void) trigerEventWithIndex:(NSInteger)index delegate:(id<FlashViewDelegate>)delegate;
-(void) onReady;
-(void) onClean;
@end

//帧数据
@interface FlashViewFrameNode : FlashViewBaseNode
//帧index
@property (nonatomic, unsafe_unretained) int frameIndex;
//是否为空帧
@property (nonatomic, unsafe_unretained) BOOL isEmpty;
//是否补间动画
@property (nonatomic, unsafe_unretained) BOOL isTween;
//关键帧共多少帧
@property (nonatomic, unsafe_unretained) int duration;
//帧上的图片名称
@property (nonatomic, copy) NSString *imageName;

//位置
@property (nonatomic, unsafe_unretained) float x;
@property (nonatomic, unsafe_unretained) float y;
//缩放
@property (nonatomic, unsafe_unretained) float scaleX;
@property (nonatomic, unsafe_unretained) float scaleY;
//旋转
@property (nonatomic, unsafe_unretained) float skewX;
@property (nonatomic, unsafe_unretained) float skewY;
//事件
@property (nonatomic, copy) NSString *mark;
//透明度
@property (nonatomic, unsafe_unretained) float alpha;
//颜色叠加
@property (nonatomic, unsafe_unretained) char r;
@property (nonatomic, unsafe_unretained) char g;
@property (nonatomic, unsafe_unretained) char b;
@property (nonatomic, unsafe_unretained) char a;
@end

//层数据
@interface FlashViewLayerNode : FlashViewBaseNode
//配置
@property (nonatomic, weak) FlashViewTool *tool;
//当前层显示的图片名字
@property (nonatomic, copy) NSString *imageName;
//关键帧
@property (nonatomic,readonly, strong) NSArray<FlashViewFrameNode *> *keyFrames;
//添加关键帧
-(void) addKeyFrame:(FlashViewFrameNode *)keyFrame;
//layer的上下层次
@property (nonatomic, unsafe_unretained) NSInteger index;
@end

//动画数据
@interface FlashViewAnimNode : FlashViewBaseNode
//动画名称
@property (nonatomic, copy) NSString *animName;
//动画帧数
@property (nonatomic, unsafe_unretained) NSInteger frameCount;
//层
@property (nonatomic, readonly, strong) NSArray<FlashViewLayerNode *> *layers;
//添加layer
-(void) addLayer:(FlashViewLayerNode *) layer;
@end

//所有动画数据
@interface FlashViewNode : FlashViewBaseNode
//帧率
@property (nonatomic, unsafe_unretained) NSUInteger frameRate;
//一帧所花时间
@property (nonatomic, unsafe_unretained) NSUInteger oneFrameDurationMs;
//动画数据
@property (nonatomic, readonly, strong) NSDictionary<NSString *, FlashViewAnimNode*> *anims;
//增加动画数据
-(void) addAnim:(FlashViewAnimNode *)anim;
@end

