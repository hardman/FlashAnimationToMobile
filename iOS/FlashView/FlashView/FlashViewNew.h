/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <UIKit/UIKit.h>
#import "FlashViewCommon.h"

@interface FlashViewNew : UIControl
//构造器
-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir scaleMode:(FlashViewScaleMode)scaleMode designResolution:(CGSize)resolution designScreenOrientation:(FlashViewScreenOrientation) designScreenOrientation currScreenOrientation:(FlashViewScreenOrientation) currScreenOrientation;
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

//运行模式，在主线程还是，后台线程运行
@property (nonatomic, unsafe_unretained) FlashViewRunMode runMode;

//隐式动画时间倍数，默认为1，适当增大此值，能够令动画变得更加流畅。但是，如果值设置过大，可能会令动画变形(丢失细节)。请根据具体情况设置。
@property (nonatomic, unsafe_unretained) CGFloat implicitAnimDurationScale;

// ==== 横屏适配 ====
/**
 *  下面4个变量用于横屏适配。
 *  未开启横屏适配时，动画会根据当前屏幕方向，调整View的尺寸，并根据设置的ScaleMode 进行自动动画适配。
 *  开启横屏适配后，动画不根据系统屏幕方向自动适配。通过下列4个变量进行适配。其中：
 *  designScreenOrientation：表示是否开启横屏适配，设为FlashViewScreenOrientationNone表示不开启。其他两个枚举表示美术设计时是按照横屏还是竖屏。
 *  screenOrientation: 当前屏幕方向，自己根据需求设置。如果为 FlashViewScreenOrientationNone，表示不适配。
 *  animPosMask：表示动画主体位置，动画主要展示的位置。如果是普通动画一般都是HorCenter|VerCenter，如果是屏幕上方的提示条幅一般是 VerTop|HorCenter。
 *  animOffset：表示动画偏移位置，上面3个变量最终影响的就是此值。程序最终会根据这个值进行最后的位置适配。所以特别情况也可以直接设置此值。
 */
//动画位置
@property (nonatomic, unsafe_unretained) FlashViewAnimPosMask animPosMask;

//屏幕方向
@property (nonatomic, unsafe_unretained) FlashViewScreenOrientation screenOrientation;

//设计屏幕方向，若不设置，表示不调整动画位置，根据系统屏幕方向计算位置。
@property (nonatomic, unsafe_unretained) FlashViewScreenOrientation designScreenOrientation;

//动画偏移量
@property (nonatomic, unsafe_unretained) CGPoint animOffset;
// ==== 横屏适配 ====

//直接设置动画缩放比例数值，isDREffect表示是否乘以自动适配的factor
-(void) setScaleWithX:(CGFloat)x y:(CGFloat) y isDesignResolutionEffect:(BOOL)isDREffect;

//是否启用隐式动画
-(void) setUseImplicitAnim:(BOOL) isUseImplicitAnim;

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
-(BOOL) reload:(NSString *)flashName;

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName andAnimDir:(NSString *)animDir;

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName andAnimDir:(NSString *)animDir scaleMode:(FlashViewScaleMode)scaleMode designResolution:(CGSize)resolution;

//判断动画是否存在
+(BOOL) isAnimExist:(NSString *)flashName;

//根据图片名获取动画图片
-(UIImage *) animImageWithName:(NSString *)name;
@end
