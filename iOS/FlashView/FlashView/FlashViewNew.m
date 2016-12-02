/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import "FlashViewNew.h"
#import "FlashViewTool.h"
#import "FlashViewNode.h"
#import "FlashViewDataReader.h"

@interface FlashViewNew()<FlashViewDelegate>
@property (nonatomic, weak) UIView *animView;
@property (nonatomic, strong) FlashViewTool *tool;
@end
@implementation FlashViewNew{
    //flash动画文件名
    NSString *mFlashName;
    //如果动画在Document里面，该文件的目录名
    NSString *mFlashAnimDir;
    //系统文件管理
    NSFileManager *mFileManager;
    //main bundle
    NSBundle *mMainBundle;
    //可写目录
    NSString *mWritablePath;
    //数据格式：json还是bin
    FlashViewFileDataType mFileDataType;
    //文件位置：在Bundle（Resource）还是在Document中
    FlashViewFileType mFileType;
    
    //动画数据
    FlashViewNode *mFlashViewNode;
    
    //计时器
    CADisplayLink *mDisplayLink;
    //动画是否在播放
    BOOL isPlaying;
    
    //动画开始时间（毫秒）
    uint64_t mStartTimeMs;
    //动画上一帧播放时间（毫秒）
    uint64_t mLastFrameTimeMs;
    
    //上次更新LoopTime帧数
    int64_t mLastUpdateLoopTimeMs;
    
    //当前动画起始帧
    NSInteger mFromIndex;
    //当前动画结束帧
    NSInteger mToIndex;
    
    //当前动画已循环次数
    NSInteger mLoopTimes;
    //当前动画循环次数限制
    NSInteger mTotalLoopTimes;
    
    //当前动画上一次循环播放哪一帧
    NSInteger mLastPlayIndex;
    //当前播放的动画名称
    NSString *mPlayingAnimName;
    
    BOOL mIsInitOk;
}

@synthesize animOffset = _animOffset;

//构造方法：flashName为flash文件名
-(instancetype) initWithFlashName:(NSString *)flashName{
    return [self initWithFlashName:flashName andAnimDir:FLASH_VIEW_DEFAULT_DIR_NAME];
}

//构造方法：animDir为目录名，若动画存储在document中，目录名有效，默认值为 flashAnims
-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir{
    return [self initWithFlashName:flashName andAnimDir:animDir scaleMode:FlashViewScaleModeHeightFit designResolution:CGSizeMake(640, 1136) designScreenOrientation:FlashViewScreenOrientationVer currScreenOrientation:FlashViewScreenOrientationNone];
}

//构造方法：animDir为目录名，若动画存储在document中，目录名有效，默认值为 flashAnimsFlashViewScaleMode为适配缩放模式，resolution为设计分辨率 designScreenOrientation：设计屏幕方向，currScreenOrientation：当前屏幕方向
-(instancetype) initWithFlashName:(NSString *)flashName andAnimDir:(NSString *)animDir scaleMode:(FlashViewScaleMode)scaleMode designResolution:(CGSize)resolution designScreenOrientation:(FlashViewScreenOrientation) designScreenOrientation currScreenOrientation:(FlashViewScreenOrientation) currScreenOrientation{
    if (self = [self init]) {
        mFlashName = flashName;
        mFlashAnimDir = animDir;
        self.designScreenOrientation = designScreenOrientation;
        self.screenOrientation = currScreenOrientation;
        [self setScaleMode:scaleMode andDesignResolution:resolution];
        if (![self innerInit]) {
            return nil;
        }
    }
    return self;
}

- (instancetype)init{
    self = [super init];
    if (self) {
        self.animPosMask = FlashViewAnimPosMaskVerCenter | FlashViewAnimPosMaskHorCenter;
        self.implicitAnimDurationScale = 1;
    }
    return self;
}

