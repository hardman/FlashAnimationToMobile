//
//  ViewController.m
//  FlashView
//
//  Created by kaso on 14/12/15.
//  Copyright © 2015年 kaso. All rights reserved.
//

#import "ViewController.h"
#import "FlashView.h"

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    [self initUI];
}

-(void) initUI{
    FlashView *flashView = [[FlashView alloc] initWithFlashName:@"bieshu" andAnimDir:nil];
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

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
