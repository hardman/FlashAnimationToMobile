/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <UIKit/UIKit.h>

@interface FlashViewTool : NSObject

//baseview
@property (nonatomic, weak) UIView *baseView;

//当前动画、图片的目录
@property (nonatomic, copy) NSString *imagePath;

//根据适配规则，计算出的缩放比例
@property (nonatomic, unsafe_unretained) CGPoint scale;

//是否开启隐式动画，默认关闭，如果打开，动画效果会比较丝滑，但是某些动画会变样，请根据具体情况使用。
//简单动画建议开启：如秀场
//复杂动画建议关闭：如人物动态(跑，跳，攻击，防御。。)
@property (nonatomic, unsafe_unretained) BOOL isUseImplicitAnim;

//增加图片
-(void) addImage:(UIImage *)image withName:(NSString *)name;

//替换图片
-(void) replaceImage:(UIImage *)image withName:(NSString *)name;

//获取图片
-(UIImage *) imageWithName:(NSString *) name;

@end