//私有初始化函数
-(BOOL) innerInit{
    isPlaying = NO;
    mPlayingAnimName = nil;
    mStartTimeMs = 0;
    mFromIndex = 0;
    mToIndex = 0;
    mLoopTimes = 0;
    mTotalLoopTimes = 0;
    mLastPlayIndex = -1;
    mLastUpdateLoopTimeMs = -1;
    
    CGSize screenSize = [UIScreen mainScreen].bounds.size;
    switch (self.designScreenOrientation) {
        case FlashViewScreenOrientationHor:
            screenSize = FLASH_VIEW_SCREEN_SIZE_HOR;
            break;
        case FlashViewScreenOrientationVer:
            screenSize = FLASH_VIEW_SCREEN_SIZE_VER;
            break;
        default:
            break;
    }
    self.frame = CGRectMake(0, 0, screenSize.width, screenSize.height);
    
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
                mFileType = FlashViewFileTypeDocument;
                mFileDataType = FlashViewFileDataTypeJson;
            }else{
                filePath = [NSString stringWithFormat:@"%@/%@/%@.flabin", mWritablePath, mFlashAnimDir, mFlashName];
                if ([mFileManager fileExistsAtPath:filePath]) {
                    mFileType = FlashViewFileTypeDocument;
                    mFileDataType = FlashViewFileDataTypeBin;
                }
            }
        }else{
            mFileType = FlashViewFileTypeResource;
            mFileDataType = FlashViewFileDataTypeBin;
        }
    }else{
        mFileType = FlashViewFileTypeResource;
        mFileDataType = FlashViewFileDataTypeJson;
    }
    
    if (mFileType == FlashViewFileTypeNone) {
        NSLog(@"FlashView init error file %@.flajson/.flabin is not exist", mFlashName);
        return NO;
    }
    
    //读取并解析数据
    if (mFileDataType == FlashViewFileDataTypeJson) {
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
    
    mIsInitOk = YES;
    
    return YES;
}

-(BOOL)isInitOk{
    return mIsInitOk;
}

//获取配置
-(FlashViewTool *)tool{
    if (!_tool) {
        _tool = [[FlashViewTool alloc] init];
        _tool.baseView = self.animView;
    }
    return _tool;
}

-(UIView *)animView{
    if (!_animView) {
        UIView *animView = [[UIView alloc] init];
        animView.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:animView];
        //top
        [self addConstraint:[NSLayoutConstraint constraintWithItem:animView attribute:NSLayoutAttributeTop relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeTop multiplier:1 constant:0]];
        //bottom
        [self addConstraint:[NSLayoutConstraint constraintWithItem:animView attribute:NSLayoutAttributeBottom relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeBottom multiplier:1 constant:0]];
        //left
        [self addConstraint:[NSLayoutConstraint constraintWithItem:animView attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeLeft multiplier:1 constant:0]];
        //right
        [self addConstraint:[NSLayoutConstraint constraintWithItem:animView attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeRight multiplier:1 constant:0]];
        _animView = animView;
    }
    return _animView;
}

//是否启用隐式动画
-(void) setUseImplicitAnim:(BOOL) isUseImplicitAnim{
    self.tool.isUseImplicitAnim = isUseImplicitAnim;
}

//设置缩放类型
-(void) setScaleMode:(FlashViewScaleMode)mode andDesignResolution:(CGSize)resolution{
    UIScreen *mainScreen = [UIScreen mainScreen];
    CGSize screenSize = mainScreen.bounds.size;
    
    //如果设置了屏幕方向，表示通过计算animOff 来调整动画位置，需要保证屏幕尺寸不跟随系统变化
    switch (self.designScreenOrientation) {
        case FlashViewScreenOrientationHor:
            screenSize = FLASH_VIEW_SCREEN_SIZE_HOR;
            break;
        case FlashViewScreenOrientationVer:
            screenSize = FLASH_VIEW_SCREEN_SIZE_VER;
            break;
        default:
            break;
    }
    
    CGFloat wRate = screenSize.width / resolution.width;
    CGFloat hRate = screenSize.height / resolution.height;
    switch (mode) {
        case FlashViewScaleModeWidthFit:
            [self setScaleWithX:wRate y:wRate isDesignResolutionEffect:NO];
            break;
        case FlashViewScaleModeHeightFit:
            [self setScaleWithX:hRate y:hRate isDesignResolutionEffect:NO];
            break;
        case FlashViewScaleModeRespective:
            [self setScaleWithX:wRate y:hRate isDesignResolutionEffect:NO];
            break;
        case FlashViewScaleModeDefault:
            [self setScaleWithX:1 y:1 isDesignResolutionEffect:NO];
            break;
    }
}

