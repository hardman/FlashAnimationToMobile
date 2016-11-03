/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <UIKit/UIKit.h>
#import "FlashViewCommon.h"
#import "FlashViewTool.h"

//数据基类
@interface FlashViewBaseNode: NSObject
//刷新到第几帧
-(void) updateToIndex:(NSInteger) index lastIndex:(NSInteger) lastIndex;
//当因为某些原因，需要改变显示元素属性（位置大小形状等）变化时调用
-(void) updateTransform;
//触发回调事件
-(void) trigerEventWithIndex:(NSInteger)index delegate:(id<FlashViewDelegate>)delegate;
//数据初始化完成
-(void) onReady;
//清空数据
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
@property (nonatomic, unsafe_unretained) unsigned char r;
@property (nonatomic, unsafe_unretained) unsigned char g;
@property (nonatomic, unsafe_unretained) unsigned char b;
@property (nonatomic, unsafe_unretained) unsigned char a;

@property (nonatomic, strong) NSValue *transformValue;
@end

//层数据：绘制工作和主要逻辑都在这里
@interface FlashViewLayerNode : FlashViewBaseNode
//层所属的动画名
@property (nonatomic, copy) NSString *animName;
//配置
@property (nonatomic, weak) FlashViewTool *tool;
//当前层显示的图片名字
@property (nonatomic, copy) NSString *imageName;
//当前图片的尺寸
@property (nonatomic, unsafe_unretained) CGSize imageSize;
//关键帧
@property (nonatomic,readonly, strong) NSArray<FlashViewFrameNode *> *keyFrames;
//添加关键帧
-(void) addKeyFrame:(FlashViewFrameNode *)keyFrame;
//layer的上下层次
@property (nonatomic, unsafe_unretained) NSInteger index;
//隐藏layer并归零
-(void) resetLayer;
//移除可显示的layers
-(void) removeLayers;
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
//移除可显示的layers
-(void) removeLayers;
//重置layer
-(void) resetLayer;
@end

//所有动画数据
@interface FlashViewNode : FlashViewBaseNode
//帧率
@property (nonatomic, unsafe_unretained) NSUInteger frameRate;
//一帧所花时间
@property (nonatomic, unsafe_unretained) uint64_t oneFrameDurationMs;
//动画数据
@property (nonatomic, readonly, strong) NSDictionary<NSString *, FlashViewAnimNode*> *anims;
//增加动画数据
-(void) addAnim:(FlashViewAnimNode *)anim;
@end

