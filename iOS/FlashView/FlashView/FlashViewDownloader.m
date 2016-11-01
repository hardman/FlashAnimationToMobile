/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import "FlashViewDownloader.h"

//默认动画目录
#define FLASH_VIEW_DEFAULT_DIR_NAME @"flashAnims"
//默认动画zip目录
#define FLASH_VIEW_DEFAULT_ZIP_NAME @"flashAnimZip"

//工具类
@interface FlashViewHelper : NSObject

@end

@implementation FlashViewHelper

+(NSString *)documentPath{
    static NSString *sPath = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    });
    return sPath;
}

//在指定目录创建文件夹
+(NSString *) createDirInDocument:(NSString *)dir root:(NSString *)root fileManager:(NSFileManager *) fileManager{
    NSString *fullPath = [NSString stringWithFormat:@"%@/%@", root, dir];
    BOOL isDir;
    if ([fileManager fileExistsAtPath:dir isDirectory:&isDir]) {
        if (isDir) {
            return fullPath;
        }else{
            [fileManager removeItemAtPath:fullPath error:nil];
        }
    }
    if([fileManager createDirectoryAtPath:fullPath withIntermediateDirectories:YES attributes:nil error:nil]){
        return fullPath;
    }
    return nil;
}

@end


/**
 * 有可能动画文件是存储在服务器上的，此时我们需要一个下载动画文件的帮助类
 * 这个动画下载类，能够下载3种类型的文件：描述文件（flajson/flabin），图片（png），zip（包含描述文件和png图片的压缩文件）
 */
@implementation FlashViewDownloader{
    NSFileManager *mFileManager;
    NSString *mDocumentPath;
}

-(instancetype)init{
    if(self = [super init]){
        mFileManager = [NSFileManager defaultManager];
        mDocumentPath = FlashViewHelper.documentPath;
        self.animDirName = FLASH_VIEW_DEFAULT_DIR_NAME;
        self.animZipDirName = FLASH_VIEW_DEFAULT_ZIP_NAME;
    }
    return self;
}

-(NSString *) createDirInDocument:(NSString *)dir{
    return [FlashViewHelper createDirInDocument:dir root:mDocumentPath fileManager:mFileManager];
}

//删除指定动画文件名相关的所有文件
-(void) removeAnimWithAnimName:(NSString *) animName{
    NSString *needRemovePath = nil;
    NSError *error;
    //remove zip
    needRemovePath = [NSString stringWithFormat:@"%@/%@/%@.zip", mDocumentPath, self.animZipDirName, animName];
    [mFileManager removeItemAtPath:needRemovePath error:&error];
    //remove animDir
    needRemovePath = [NSString stringWithFormat:@"%@/%@/%@", mDocumentPath, self.animDirName, animName];
    [mFileManager removeItemAtPath:needRemovePath error:&error];
    //remove .flajson
    needRemovePath = [NSString stringWithFormat:@"%@/%@/%@.flajson", mDocumentPath, self.animDirName, animName];
    [mFileManager removeItemAtPath:needRemovePath error:&error];
    //remove .flabin
    needRemovePath = [NSString stringWithFormat:@"%@/%@/%@.flabin", mDocumentPath, self.animDirName, animName];
    [mFileManager removeItemAtPath:needRemovePath error:&error];
}

//解压后的动画的文件是否存在
-(BOOL) isAnimExistWithAnimName:(NSString *)animName animDirName:(NSString *)animDirName{
    NSString *flajsonPath = [NSString stringWithFormat:@"%@/%@/%@.flajson", mDocumentPath, animDirName, animName];
    if ([mFileManager fileExistsAtPath:flajsonPath]) {
        return YES;
    }else{
        NSString *flabinPath = [NSString stringWithFormat:@"%@/%@/%@.flabin", mDocumentPath, animDirName, animName];
        return [mFileManager fileExistsAtPath:flabinPath];
    }
}

/**
 *  下载文件并解压，保存至指定目录
 *
 *  @param url          需要下载的文件地址
 *  @param saveFileName 下载后保存到本地的文件名
 *  @param animFlaName  动画fla的名字
 *  @param version      版本号，做版本升级用
 *  @param downType     下载类型，一般用zip
 *  @param percentCb    下载进度
 *  @param completeCb   完成回调
 *
 *  @return 返回 是：正确，否：发生错误
 */
