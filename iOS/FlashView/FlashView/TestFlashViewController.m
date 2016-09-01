//
//  TestFlashViewController.m
//  FlashView
//
//  Created by kaso on 1/9/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "TestFlashViewController.h"
#import "FlashView.h"

/**
 *  测试FlashView
 */

@interface TestFlashViewController ()

@end

@implementation TestFlashViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor whiteColor];
    [self testFlashView];
}

-(void) testFlashView{
    FlashView *flashView = [[FlashView alloc] initWithFlashName:@"bieshu"];
    flashView.frame = self.view.frame;// CGRectMake(100, 100, 200, 500);
    flashView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:flashView];
    [flashView play:@"bieshu" loopTimes:FlashLoopTimeForever fromIndex:20 toIndex:30];
    
    [self performSelector:@selector(loadCar:) withObject:flashView afterDelay:3];
}

-(void) reload:(FlashView *)flashView{
    [flashView reload:@"heiniao" andAnimDir:nil];
    [flashView play:@"atk" loopTimes:FlashLoopTimeForever];
    [self performSelector:@selector(loadCar:) withObject:flashView afterDelay:3];
}

-(void) loadCar:(FlashView *)flashView{
    [flashView reload:@"testDB" andAnimDir:nil];
    [flashView play:@"applanbo" loopTimes:FlashLoopTimeForever];
    [self performSelector:@selector(reload:) withObject:flashView afterDelay:3];
}


@end
