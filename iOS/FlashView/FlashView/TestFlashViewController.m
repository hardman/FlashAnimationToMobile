//
//  TestFlashViewController.m
//  FlashView
//
//  Created by kaso on 1/9/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "TestFlashViewController.h"
#import "FlashView.h"
#import "FlashViewNew.h"
#import <objc/runtime.h>

/**
 *  测试FlashView
 */

static char *keynamefortest = 0;

@interface TestFlashViewController ()

@property (nonatomic, strong) FlashView *flashView;

@property (nonatomic, strong) FlashViewNew *flashViewNew;

@property (nonatomic, unsafe_unretained) NSInteger currAnimIndex;

@property (nonatomic, copy) NSString * currAnim;

@property (nonatomic, unsafe_unretained) NSInteger loopTimes;

@end

@implementation TestFlashViewController
- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor whiteColor];
    self.loopTimes = FlashViewLoopTimeOnce;
    [self buildTestFlashView];
}

-(FlashView *)flashView{
    if (!_flashView) {
        _flashView = [[FlashView alloc] init];
        [self.view addSubview:_flashView];
    }
    return _flashView;
}

-(FlashViewNew *)flashViewNew{
    if (!_flashViewNew) {
        _flashViewNew = [[FlashViewNew alloc] init];
        _flashViewNew.designScreenOrientation = FlashViewScreenOrientationVer;
        _flashViewNew.screenOrientation = FlashViewScreenOrientationVer;
        _flashViewNew.animPosMask = FlashViewAnimPosMaskVerCenter | FlashViewAnimPosMaskHorCenter;
        [self.view addSubview:_flashViewNew];
    }
    return _flashViewNew;
}

-(void) buildTestFlashView{
    id flashAnimsData = @[].mutableCopy;
    NSString *parent = [[NSBundle mainBundle] bundlePath];
    NSArray *paths = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:parent error:nil];
    for (NSString *path in paths) {
        if ([path hasSuffix:@".flabin"]) {
            NSString *prefix = [path substringToIndex:path.length - @".flabin".length];
            [flashAnimsData addObject:prefix];
        }
        if ([path hasSuffix:@".flajson"]) {
            NSString *prefix = [path substringToIndex:path.length - @".flajson".length];
            [flashAnimsData addObject:prefix];
        }
    }
    
    CGSize screenSize = [UIScreen mainScreen].bounds.size;
    UIScrollView *sc = [[UIScrollView alloc] initWithFrame:CGRectMake(0, 0, screenSize.width, screenSize.height)];
    sc.tag = 123456;
    sc.bounces = YES;
    sc.backgroundColor = [UIColor greenColor];
    UIView *scContainer = [[UIView alloc] init];
    [self.view addSubview:sc];
    [sc addSubview:scContainer];
    
    UIView *lastView = nil;
    for (id data in flashAnimsData) {
        UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, lastView ? lastView.frame.origin.y + lastView.frame.size.height: 0, screenSize.width, 50)];
        [sc addSubview:view];
        
        objc_setAssociatedObject(view, &keynamefortest, data, OBJC_ASSOCIATION_COPY_NONATOMIC);
        
        [view addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(clickOnAnimForTest:)]];
        
        UILabel *nameLabel = [UILabel new];
        [view addSubview:nameLabel];
        nameLabel.text = data;
        nameLabel.frame = CGRectMake(15, 20, 0, 0);
        [nameLabel sizeToFit];
        
        UIView *lineView = [[UIView alloc] initWithFrame:CGRectMake(0, 50, screenSize.width, 1)];
        lineView.backgroundColor = [UIColor blackColor];
        [view addSubview:lineView];
        lastView = view;
    }
    
    sc.contentSize = CGSizeMake(screenSize.width, lastView.frame.origin.y + 50);
}


-(void)clickOnAnimForTest:(UIGestureRecognizer*)gesture{
    NSString *name = objc_getAssociatedObject(gesture.view, &keynamefortest);
    switch (self.testType) {
        case TestTypeNewAnim:{
            self.flashViewNew.userInteractionEnabled = YES;
            if (!self.currAnim) {
                if (!self.flashViewNew.superview) {
                    [self.view addSubview:self.flashViewNew];
                }
                if(![self.flashViewNew reload:name]){
                    NSLog(@"reload error for name %@", name);
                    return;
                }
                self.currAnim = name;
            }
            NSArray *anims = self.flashViewNew.animNames;
            if (anims.count == 0) {
                return;
            }
            [self.flashViewNew play:anims[self.currAnimIndex] loopTimes:self.loopTimes];
            
            __weak TestFlashViewController *weakCtl = self;
            __weak FlashViewNew *weakFlashView = self.flashViewNew;
            self.flashViewNew.onEventBlock = ^(FlashViewEvent evt, id data){
                if (evt == FlashViewEventStop) {
                    if (weakCtl.currAnimIndex >= anims.count - 1) {
                        [weakFlashView removeFromSuperview];
                        weakCtl.currAnimIndex = 0;
                        weakCtl.currAnim = nil;
                    }else{
                        weakCtl.currAnimIndex++;
                        [weakCtl clickOnAnimForTest:nil];
                    }
                }
            };
        }
            break;
        case TestTypeOldAnim:{
            self.flashView.userInteractionEnabled = YES;
            if (!self.currAnim) {
                if (!self.flashView.superview) {
                    [self.view addSubview:self.flashView];
                }
                [self.flashView reload:name];
                self.currAnim = name;
            }
            NSArray *anims = self.flashView.animNames;
            if (anims.count == 0) {
                return;
            }
            [self.flashView play:anims[self.currAnimIndex] loopTimes:self.loopTimes];
            
            __weak TestFlashViewController *weakCtl = self;
            __weak FlashView *weakFlashView = self.flashView;
            self.flashView.onEventBlock = ^(FlashViewEvent evt, id data){
                if (evt == FlashViewEventStop) {
                    if (weakCtl.currAnimIndex >= anims.count - 1) {
                        [weakFlashView removeFromSuperview];
                        weakCtl.currAnimIndex = 0;
                        weakCtl.currAnim = nil;
                    }else{
                        weakCtl.currAnimIndex++;
                        [weakCtl clickOnAnimForTest:nil];
                    }
                }
            };
        }
            break;
    }
}

-(void)willMoveToParentViewController:(UIViewController *)parent{
    if (!parent) {
        [self.class cancelPreviousPerformRequestsWithTarget:self];
    }
}

-(void)dealloc{
    NSLog(@"dealloc for TestFlashViewController");
}

#pragma mark 屏幕旋转
- (BOOL) shouldAutorotate{
    return YES;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
    return (UIInterfaceOrientation)[UIDevice currentDevice].orientation;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskAll;
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
    if (UIInterfaceOrientationIsPortrait(toInterfaceOrientation)) {
        self.flashViewNew.screenOrientation = FlashViewScreenOrientationVer;
    }else if(UIInterfaceOrientationIsLandscape(toInterfaceOrientation)){
        self.flashViewNew.screenOrientation = FlashViewScreenOrientationHor;
    }
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
}

-(void)viewWillAppear:(BOOL)animated{
    UIInterfaceOrientation toInterfaceOrientation = (UIInterfaceOrientation)[UIDevice currentDevice].orientation;
    if (UIInterfaceOrientationIsPortrait(toInterfaceOrientation)) {
        self.flashViewNew.screenOrientation = FlashViewScreenOrientationVer;
    }else if(UIInterfaceOrientationIsLandscape(toInterfaceOrientation)){
        self.flashViewNew.screenOrientation = FlashViewScreenOrientationHor;
    }
}

@end