//直接设置缩放比例数值
-(void) setScaleWithX:(CGFloat)x y:(CGFloat) y isDesignResolutionEffect:(BOOL)isDREffect{
    if (isDREffect) {
        self.tool.scale = CGPointMake(self.tool.scale.x * x, self.tool.scale.y * y);
    }else{
        self.tool.scale = CGPointMake(x, y);
    }
    
    if (self.isInitOk) {
        [mFlashViewNode updateTransform];
    }
}

//设置动画位置
-(void)setAnimPosMask:(FlashViewAnimPosMask)animPosMask{
    if (_animPosMask == animPosMask) {
        return;
    }
    _animPosMask = animPosMask;
    
    //计算偏移量
    [self calculateFactAnimOffset];
    
    if (self.isInitOk) {
        [mFlashViewNode updateTransform];
    }
}

-(void)setScreenOrientation:(FlashViewScreenOrientation)screenOrientation{
    if (_screenOrientation == screenOrientation) {
        return;
    }
    _screenOrientation = screenOrientation;
    
    //计算偏移量
    [self calculateFactAnimOffset];
    
    if (self.isInitOk) {
        [mFlashViewNode updateTransform];
    }
}

//直接设置AnimOffset
-(void)setAnimOffset:(CGPoint)animOffset{
    if (CGPointEqualToPoint(_animOffset, animOffset)) {
        return;
    }
    _animOffset = animOffset;
    self.tool.animOffset = animOffset;
    if (self.isInitOk) {
        [mFlashViewNode updateTransform];
    }
}

-(CGPoint)animOffset{
    return self.tool.animOffset;
}

//-(void)setFrame:(CGRect)frame{
//    [super setFrame:frame];
//    if (self.isInitOk) {
//        [self calculateFactAnimOffset];
//    }
//}

//根据 screenOrientation posMask 能够算出动画偏移量。
-(void) calculateFactAnimOffset{
    //屏幕方向变化了
    if (self.designScreenOrientation != FlashViewScreenOrientationNone && _screenOrientation != self.designScreenOrientation) {
        CGFloat animOffX = 0;
        CGFloat animOffY = 0;
        switch (_screenOrientation) {
                //横向变竖向
            case FlashViewScreenOrientationVer:{
                //计算posMask 横向
                if (self.animPosMask & FlashViewAnimPosMaskHorCenter) {
                    animOffX = -(FLASH_VIEW_SCREEN_SIZE_HOR.width / 2 - FLASH_VIEW_SCREEN_SIZE_VER.width / 2);
                }else if(self.animPosMask & FlashViewAnimPosMaskRight){
                    animOffX = -(FLASH_VIEW_SCREEN_SIZE_HOR.width - self.frame.size.width);
                }
                
                //计算posMask 竖向
                if (self.animPosMask & FlashViewAnimPosMaskVerCenter) {
                    animOffY = -(FLASH_VIEW_SCREEN_SIZE_HOR.height / 2 - FLASH_VIEW_SCREEN_SIZE_VER.height / 2);
                }else if(self.animPosMask & FlashViewAnimPosMaskBottom){
                    animOffY = FLASH_VIEW_SCREEN_SIZE_VER.height - self.frame.size.height;
                }
            }
                break;
                //竖向变横向
            case FlashViewScreenOrientationHor:{
                //计算posMask 横向
                if (self.animPosMask & FlashViewAnimPosMaskHorCenter) {
                    animOffX = FLASH_VIEW_SCREEN_SIZE_HOR.width / 2 - FLASH_VIEW_SCREEN_SIZE_VER.width / 2;
                }else if(self.animPosMask & FlashViewAnimPosMaskRight){
                    animOffX = FLASH_VIEW_SCREEN_SIZE_HOR.width - self.frame.size.width;
                }
                
                //计算posMask 竖向
                if (self.animPosMask & FlashViewAnimPosMaskVerCenter) {
                    animOffY = FLASH_VIEW_SCREEN_SIZE_HOR.height / 2 - FLASH_VIEW_SCREEN_SIZE_VER.height / 2;
                }else if(self.animPosMask & FlashViewAnimPosMaskBottom){
                    animOffY = -(FLASH_VIEW_SCREEN_SIZE_VER.height - self.frame.size.height);
                }
            }
                break;
            default:
                break;
        }
        self.tool.animOffset = CGPointMake(animOffX, animOffY);
    }else{
        self.tool.animOffset = _animOffset;
    }
}

