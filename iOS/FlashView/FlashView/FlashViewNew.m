//
//  FlashViewNew.m
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashViewNew.h"
#import "FlashViewImageCache.h"
#import "FlashViewNode.h"
#import "FlashViewDataReaderNew.h"
//表示文件是在Resource里还是在Document里
typedef enum : NSUInteger {
    FileTypeNone,
    FileTypeResource,
    FileTypeDocument,
} FlashFileType;

//表示动画描述文件是json还是二进制
typedef enum : NSUInteger {
    FileDataTypeNone,
    FileDataTypeJson,
    FileDataTypeBin,
} FlashFileDataType;

@implementation FlashViewNew{
    //flash动画文件名
    NSString *mFlashName;
    //如果动画在Document里面，该文件的目录名
    NSString *mFlashAnimDir;
    NSFileManager *mFileManager;
    NSBundle *mMainBundle;
    NSString *mWritablePath;
    FlashFileDataType mFileDataType;
    FlashFileType mFileType;
    
    //图片
    FlashViewImageCache *mImageCache;
    
    //数据
    FlashViewNode *mFlashViewNode;
    
    CADisplayLink *mDisplayLink;
    BOOL isPlaying;
    
    NSInteger mStartTimeMs;
    NSInteger mLastFrameTimeMs;
    
    NSUInteger mFromIndex;
    NSUInteger mToIndex;
    
    NSUInteger mLoopTimes;
    
    NSInteger mLastPlayIndex;
    NSTimeInterval mLastPlayTime;
    NSString *mPlayingAnimName;
}

//构造方法：flashName为flash文件名
-(instancetype) initWithFlashName:(NSString *)flashName{
    return [self initWithFlashName:flashName andAnimDir:FLASH_VIEW_DEFAULT_DIR_NAME];
}

//构造方法：animDir为目录名，若动画存储在document中，目录名有效，默认值为 flashAnims
-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir{
    if (self = [super init]) {
        mFlashName = flashName;
        mFlashAnimDir = animDir;
        if (![self innerInit]) {
            return nil;
        }
    }
    return self;
}

//私有初始化函数
-(BOOL) innerInit{
    mFileManager = [NSFileManager defaultManager];
    mMainBundle = [NSBundle mainBundle];
    //document根目录
    mWritablePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    
    //先查找是否存在flajson文件，不存在则查找flabin。都不存在则初始化错误。并且确定文件是在Resource中还是在Document中
    NSString * filePath = [mMainBundle pathForResource:[NSString stringWithFormat:@"%@.flajson", mFlashName] ofType:nil];
    if (!filePath) {
        filePath = [mMainBundle pathForResource:[NSString stringWithFormat:@"%@.flabin", mFlashName] ofType:nil];
        if (!filePath) {
            filePath = [NSString stringWithFormat:@"%@/%@/%@.flajson", mWritablePath, mFlashAnimDir, mFlashName];
            if ([mFileManager fileExistsAtPath:filePath]) {
                mFileType = FileTypeDocument;
                mFileDataType = FileDataTypeJson;
            }else{
                filePath = [NSString stringWithFormat:@"%@/%@/%@.flabin", mWritablePath, mFlashAnimDir, mFlashName];
                if ([mFileManager fileExistsAtPath:filePath]) {
                    mFileType = FileTypeDocument;
                    mFileDataType = FileDataTypeBin;
                }
            }
        }else{
            mFileType = FileTypeResource;
            mFileDataType = FileDataTypeBin;
        }
    }else{
        mFileType = FileTypeResource;
        mFileDataType = FileDataTypeJson;
    }
    
    if (mFileType == FileTypeNone) {
        NSLog(@"FlashView init error file %@.flajson/.flabin is not exist", mFlashName);
        return NO;
    }
    
    //读取并解析数据
    if (mFileDataType == FileDataTypeJson) {
        if (![self parseJsonFile]) {
            NSLog(@"FlashView init error file %@.flajson is not json format", mFlashName);
            return NO;
        }
    }else{
        if (![self parseBinFile]) {
            NSLog(@"FlashView init error file %@.flabin is not valid", mFlashName);
            return NO;
        }
    }
    
    return YES;
}