-(BOOL) downloadAnimFileWithUrl:(NSString *)url
                   saveFileName:(NSString *)saveFileName animFlaName:(NSString *)animFlaName
                        version:(NSString *)version
                       downType:(FlashDownloadType) downType
                      percentCb:(DownloadPercentCallback) percentCb
                     completeCb:(DownloadCompleteCallback) completeCb{
    NSString *outFile = nil;
    
    switch (downType){
        case IMAGE:
        {
            NSString * imageDirFile = [self createDirInDocument: [NSString stringWithFormat:@"%@/%@", self.animDirName, animFlaName]];
            if(imageDirFile != nil){
                outFile = [NSString stringWithFormat:@"%@/%@", imageDirFile, saveFileName];
            }
        }
            break;
        case DESCRIPTION:
            
        {
            NSString * desDirFile = [self createDirInDocument: self.animDirName];
            if(desDirFile != nil){
                outFile = [NSString stringWithFormat:@"%@/%@", desDirFile, saveFileName];
            }
        }
        case ZIP:
        {
            NSString *zipDirFile = [self createDirInDocument:self.animZipDirName];
            if (zipDirFile != nil) {
                outFile = [NSString stringWithFormat:@"%@/%@", zipDirFile, saveFileName];
            }
        }
            break;
    }
    
    if (!outFile) {
        NSLog(@"[E]outFile is nil when download flash anim");
        return FALSE;
    }
    
    //比较版本号，只有两个版本号一致才不下载文件
    FlashViewVersion *flashViewVersion = [FlashViewVersion getInstance];
    NSString *currentStoreVersion = [flashViewVersion versionOfAnimName:animFlaName];
    if(!currentStoreVersion || ![currentStoreVersion isEqualToString:version]){
        [self removeAnimWithAnimName:animFlaName];
        
        [flashViewVersion updateAnimName:animFlaName version:version];
        [flashViewVersion synchronize];
    }
    
    BOOL isDir;
    if ([mFileManager fileExistsAtPath:outFile isDirectory:&isDir]) {
        if (isDir) {
            [mFileManager removeItemAtPath:outFile error:nil];
        }else{
            //检查.flabin/.flajson文件是否存在
            if([self isAnimExistWithAnimName:animFlaName animDirName:self.animDirName]){
                dispatch_async(dispatch_get_main_queue(), ^{
                    completeCb(YES);
                });
            }else{
                NSString * desDirFile = [self createDirInDocument: self.animDirName];
                BOOL unzipRet = [self.delegate unzipDownloadedFlashFile:outFile toDir:desDirFile];
                dispatch_async(dispatch_get_main_queue(), ^{
                    completeCb(unzipRet);
                });
            }
            return YES;
        }
    }
    
    if(outFile != nil && self.delegate) {
        [self.delegate downloadFlashFileWithUrl:url outFile:outFile percentCb:^(float per) {
            percentCb(per);
        } completeCb:^(BOOL succ) {
            if (succ && (downType == ZIP)) {
                NSString * desDirFile = [self createDirInDocument: self.animDirName];
                BOOL unzipRet = [self.delegate unzipDownloadedFlashFile:outFile toDir:desDirFile];
                dispatch_async(dispatch_get_main_queue(), ^{
                    completeCb(unzipRet);
                });
            }else{
                dispatch_async(dispatch_get_main_queue(), ^{
                    completeCb(succ);
                });
            }
        }];
    }
    
    return YES;
}
@end

@interface FlashViewVersion()
@property (nonatomic, copy) NSString *versionPath;
@property (nonatomic, strong) NSMutableDictionary *versionData;
@property (nonatomic, weak) NSFileManager *fileManager;
@end

@implementation FlashViewVersion : NSObject

@synthesize versionData=_versionData;

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.fileManager = [NSFileManager defaultManager];
    }
    return self;
}

-(NSString *)versionPath{
    NSString *versionDir = [FlashViewHelper createDirInDocument:FLASH_VIEW_DEFAULT_ZIP_NAME root:FlashViewHelper.documentPath fileManager:self.fileManager];
    return [NSString stringWithFormat:@"%@/version", versionDir];
}

-(void) updateAnimName:(NSString *)animName version:(NSString *)version{
    if (!animName || !version) {
        return;
    }
    self.versionData[animName] = version;
}

-(NSString *) versionOfAnimName:(NSString *)animName{
    return self.versionData[animName];
}

-(void)setVersionData:(NSMutableDictionary *)versionData{
    _versionData = versionData;
}

-(NSMutableDictionary *)versionData{
    if (!_versionData) {
        NSData *data = [NSData dataWithContentsOfFile:self.versionPath];
        if (data) {
            _versionData = [[NSJSONSerialization JSONObjectWithData:data options:0 error:nil] mutableCopy];
        }else{
            _versionData = @{}.mutableCopy;
        }
    }
    return _versionData;
}

-(void) synchronize{
    __weak typeof(self) weakSelf = self;
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        @synchronized (weakSelf) {
            if (_versionData) {
                NSData *data = [NSJSONSerialization dataWithJSONObject:_versionData options:0 error:nil];
                [data writeToFile:weakSelf.versionPath atomically:YES];
            }
        }
    });
}

-(void)dealloc{
    [self synchronize];
}

+(instancetype) getInstance{
    static FlashViewVersion *sVersion = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sVersion = [[FlashViewVersion alloc] init];
    });
    return sVersion;
}

@end
