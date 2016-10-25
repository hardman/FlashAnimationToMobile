//
//  FlashViewTool.h
//  FlashView
//
//  Created by kaso on 24/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface FlashViewTool : NSObject
//baseview
@property (nonatomic, weak) UIView *baseView;
//当前动画路径
@property (nonatomic, copy) NSString *imagePath;
//缩放
@property (nonatomic, unsafe_unretained) CGPoint scale;
//是否开启隐式动画
@property (nonatomic, unsafe_unretained) BOOL isUseImplicitAnim;

-(void) addImage:(UIImage *)image withName:(NSString *)name;

-(void) replaceImage:(UIImage *)image withName:(NSString *)name;

-(UIImage *) imageWithName:(NSString *) name;

@end
