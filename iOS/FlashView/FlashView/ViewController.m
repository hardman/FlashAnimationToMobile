//
//  ViewController.m
//  FlashView
//
//  Created by kaso on 14/12/15.
//  Copyright © 2015年 kaso. All rights reserved.
//

#import "ViewController.h"
#import "FlashView.h"
#import "FlashViewDownloader.h"
#import "Zip/ZipArchive.h"
#import "TestFlashViewController.h"
#import "TestFlashViewDownloadViewController.h"

@interface ViewController ()
@property (nonatomic, weak) UIButton *testFlashViewBtn;
@property (nonatomic, weak) UIButton *testFlashViewDownloadBtn;
@end

@implementation ViewController

-(UIButton *)testFlashViewBtn{
    if (!_testFlashViewBtn) {
        _testFlashViewBtn = [self createTestBtnWithTitle:@"测试FlashView" y:self.view.center.y - 35];
    }
    return _testFlashViewBtn;
}

-(UIButton *)testFlashViewDownloadBtn{
    if (!_testFlashViewDownloadBtn) {
        _testFlashViewDownloadBtn = [self createTestBtnWithTitle:@"测试FlashView下载" y:self.view.center.y + 35];
    }
    return _testFlashViewDownloadBtn;
}

-(UIButton *)createTestBtnWithTitle:(NSString *)title y:(CGFloat) y{
    UIButton *testBtn = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 200, 50)];
    testBtn.backgroundColor = [UIColor grayColor];
    [testBtn setTitle:title forState:UIControlStateNormal];
    [testBtn setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [testBtn setTitleColor:[UIColor whiteColor] forState:UIControlStateHighlighted];
    testBtn.center = CGPointMake(self.view.center.x, y);
    [testBtn addTarget:self action:@selector(onClickTest:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:testBtn];
    return testBtn;
}

-(void) onClickTest:(UIButton *) btn{
    if ([btn isEqual:self.testFlashViewBtn]) {
        [self.navigationController pushViewController:[[TestFlashViewController alloc] init] animated:YES];
    }else{
        [self.navigationController pushViewController:[[TestFlashViewDownloadViewController alloc] init] animated:YES];
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self testFlashViewBtn];
    [self testFlashViewDownloadBtn];
}

@end