//以二进制方式读取文件数据
-(NSData *)readData{
    NSData *data = nil;
    switch (mFileType) {
        case FileTypeDocument:
            data = [NSData dataWithContentsOfFile:[NSString stringWithFormat:@"%@/%@/%@%@", mWritablePath, mFlashAnimDir, mFlashName, mFileDataType == FileDataTypeJson ? @".flajson" : @".flabin"]];
            break;
        case FileTypeResource:
            data = [NSData dataWithContentsOfFile:[mMainBundle pathForResource:[NSString stringWithFormat:@"%@%@", mFlashName, mFileDataType == FileDataTypeJson ? @".flajson" : @".flabin"] ofType:nil]];
            break;
        default:
            break;
    }
    return data;
}

//读取json文件数据
-(NSDictionary *)readJson{
    NSDictionary *ret = nil;
    NSData *data = [self readData];
    if(data){
        NSError *jsonErr;
        ret = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&jsonErr];
        if (jsonErr) {
            NSLog(@"json 解析失败！%@", jsonErr);
            return nil;
        }
        return ret;
    }
    return nil;
}

//根据flash数据文件中得到的图片名字，读取真正的图片。
-(UIImage *)readImage:(NSString *)path{
    switch (mFileType) {
        case FileTypeDocument:
            return [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@/%@/%@/%@", mWritablePath, mFlashAnimDir, mFlashName, path]];
        case FileTypeResource:
            return [UIImage imageWithContentsOfFile:[mMainBundle pathForResource:path ofType:nil]];
        default:
            break;
    }
    return nil;
}

-(BOOL) parseJsonFile{
    NSDictionary *jsonDict = [self readJson];
    if (!jsonDict) {
        NSLog(@"未读取到json文件");
        return NO;
    }
    
    //帧率
    NSInteger frameRate = [jsonDict[@"frameRate"] integerValue];
    NSInteger oneFrameTime = 1000 / frameRate;
    
    if (!mFlashViewNode) {
        mFlashViewNode = [[FlashViewNode alloc] init];
        mFlashViewNode.oneFrameDurationMs = oneFrameTime;
        mFlashViewNode.frameRate = frameRate;
    }
    
    //图片
    for (NSString *texName in jsonDict[@"textures"]) {
        if (!mImageCache) {
            mImageCache = [FlashViewImageCache cache];
        }
        [mImageCache addImage:[self readImage:texName] withName:texName];
    }
    
    //读取动画
    for (NSDictionary *anim in jsonDict[@"anims"]) {
        FlashViewAnimNode *animNode = [[FlashViewAnimNode alloc] init];
        animNode.animName = anim[@"animName"];
        animNode.frameCount = [anim[@"frameMaxNum"] integerValue];
        [mFlashViewNode addAnim:animNode];
        for (int i = 0; i < [anim[@"layers"] length]; i++) {
            NSDictionary *layer = anim[@"layers"][i];
            FlashViewLayerNode *layerNode = [[FlashViewLayerNode alloc] init];
            layerNode.index = i;
            [animNode addLayer:layerNode baseView:self];
            for (NSDictionary *keyFrame in layer[@"frames"]) {
                FlashViewFrameNode *frameNode = [[FlashViewFrameNode alloc] init];
                [layerNode addKeyFrame:frameNode];
                frameNode.frameIndex = [keyFrame[@"frameIndex"] intValue];
                frameNode.isEmpty = [keyFrame[@"isEmpty"] boolValue];
                if (!frameNode.isEmpty) {
                    frameNode.isTween = [keyFrame[@"isTween"] boolValue];
                    frameNode.duration = [keyFrame[@"duration"] intValue];
                    frameNode.imageName = keyFrame[@"texName"];
                    frameNode.x = [keyFrame[@"x"] floatValue];
                    frameNode.y = [keyFrame[@"y"] floatValue];
                    frameNode.scaleX = [keyFrame[@"scaleX"] floatValue];
                    frameNode.scaleY = [keyFrame[@"scaleY"] floatValue];
                    frameNode.skewX = [keyFrame[@"skewX"] floatValue];
                    frameNode.skewY = [keyFrame[@"skewY"] floatValue];
                    frameNode.mark = keyFrame[@"mark"];
                    frameNode.alpha = [keyFrame[@"alpha"] floatValue];
                    frameNode.r = [keyFrame[@"color"][@"r"] charValue];
                    frameNode.g = [keyFrame[@"color"][@"g"] charValue];
                    frameNode.b = [keyFrame[@"color"][@"b"] charValue];
                    frameNode.a = [keyFrame[@"color"][@"a"] charValue];
                }
            }
        }
    }
    
    return YES;
}

-(BOOL) parseBinFile{
    NSData *binData = [self readData];
    if (!binData) {
        NSLog(@"读取二进制描述文件失败");
        return NO;
    }
    
    FlashViewDataReaderNew *dataReader = [[FlashViewDataReaderNew alloc] init];
    NSInteger frameRate = [dataReader readUShort];
    NSInteger oneFrameTime = 1000 / frameRate;
    
    if (!mFlashViewNode) {
        mFlashViewNode = [[FlashViewNode alloc] init];
        mFlashViewNode.oneFrameDurationMs = oneFrameTime;
        mFlashViewNode.frameRate = frameRate;
    }
    
    NSString *imagePath = nil;
    switch (mFileType) {
        case FileTypeDocument:
            imagePath = [NSString stringWithFormat:@"%@/%@/%@", mWritablePath, mFlashAnimDir, mFlashName];
            break;
        case FileTypeResource:
            imagePath = [mMainBundle bundlePath];
            break;
        default:
            break;
    }
    [FlashViewImageCache cache].imagePath = imagePath;
    
    NSInteger imageCount = [dataReader readUShort];
    NSMutableArray *imageNames = [[NSMutableArray alloc] init];
    for (int i = 0; i < imageCount; i++) {
        NSString *texName = [dataReader readNSString];
        if (!mImageCache) {
            mImageCache = [FlashViewImageCache cache];
        }
        [mImageCache addImage:[self readImage:texName] withName:texName];
        [imageNames addObject:texName];
    }
    
    NSInteger animCount = [dataReader readUShort];
    for (int j = 0; j < animCount; j++) {
        FlashViewAnimNode *animNode = [[FlashViewAnimNode alloc] init];
        animNode.animName = [dataReader readNSString];
        animNode.frameCount = [dataReader readUShort];
        [mFlashViewNode addAnim:animNode];
        NSInteger layerCount = [dataReader readUShort];
        for (int k = 0; k < layerCount; k++) {
            FlashViewLayerNode *layerNode = [[FlashViewLayerNode alloc] init];
            layerNode.index = k;
            [animNode addLayer:layerNode baseView:self];
            NSInteger keyFrameCount = [dataReader readUShort];
            for (int l = 0; l < keyFrameCount; l++) {
                FlashViewFrameNode *frameNode = [[FlashViewFrameNode alloc] init];
                [layerNode addKeyFrame:frameNode];
                BOOL isEmpty = [dataReader readBool];
                frameNode.isEmpty = isEmpty;
                frameNode.frameIndex = [dataReader readUShort];
                if (!isEmpty) {
                    frameNode.duration = [dataReader readUShort];
                    frameNode.isTween = [dataReader readBool];
                    frameNode.imageName = imageNames[[dataReader readUShort]];
                    frameNode.mark = [dataReader readNSString];
                    frameNode.alpha = [dataReader readUChar];
                    frameNode.r = [dataReader readUChar];
                    frameNode.g = [dataReader readUChar];
                    frameNode.b = [dataReader readUChar];
                    frameNode.a = [dataReader readUChar];
                    frameNode.scaleX = [dataReader readFloat];
                    frameNode.scaleY = [dataReader readFloat];
                    frameNode.skewX = [dataReader readFloat];
                    frameNode.skewY = [dataReader readFloat];
                    frameNode.x = [dataReader readFloat];
                    frameNode.y = [dataReader readFloat];
                }
            }
        }
    }
    return YES;
}

-(void)updateToFrameIndex:(NSInteger)frameIndex{
    if (!isPlaying) {
        NSLog(@"updateToFrameIndex当前没有正在播放的动画");
        return;
    }
    FlashViewAnimNode *animNode = mFlashViewNode.anims[mPlayingAnimName];
    [animNode updateToIndex:frameIndex];
}

-(void)trigerEventWithIndex:(NSInteger)frameIndex{
    if (!isPlaying) {
        NSLog(@"trigerEventWithIndex当前没有正在播放的动画");
        return;
    }
    FlashViewAnimNode *animNode = mFlashViewNode.anims[mPlayingAnimName];
    [animNode trigerEventWithIndex:frameIndex];
}

-(NSInteger) currentTimeMs{
    return [NSDate date].timeIntervalSince1970;
}

-(FlashViewAnimNode *) currAnimNode{
    if (isPlaying) {
        return mFlashViewNode.anims[mPlayingAnimName];
    }
    return nil;
}

//触发事件
-(void) triggerEventWithCurrTime:(NSTimeInterval) currTime{
    if (!isPlaying || mLastPlayIndex < 0) {
        return;
    }
    
    //从上次update之后，过了几帧，每一帧都要检查是否有事件
    NSInteger passedFrames = floor((currTime - mLastPlayTime) / mFlashViewNode.oneFrameDurationMs);
    for (NSInteger i = 0; i < passedFrames; i++) {
        NSInteger frameIndex = mLastPlayIndex + 1 + i;
        if (frameIndex > mToIndex) {
            frameIndex = frameIndex - mToIndex + mFromIndex;
        }
        [self trigerEventWithIndex:frameIndex];
    }
}

-(void) updateAnim:(CADisplayLink *)displayLink{
    NSTimeInterval currTime = self.currentTimeMs;
    NSTimeInterval passedTime = currTime - mStartTimeMs;
    NSTimeInterval passedCount = passedTime / mFlashViewNode.oneFrameDurationMs;
    NSUInteger animLen = mToIndex - mFromIndex + 1;
    NSUInteger currIndex = (NSUInteger)passedCount % animLen;
    
    //播放
    if (currIndex != mLastPlayIndex) {
        [self updateToFrameIndex:currIndex];
    }
    
    //触发事件
    [self triggerEventWithCurrTime:currTime];
    
    //重置状态
    mLastPlayIndex = currIndex;
    //向前对齐
    if (passedCount != (NSUInteger) passedCount) {
        currTime = ceil(passedCount) * mFlashViewNode.oneFrameDurationMs;
    }
    mLastPlayTime = currTime;
}

-(CADisplayLink *)displayLink{
    if (!mDisplayLink) {
        mDisplayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateAnim:)];
    }
    return mDisplayLink;
}

-(void) playAnimWithName:(NSString *) animName fromIndex:(NSUInteger) fromIndex toIndex:(NSUInteger) toIndex loopTimes:(NSUInteger) loopTimes{
    if (isPlaying) {
        [self stopAnim];
    }
    mFromIndex = fromIndex;
    mToIndex = toIndex;
    mLoopTimes = loopTimes;
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
    mStartTimeMs = self.currentTimeMs;
}

-(void) stopAnim{
    isPlaying = NO;
    mPlayingAnimName = nil;
    mStartTimeMs = 0;
    mFromIndex = 0;
    mToIndex = 0;
    mLoopTimes = 0;
    mLastPlayIndex = -1;
    [self.displayLink invalidate];
}


@end