//以二进制方式读取文件数据
-(NSData *)readData{
    NSData *data = nil;
    switch (mFileType) {
        case FlashViewFileTypeDocument:
            data = [NSData dataWithContentsOfFile:[NSString stringWithFormat:@"%@/%@/%@%@", mWritablePath, mFlashAnimDir, mFlashName, mFileDataType == FlashViewFileDataTypeJson ? @".flajson" : @".flabin"]];
            break;
        case FlashViewFileTypeResource:
            data = [NSData dataWithContentsOfFile:[mMainBundle pathForResource:[NSString stringWithFormat:@"%@%@", mFlashName, mFileDataType == FlashViewFileDataTypeJson ? @".flajson" : @".flabin"] ofType:nil]];
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
        case FlashViewFileTypeDocument:
            return [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@/%@/%@/%@", mWritablePath, mFlashAnimDir, mFlashName, path]];
        case FlashViewFileTypeResource:
            return [UIImage imageWithContentsOfFile:[mMainBundle pathForResource:path ofType:nil]];
        default:
            break;
    }
    return nil;
}

//获取动画名称
-(NSArray *)animNames{
    return mFlashViewNode.anims.allKeys;
}

//解析json文件
-(BOOL) parseJsonFile{
    NSDictionary *jsonDict = [self readJson];
    if (!jsonDict) {
        NSLog(@"未读取到json文件");
        return NO;
    }
    
    //帧率
    NSInteger frameRate = [jsonDict[@"frameRate"] integerValue];
    uint64_t oneFrameTimeMs = 1000 / frameRate;
    
    //test
    //    frameRate = 5;
    //    oneFrameTimeMs = 200;
    
    self.tool.implicitAnimDuration = self.implicitAnimDurationScale / frameRate;
    
    if (!mFlashViewNode) {
        mFlashViewNode = [[FlashViewNode alloc] init];
        mFlashViewNode.oneFrameDurationMs = oneFrameTimeMs;
        mFlashViewNode.frameRate = frameRate;
    }
    
    //图片
    for (NSString *texName in jsonDict[@"textures"]) {
        [self.tool addImage:[self readImage:texName] withName:texName];
    }
    
    //读取动画
    for (NSDictionary *anim in jsonDict[@"anims"]) {
        FlashViewAnimNode *animNode = [[FlashViewAnimNode alloc] init];
        animNode.animName = anim[@"animName"];
        animNode.frameCount = [anim[@"frameMaxNum"] integerValue];
        [mFlashViewNode addAnim:animNode];
        NSArray *layers = anim[@"layers"];
        for (int i = 0; i < layers.count; i++) {
            NSDictionary *layer = layers[i];
            FlashViewLayerNode *layerNode = [[FlashViewLayerNode alloc] init];
            layerNode.index = layers.count - i;
            layerNode.tool = self.tool;
            layerNode.animName = animNode.animName;
            [animNode addLayer:layerNode];
            for (NSDictionary *keyFrame in layer[@"frames"]) {
                FlashViewFrameNode *frameNode = [[FlashViewFrameNode alloc] init];
                [layerNode addKeyFrame:frameNode];
                frameNode.frameIndex = [keyFrame[@"frameIndex"] intValue];
                frameNode.isEmpty = [keyFrame[@"isEmpty"] boolValue];
                if (!frameNode.isEmpty) {
                    frameNode.duration = [keyFrame[@"duration"] intValue];
                    frameNode.isTween = [keyFrame[@"isTween"] boolValue];
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
                }else{
                    frameNode.duration = 1;
                }
            }
        }
    }
    [mFlashViewNode onReady];
    
    return YES;
}

//解析二进制描述文件
-(BOOL) parseBinFile{
    NSData *binData = [self readData];
    if (!binData) {
        NSLog(@"读取二进制描述文件失败");
        return NO;
    }
    
    FlashViewDataReader *dataReader = [[FlashViewDataReader alloc] initWithNSData:binData];
    NSInteger frameRate = [dataReader readUShort];
    uint64_t oneFrameTimeMs = 1000 / frameRate;
    
    self.tool.implicitAnimDuration = self.implicitAnimDurationScale / frameRate;
    
    if (!mFlashViewNode) {
        mFlashViewNode = [[FlashViewNode alloc] init];
        mFlashViewNode.oneFrameDurationMs = oneFrameTimeMs;
        mFlashViewNode.frameRate = frameRate;
    }
    
    NSString *imagePath = nil;
    switch (mFileType) {
        case FlashViewFileTypeDocument:
            imagePath = [NSString stringWithFormat:@"%@/%@/%@", mWritablePath, mFlashAnimDir, mFlashName];
            break;
        case FlashViewFileTypeResource:
            imagePath = [mMainBundle bundlePath];
            break;
        default:
            break;
    }
    self.tool.imagePath = imagePath;
    
    NSInteger imageCount = [dataReader readUShort];
    NSMutableArray *imageNames = [[NSMutableArray alloc] init];
    for (int i = 0; i < imageCount; i++) {
        NSString *texName = [dataReader readNSString];
        [self.tool addImage:[self readImage:texName] withName:texName];
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
            layerNode.index = layerCount - k;
            layerNode.tool = self.tool;
            layerNode.animName = animNode.animName;
            [animNode addLayer:layerNode];
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
                }else{
                    frameNode.duration = 1;
                }
            }
        }
    }
    [mFlashViewNode onReady];
    return YES;
}

