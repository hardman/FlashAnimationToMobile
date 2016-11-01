/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <Foundation/Foundation.h>

//屏幕尺寸
//当前屏幕尺寸
#define FLASH_VIEW_SCREEN_SIZE [UIScreen mainScreen].bounds.size
//横屏屏幕尺寸
#define FLASH_VIEW_SCREEN_SIZE_HOR \
CGSizeMake(\
(FLASH_VIEW_SCREEN_SIZE.width > FLASH_VIEW_SCREEN_SIZE.height ? FLASH_VIEW_SCREEN_SIZE.width : FLASH_VIEW_SCREEN_SIZE.height) \
, \
(FLASH_VIEW_SCREEN_SIZE.width < FLASH_VIEW_SCREEN_SIZE.height ? FLASH_VIEW_SCREEN_SIZE.width : FLASH_VIEW_SCREEN_SIZE.height) \
)
//竖屏屏幕尺寸
#define FLASH_VIEW_SCREEN_SIZE_VER CGSizeMake(FLASH_VIEW_SCREEN_SIZE_HOR.height, FLASH_VIEW_SCREEN_SIZE_HOR.width)

//默认动画目录
#define FLASH_VIEW_DEFAULT_DIR_NAME @"flashAnims"
//默认动画zip目录
#define FLASH_VIEW_DEFAULT_ZIP_NAME @"flashAnimZip"

//表示文件是在Resource里还是在Document里
typedef enum : NSInteger {
    FlashViewFileTypeNone,
    FlashViewFileTypeResource,
    FlashViewFileTypeDocument,
} FlashViewFileType;

//表示动画描述文件是json还是二进制
typedef enum : NSInteger {
    FlashViewFileDataTypeNone,
    FlashViewFileDataTypeJson,
    FlashViewFileDataTypeBin,
} FlashViewFileDataType;

//动画帧更新模式（deprecated）
typedef enum : NSUInteger {
    FlashViewUpdateModeRealtimeTime,//按照真实时间更新，不会卡顿，可能会掉帧。（建议）
    FlashViewUpdateModeEveryFrame,//按照每一帧的顺序播放，不会掉帧，可能会卡顿。
} FlashViewUpdateMode;

//动画循环次数
typedef enum : NSUInteger {
    FlashViewLoopTimeOnce = 1,//循环1次
    FlashViewLoopTimeForever = 0,//无限循环
} FlashViewLoopTime;

//动画事件，监听动画开始结束，帧事件等等。
typedef enum NSUInteger{
    FlashViewEventStart,//开始，数据为空
    FlashViewEventFrame,//每一帧，数据为当前帧数
    FlashViewEventOneLoopEnd,//最后一帧，数据为当前循环次数
    FlashViewEventStop,//结束，数据为空
    FlashViewEventMark,//flash中添加的事件，数据为：@{@"mark":@"事件名称", @"frameIndex":第几帧, @"imageName": @"图片名", @"layerIndex":@"从下往上数，第几层，从1开始"}
} FlashViewEvent;

//缩放模式，类似于UIView的 contentMode
typedef enum : NSUInteger {
   FlashViewScaleModeWidthFit,//宽度充满frame，保持宽高比
   FlashViewScaleModeHeightFit,//高度充满frame，保持宽高比
   FlashViewScaleModeRespective,//宽度高度分别充满frame，不一定保持宽高比
   FlashViewScaleModeDefault,//默认，不处理
}FlashViewScaleMode;

//动画运行线程
typedef enum : NSUInteger {
    FlashViewRunModeBackgroundThread,//动画在后台线程运行（建议）
    FlashViewRunModeMainThread,//动画在主线程运行
} FlashViewRunMode;

//Flash动画的位置
typedef enum : NSUInteger {
    //横向
    FlashViewAnimPosMaskLeft = 1,//靠左，
    FlashViewAnimPosMaskRight = 1 << 1,//靠右
    FlashViewAnimPosMaskHorCenter = 1 << 5,//横向居中
    
    //竖向
    FlashViewAnimPosMaskTop = 1 << 2,//靠上
    FlashViewAnimPosMaskBottom = 1 << 3,//靠下
    FlashViewAnimPosMaskVerCenter = 1 << 4,//竖向居中
} FlashViewAnimPosMask;

//设置屏幕方向，用于根据设置的屏幕方向（而不是系统的）调整动画的位置。
typedef enum : NSUInteger {
    FlashViewScreenOrientationNone,//不关心横竖屏
    FlashViewScreenOrientationHor,//横屏
    FlashViewScreenOrientationVer,//竖屏
} FlashViewScreenOrientation;

//事件回调代理
@protocol FlashViewDelegate <NSObject>

-(void)onEvent:(FlashViewEvent) evt data:(id)d;

@end

//事件回调block
typedef void (^FlashUIntCallback)(FlashViewEvent, id);

