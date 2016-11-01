//
//  TestFlashViewDownloadViewController.m
//  FlashView
//
//  Created by kaso on 1/9/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "TestFlashViewDownloadViewController.h"
#import "FlashViewDownloader.h"
#import "ZipArchive.h"
#import "FlashViewNew.h"

/**
 *  测试FlashView下载
 *  为了能够下载，记得在 Info.plist 中 添加 “App Transport Security Settings － Allow Arbitrary Loads＝YES”
 *  我这里下载使用的 URLSession，解压使用的 ZipArchive。你可以根据你的情况，使用自己的下载器和解压工具。
 */

@interface TestFlashViewDownloadViewController ()<FlashViewDownloadDelegate>
@property (nonatomic, weak) UIActivityIndicatorView *loadingView;
@property (nonatomic, weak) UILabel *loadingLabel;
@property (nonatomic, unsafe_unretained) NSInteger nextIndex;
@end

@implementation TestFlashViewDownloadViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor whiteColor];
    [self testDownloader];
}

/**
 *  我自己使用的下载器。把 描述文件abcd.flabin 和 图片文件夹 abcd/ 打包成一个 abcd.zip 然后放在服务器上
 *  比如这个Demo，我是把 heiniao.flabin 和 /heiniao 这两个文件打包成 heiniao.zip 放到github上。
 *  将zip下载并解压到~/Library/Cache下的指定目录(已有默认值，无需设置；如果想修改，则需要播放的时候同事指定该目录)中。
 *  然后就可以直接使用 FlashView 播放动画了。
 */
-(void) testDownloader{
    FlashViewDownloader *downloader = [[FlashViewDownloader alloc] init];
    downloader.delegate = self;
    __weak TestFlashViewDownloadViewController *weakCtl = self;
    [downloader downloadAnimFileWithUrl:@"https://github.com/hardman/OutLinkImages/raw/master/FlashAnimationToMobile/zips/heiniao.zip" saveFileName:@"heiniao.zip" animFlaName:@"heiniao" version:@"1" downType:ZIP percentCb:^(float per) {
        //do nothing
    } completeCb:^(BOOL succ) {
        if (succ) {
            [weakCtl playFlashAnim];
            NSLog(@"动画下载成功并播放");
        }else{
            NSLog(@"下载动画后播放失败");
        }
    }];
}

-(void) playFlashAnim{
    FlashViewNew *flashView = [[FlashViewNew alloc] initWithFlashName:@"heiniao"];
    [self.view addSubview:flashView];
    [flashView play:flashView.animNames[0] loopTimes:FlashViewLoopTimeOnce];
    __weak FlashViewNew *weakFlashView = flashView;
    __weak TestFlashViewDownloadViewController *weakCtl = self;
    flashView.onEventBlock = ^(FlashViewEvent evt, id data){
        if (evt == FlashViewEventStop) {
            weakCtl.nextIndex++;
            if (weakCtl.nextIndex >= flashView.animNames.count) {
                weakCtl.nextIndex = 0;
            }
            [weakFlashView play:weakFlashView.animNames[weakCtl.nextIndex] loopTimes:FlashViewLoopTimeOnce];
        }
    };
}

#pragma mark FlashViewDownloadDelegate 自定义下载文件的函数，可以自己选择下载和解压文件的方法
-(void)downloadFlashFileWithUrl:(NSString *)url outFile:(NSString *)outFile percentCb:(DownloadPercentCallback)percentCb completeCb:(DownloadCompleteCallback)completeCb{
    NSLog(@"开始下载:%@", url);
    
    //显示loadingview
    [self setLoadingViewShow:YES];
    
    NSURLSession *session = [NSURLSession sharedSession];
    NSURLSessionDownloadTask *downloadTask = [session downloadTaskWithURL:[NSURL URLWithString:url] completionHandler:^(NSURL * _Nullable location, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (error) {
            NSLog(@"下载失败：%@, url=%@", error, url);
            completeCb(NO);
        }else{
            NSError *moveItemError = nil;
            [[NSFileManager defaultManager] moveItemAtURL:location toURL:[NSURL fileURLWithPath:outFile] error:&moveItemError];
            if (moveItemError) {
                NSLog(@"文件下载成功后无法从%@移动到%@", location, outFile);
                completeCb(NO);
            }else{
                NSLog(@"下载成功");
                completeCb(YES);
            }
        }
        //关闭loadingview
        dispatch_async(dispatch_get_main_queue(), ^{
            [self setLoadingViewShow:NO];
        });
    }];
    [downloadTask resume];
}

-(BOOL)unzipDownloadedFlashFile:(NSString *)zipFile toDir:(NSString *)dir{
    ZipArchive *za = [[ZipArchive alloc] init];
    if ([za UnzipOpenFile:zipFile]) {
        BOOL ret = [za UnzipFileTo:dir overWrite:YES];
        if (!ret) {
            [za UnzipCloseFile];
            return NO;
        }
        return YES;
    }
    return NO;
}

// 主要是上面3个函数，下面是loading view
-(UIActivityIndicatorView *)loadingView{
    if (!_loadingView) {
        UIActivityIndicatorView *loadingView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
        loadingView.center = self.view.center;
        [self.view addSubview:loadingView];
        _loadingView = loadingView;
    }
    return _loadingView;
}

-(UILabel *)loadingLabel{
    if (!_loadingLabel) {
        UILabel *loadingLabel = [[UILabel alloc] init];
        loadingLabel.font = [UIFont systemFontOfSize:13];
        loadingLabel.text = @"下载动画文件中...";
        [loadingLabel sizeToFit];
        [self.view addSubview:loadingLabel];
        loadingLabel.center = CGPointMake(self.view.center.x, self.view.center.y + 25);
        _loadingLabel = loadingLabel;
    }
    return _loadingLabel;
}

-(void) setLoadingViewShow:(BOOL) show{
    if (show) {
        self.loadingLabel.hidden = NO;
        self.loadingView.hidden = NO;
        [self.loadingView startAnimating];
        [self.view bringSubviewToFront:self.loadingView];
    }else{
        self.loadingLabel.hidden = YES;
        self.loadingView.hidden = YES;
        [self.loadingView stopAnimating];
    }
}

@end
