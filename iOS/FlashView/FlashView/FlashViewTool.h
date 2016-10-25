/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <UIKit/UIKit.h>

@interface FlashViewTool : NSObject

//baseview
@property (nonatomic, weak) UIView *baseView;

//当前动画路径
@property (nonatomic, copy) NSString *imagePath;

//缩放
@property (nonatomic, unsafe_unretained) CGPoint scale;

//是否开启隐式动画，默认关闭，如果打开，动画效果会比较丝滑，但是某些动画会变样，请根据具体情况使用。
@property (nonatomic, unsafe_unretained) BOOL isUseImplicitAnim;

//增加图片
-(void) addImage:(UIImage *)image withName:(NSString *)name;

//替换图片
-(void) replaceImage:(UIImage *)image withName:(NSString *)name;

//获取图片
-(UIImage *) imageWithName:(NSString *) name;

//获取颜色叠加后的图片
-(UIImage *)colorOverlayImageWithColor:(UIColor *) color srcImgName:(NSString *)srcImgName;

//图片
+(UIImage *)imageWithColor:(UIColor *)color size:(CGSize)size;

@end
