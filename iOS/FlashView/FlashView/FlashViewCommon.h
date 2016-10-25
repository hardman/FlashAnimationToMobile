/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <Foundation/Foundation.h>

//默认动画目录
#define FLASH_VIEW_DEFAULT_DIR_NAME @"flashAnims"
//默认动画zip目录
#define FLASH_VIEW_DEFAULT_ZIP_NAME @"flashAnimZip"

//动画帧更新模式
typedef enum : NSUInteger {
    FlashViewUpdateModeRealtimeTime,//按照真实时间更新，不会卡顿，可能会掉帧。（建议）
    FlashViewUpdateModeEveryFrame,//按照每一帧的顺序播放，不会掉帧，可能会卡顿。
} FlashViewUpdateMode;

typedef enum : NSUInteger {
    FlashLoopTimeOnce = 1,//循环1次
    FlashLoopTimeForever = 0,//无限循环
} FlashLoopTime;//动画循环次数

typedef enum NSUInteger{
    FlashViewEventStart,//开始
    FlashViewEventFrame,//每一帧
    FlashViewEventOneLoopEnd,//最后一帧
    FlashViewEventStop,//结束
    FlashViewEventMark,//flash中添加的事件
} FlashViewEvent;//动画事件，监听动画开始结束，帧事件等等。

typedef enum : NSUInteger {
    ScaleModeWidthFit,//宽度充满frame，保持宽高比
    ScaleModeHeightFit,//高度充满frame，保持宽高比
    ScaleModeRespective,//宽度高度分别充满frame，不一定保持宽高比
    ScaleModeDefault,//默认，不处理
} ScaleMode;//缩放模式，类似于UIView的 contentMode

//动画运行线程
typedef enum : NSUInteger {
    FlashViewRunModeBackgroundThread,//动画在后台线程运行（建议）
    FlashViewRunModeMainThread,//动画在主线程运行
} FlashViewRunMode;

//事件回调代理
@protocol FlashViewDelegate <NSObject>

-(void)onEvent:(FlashViewEvent) evt data:(id)d;

@end

//事件回调block
typedef void (^FlashUIntCallback)(FlashViewEvent, id);

