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
@property (nonatomic, weak) UIButton *testMutipleElementAnimBtn;
@property (nonatomic, weak) UIButton *testFlashViewBtn;
@property (nonatomic, weak) UIButton *testFlashViewNewBtn;
@property (nonatomic, weak) UIButton *testFlashViewDownloadBtn;
@end

@implementation ViewController

-(UIButton *)testFlashViewBtn{
    if (!_testFlashViewBtn) {
        _testFlashViewBtn = [self createTestBtnWithTitle:@"测试旧FlashView" y:self.view.center.y - 35];
    }
    return _testFlashViewBtn;
}

-(UIButton *)testFlashViewNewBtn{
    if (!_testFlashViewNewBtn) {
        _testFlashViewNewBtn = [self createTestBtnWithTitle:@"测试新FlashView" y:self.view.center.y - 105];
    }
    return _testFlashViewNewBtn;
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
        TestFlashViewController *testViewCtl = [[TestFlashViewController alloc] init];
        testViewCtl.testType = TestTypeOldAnim;
        [self.navigationController pushViewController:testViewCtl animated:YES];
    } else if([btn isEqual:self.testFlashViewNewBtn]){
        TestFlashViewController *testViewCtl = [[TestFlashViewController alloc] init];
        testViewCtl.testType = TestTypeNewAnim;
        [self.navigationController pushViewController:testViewCtl animated:YES];
    } else if([btn isEqual:self.testFlashViewDownloadBtn]) {
        [self.navigationController pushViewController:[[TestFlashViewDownloadViewController alloc] init] animated:YES];
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self testFlashViewBtn];
    [self testFlashViewNewBtn];
    [self testFlashViewDownloadBtn];
}

@end
