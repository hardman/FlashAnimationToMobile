//
//  TestFlashViewController.h
//  FlashView
//
//  Created by kaso on 1/9/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>

typedef enum : NSUInteger {
    TestTypeOldAnim,
    TestTypeNewAnim,
} TestType;

@interface TestFlashViewController : UIViewController

@property (nonatomic, unsafe_unretained) TestType testType;

@end