//播放某帧动画
-(void)updateToFrameIndex:(NSInteger)frameIndex{
    if (!isPlaying) {
        return;
    }
    FlashViewAnimNode *animNode = mFlashViewNode.anims[mPlayingAnimName];
    [animNode updateToIndex:frameIndex lastIndex:mLastPlayIndex];
}

//动画内事件
-(void)trigerEventWithIndex:(NSInteger)frameIndex{
    if (!isPlaying) {
        return;
    }
    FlashViewAnimNode *animNode = mFlashViewNode.anims[mPlayingAnimName];
    [animNode trigerEventWithIndex:frameIndex delegate:self];
}

//触发事件
-(void)onEvent:(FlashViewEvent)evt data:(id)d{
    if (self.onEventBlock) {
        self.onEventBlock(evt, d);
    }else{
        if (self.delegate) {
            [self.delegate onEvent:evt data:d];
        }
    }
}

//当前时间：毫秒
-(uint64_t) currentTimeMs{
    NSTimeInterval time = [NSDate date].timeIntervalSince1970;
    return (uint64_t)(time * 1000);
}

//当前播放的动画数据
-(FlashViewAnimNode *) currAnimNode{
    if (isPlaying) {
        return mFlashViewNode.anims[mPlayingAnimName];
    }
    return nil;
}

//触发事件
-(void) triggerEventWithCurrTime:(NSTimeInterval) currTime{
    if (!isPlaying) {
        return;
    }
    
    //自上次循环结束到现在，应该播放多少帧
    NSInteger passedFrames = floor((currTime - mLastFrameTimeMs) / mFlashViewNode.oneFrameDurationMs);
    if (passedFrames == 0) {
        return;
    }
    
    //从上次updateAnim:之后，过了几帧，每一帧都要检查是否有事件，算当前的帧，如果上一帧是5，当前帧是6，则 循环次数为1.
    for (NSInteger i = 0; i < passedFrames; i++) {
        NSInteger frameIndex = mLastPlayIndex + 1 + i;
        if (frameIndex > mToIndex) {
            frameIndex = frameIndex - mToIndex + mFromIndex;
        }
        [self trigerEventWithIndex:frameIndex];
    }
}

