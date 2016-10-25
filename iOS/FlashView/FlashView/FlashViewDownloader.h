/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <Foundation/Foundation.h>
typedef void (^DownloadPercentCallback) (float per);
typedef void (^DownloadCompleteCallback) (BOOL succ);
//文件下载，当flash文件存储在服务器中时，提供下载功能，支持将文件打包为zip格式。
//delegate用于令用户自己继承并实现http下载。
@protocol FlashViewDownloadDelegate <NSObject>

//下载文件
-(void) downloadFlashFileWithUrl:(NSString *)url outFile:(NSString *)outFile
                       percentCb:(DownloadPercentCallback) percentCb
                      completeCb:(DownloadCompleteCallback) completeCb
;

//解压下载的Zip文件
-(BOOL) unzipDownloadedFlashFile:(NSString *)zipFile toDir:(NSString *)dir;

@end

typedef enum : NSUInteger {
    IMAGE,//图片
    DESCRIPTION,//描述文件，flajson/flabin
    ZIP//压缩文件
} FlashDownloadType;//文件下载类型

//下载器，包含下载文件，并移动（解压）到指定目录中。
@interface FlashViewDownloader : NSObject
//真正的下载文件代理
@property (nonatomic, strong) id<FlashViewDownloadDelegate> delegate;
//默认下载动画存储目录父目录的名字
@property (nonatomic, copy) NSString *animDirName;
//默认下载动画存储压缩文件的目录名字
@property (nonatomic, copy) NSString *animZipDirName;

//移除指定的动画，用户修改或升级
-(void) removeAnimWithAnimName:(NSString *) animName;

//下载指定文件，并移动（解压）到指定目录中
-(BOOL) downloadAnimFileWithUrl:(NSString *)url
                   saveFileName:(NSString *)saveFileName
                    animFlaName:(NSString *)animFlaName
                        version:(NSString *)version
                       downType:(FlashDownloadType) downType
                      percentCb:(DownloadPercentCallback) percentCb
                     completeCb:(DownloadCompleteCallback) completeCb;

@end

// 版本号管理，将版本号用NSDictionary["动画名": "版本号"]存储，并保存到文件中。
// 每次下载动画时需传入版本号，并同存储的版本号进行对比，如果不一致，需要重新下载动画文件，并更新版本号
@interface FlashViewVersion : NSObject
//版本号信息存储路径
@property (nonatomic, readonly, copy) NSString *versionPath;
//缓存数据
@property (nonatomic, readonly, strong) NSMutableDictionary *versionData;

//单例
+(instancetype) getInstance;

//更新文件，将缓存存入versionPath中
-(void) synchronize;

//更新缓存的版本号
-(void) updateAnimName:(NSString *)animName version:(NSString *)version;

//获取当前对应animName的版本号
-(NSString *) versionOfAnimName:(NSString *)animName;

@end

