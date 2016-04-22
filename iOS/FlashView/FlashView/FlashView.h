/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

/***
 典型用法：
 
 -(void) initUI{
 FlashView *flashView = [[FlashView alloc] initWithFlashName:@"bieshu" andAnimDir:nil];
 flashView.frame = self.view.frame;// CGRectMake(100, 100, 200, 500);
 flashView.backgroundColor = [UIColor clearColor];
 [self.view addSubview:flashView];
 [flashView play:@"bieshu" loopTimes:FOREVER fromIndex:20 toIndex:30];
 
 //[self performSelector:@selector(loadCar:) withObject:flashView afterDelay:3];
 }
 
 -(void) reload:(FlashView *)flashView{
 [flashView reload:@"heiniao" andAnimDir:nil];
 [flashView play:@"atk" loopTimes:FOREVER];
 [self performSelector:@selector(loadCar:) withObject:flashView afterDelay:3];
 }
 
 -(void) loadCar:(FlashView *)flashView{
 [flashView reload:@"testDB" andAnimDir:nil];
 [flashView play:@"applanbo" loopTimes:FOREVER];
 [self performSelector:@selector(reload:) withObject:flashView afterDelay:3];
 }
 ***/

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

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

@interface FlashView : UIView

//文件是否存在
+(BOOL) isAnimExist:(NSString *)flashName;

@property (nonatomic, weak) id<FlashViewDelegate> delegate;//事件回调代理
@property (nonatomic, copy) FlashUIntCallback onEventBlock;//事件回调block
@property (nonatomic, unsafe_unretained) FlashViewRunMode runMode;//运行模式，在主线程还是，后台线程运行
@property (nonatomic, unsafe_unretained) BOOL isInitOk;//是否解析动画并存储对应的图片成功

@property (nonatomic, unsafe_unretained) FlashViewUpdateMode updateMode;

-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir;

-(instancetype) initWithFlashName:(NSString *)flashName;

//设置缩放类型
-(void) setScaleMode:(ScaleMode)mode andDesignResolution:(CGSize)resolution;

//直接设置缩放比例数值
-(void) setScaleWithX:(CGFloat)x y:(CGFloat) y isDesignResolutionEffect:(BOOL)isDREffect;

//获取动画名称
-(NSArray *)animNames;

-(NSDictionary *)images;

//播放动画，animName即为flash中anims文件夹内的动画名称
-(void) play:(NSString *)animName loopTimes:(NSUInteger) times;

-(void) play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger) from;

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