//动画主循环函数
-(void) updateAnim:(CADisplayLink *)displayLink{
    uint64_t currTime = self.currentTimeMs;
    NSTimeInterval passedTime = currTime - mStartTimeMs;
    NSTimeInterval passedCount = passedTime / mFlashViewNode.oneFrameDurationMs;
    NSInteger animLen = mToIndex - mFromIndex + 1;
    NSInteger currIndex = mFromIndex + (NSInteger)passedCount % animLen;
    
    //    NSLog(@"currIndex=%ld mlastPlayIndex=%ld", (long)currIndex, (long)mLastPlayIndex);
    //播放
    if (currIndex != mLastPlayIndex) {
        [self updateToFrameIndex:currIndex];
        [self onEvent:FlashViewEventFrame data:@(currIndex)];
    }
    
    //初始化lastPlayIndex
    if (mLastPlayIndex < 0) {
        mLastPlayIndex = 0;
    }
    
    //触发事件
    [self triggerEventWithCurrTime:currTime];
    
    //判断结束事件
    if (currIndex != mLastPlayIndex && (mLastUpdateLoopTimeMs < 0 || currTime - mLastUpdateLoopTimeMs >= mFlashViewNode.oneFrameDurationMs * (animLen - 1))) {
        FlashViewAnimNode *animNode = mFlashViewNode.anims[mPlayingAnimName];
        if (currIndex + 1 >= animNode.frameCount || currIndex < mLastPlayIndex) {
            mLoopTimes++;
            mLastUpdateLoopTimeMs = currTime;
            if (mTotalLoopTimes != FlashViewLoopTimeForever && mLoopTimes >= mTotalLoopTimes) {
                [self stop];
                return;
            }
            
            [self onEvent:FlashViewEventOneLoopEnd data:@(mLoopTimes)];
            [animNode resetLayer];
        }
    }
    
    //重置状态
    mLastPlayIndex = currIndex;
    //向前对齐
    if (passedCount != (NSInteger) passedCount) {
        mLastFrameTimeMs = floor(passedCount) * mFlashViewNode.oneFrameDurationMs + mStartTimeMs;
    }else{
        mLastFrameTimeMs = currTime;
    }
}

//计时器
-(CADisplayLink *)displayLink{
    if (!mDisplayLink) {
        mDisplayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateAnim:)];
    }
    return mDisplayLink;
}

//播放动画
-(void)play:(NSString *)animName loopTimes:(NSUInteger)times{
    [self play:animName loopTimes:times fromIndex:0];
}

//播放动画
-(void)play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger)from{
    [self play:animName loopTimes:times fromIndex:from toIndex:-1];
}

//播放动画
-(void) play:(NSString *) animName loopTimes:(NSUInteger) loopTimes fromIndex:(NSInteger) fromIndex toIndex:(NSInteger) toIndex{
    if (!animName || ![self.animNames containsObject:animName]) {
        NSLog(@"[E] param animName(%@) is error in FlashView.play", animName);
        return;
    }
    if (!isPlaying) {
        [self.displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSRunLoopCommonModes];
    }
    mPlayingAnimName = animName;
    NSInteger maxIndex = mFlashViewNode.anims[mPlayingAnimName].frameCount - 1;
    mFromIndex = fromIndex;
    if (mFromIndex < 0 || mFromIndex > maxIndex) {
        mFromIndex = 0;
    }
    mToIndex = toIndex;
    if (mToIndex < 0 || mToIndex > maxIndex) {
        mToIndex = maxIndex;
    }
    mTotalLoopTimes = loopTimes;
    mLoopTimes = 0;
    mStartTimeMs = self.currentTimeMs;
    mLastFrameTimeMs = mStartTimeMs;
    mLastUpdateLoopTimeMs = -1;
    
    [self onEvent:FlashViewEventStart data:nil];
    isPlaying = YES;
    
    self.tool.playingAnimName = animName;
}

//设置循环次数
-(void) setLoopTimes:(NSInteger) times{
    if (isPlaying) {
        mTotalLoopTimes = times;
    }
}

