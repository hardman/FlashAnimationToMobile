//
//  FlashViewNew.h
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "FlashViewCommon.h"

@interface FlashViewNew : UIControl
//构造器
-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir;
//构造器
-(instancetype) initWithFlashName:(NSString *)flashName;
//事件回调代理
@property (nonatomic, weak) id<FlashViewDelegate> delegate;
//事件回调block
@property (nonatomic, copy) FlashUIntCallback onEventBlock;
//是否解析动画并存储对应的图片成功
@property (nonatomic, unsafe_unretained) BOOL isInitOk;

//是否启用隐式动画
-(void) setUseImplicitAnim:(BOOL) isUseImplicitAnim;

//设置缩放类型
-(void) setScaleMode:(ScaleMode)mode andDesignResolution:(CGSize)resolution;

//直接设置缩放比例数值
-(void) setScaleWithX:(CGFloat)x y:(CGFloat) y isDesignResolutionEffect:(BOOL)isDREffect;

//获取动画名称
-(NSArray *)animNames;

//播放动画，animName即为flash中anims文件夹内的动画名称
-(void) play:(NSString *)animName loopTimes:(NSUInteger) times;

//播放动画，animName即为flash中anims文件夹内的动画名称，fromIndex为起始帧
-(void) play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger) from;

//播放动画，animName即为flash中anims文件夹内的动画名称，fromIndex为起始帧，toIndex为结束帧
-(void) play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger) from toIndex:(NSInteger) to;

//像图片一样显示动画的某一帧内容
-(void) stopAtFrameIndex:(NSInteger) frameIndex animName:(NSString *) animName;

//设置循环次数
-(void) setLoopTimes:(NSInteger) times;

//停止动画
-(void) stop;

//暂停
-(void) pause;

//恢复播放
-(void) resume;

//替换某一个动画元件的图片
-(void) replaceImage:(NSString *)texName image:(UIImage *)image;

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName andAnimDir:(NSString *)animDir;
@end