//像图片一样显示动画的某一帧内容
-(void)stopAtFrameIndex:(NSInteger)frameIndex animName:(NSString *)animName{
    if (isPlaying) {
        [self stop];
    }
    self.tool.playingAnimName = animName;
    FlashViewAnimNode *animNode = mFlashViewNode.anims[animName];
    if (animNode && frameIndex >= 0 && frameIndex < animNode.frameCount) {
        [animNode updateToIndex:frameIndex lastIndex:-1];
    }
}
//暂停
-(void) pause{
    isPlaying = NO;
}

//恢复播放
-(void) resume{
    isPlaying = YES;
}

-(void) stopInner{
    //清除当前view
    [mFlashViewNode.anims[mPlayingAnimName] removeLayers];
    
    isPlaying = NO;
    mPlayingAnimName = nil;
    mStartTimeMs = 0;
    mFromIndex = 0;
    mToIndex = 0;
    mLoopTimes = 0;
    mTotalLoopTimes = 0;
    mLastPlayIndex = -1;
    mLastUpdateLoopTimeMs = -1;
    [self.displayLink invalidate];
    mDisplayLink = nil;
    
    self.tool.playingAnimName = nil;
}

//停止动画
-(void) stop{
    [self stopInner];
    [self onEvent:FlashViewEventStop data:nil];
}

//替换某一个动画元件的图片
-(void) replaceImage:(NSString *)texName image:(UIImage *)image{
    [self.tool replaceImage:image withName:texName];
}

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName{
    return [self reload:flashName andAnimDir:FLASH_VIEW_DEFAULT_DIR_NAME];
}

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName andAnimDir:(NSString *)animDir{
    return [self reload:flashName andAnimDir:animDir scaleMode:FlashViewScaleModeRespective designResolution:CGSizeMake(640, 1136)];
}

//重新加载一个新的动画文件
-(BOOL) reload:(NSString *)flashName andAnimDir:(NSString *)animDir scaleMode:(FlashViewScaleMode)scaleMode designResolution:(CGSize)resolution{
    [self stopInner];
    mFlashViewNode = nil;
    self.tool = nil;
    mFlashName = flashName;
    mFlashAnimDir = animDir;
    [self calculateFactAnimOffset];
    [self setScaleMode:scaleMode andDesignResolution:resolution];
    if (![self innerInit]) {
        return NO;
    }
    return YES;
}

//view被移除后，关闭定时器。
-(void)willMoveToWindow:(UIWindow *)newWindow{
    if (!newWindow) {
        if (isPlaying) {
            [self.displayLink invalidate];
        }
    }else{
        if (isPlaying) {
            [self.displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSRunLoopCommonModes];
        }
    }
}

//判断动画是否存在
+(BOOL) isAnimExist:(NSString *)flashName{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSBundle *mainBundle = [NSBundle mainBundle];
    NSString *writablePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    //先查找是否存在flajson文件，不存在则查找flabin。都不存在则初始化错误。并且确定文件是在Resource中还是在Document中
    NSString * filePath = [mainBundle pathForResource:[NSString stringWithFormat:@"%@.flajson", flashName] ofType:nil];
    if (!filePath) {
        filePath = [mainBundle pathForResource:[NSString stringWithFormat:@"%@.flabin", flashName] ofType:nil];
        if (!filePath) {
            filePath = [NSString stringWithFormat:@"%@/%@/%@.flajson", writablePath, FLASH_VIEW_DEFAULT_DIR_NAME, flashName];
            if ([fileManager fileExistsAtPath:filePath]) {
                return YES;
            }else{
                filePath = [NSString stringWithFormat:@"%@/%@/%@.flabin", writablePath, FLASH_VIEW_DEFAULT_DIR_NAME, flashName];
                if ([fileManager fileExistsAtPath:filePath]) {
                    return YES;
                }
            }
        }else{
            return YES;
        }
    }else{
        return YES;
    }
    return NO;
}

//根据图片名获取动画图片
-(UIImage *) animImageWithName:(NSString *)name{
    return [self.tool imageWithName:name];
}

@end
