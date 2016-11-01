/*
 copyright 2016 wanghongyu. 
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import "FlashView.h"
#import "FlashViewDataReader.h"

//自定义颜色叠加类型
typedef struct FlashColor{
    float r;
    float g;
    float b;
    float a;
} FlashColor;

//颜色叠加类型
static FlashColor FlashColorMake(float r, float g, float b, float a){
    FlashColor c;
    c.r = r;
    c.g = g;
    c.b = b;
    c.a = a;
    return c;
}

@implementation FlashView{
    //flash动画文件名
    NSString *mFlashName;
    //如果动画在Document里面，该文件的目录名
    NSString *mFlashAnimDir;
    NSFileManager *mFileManager;
    NSBundle *mMainBundle;
    NSString *mWritablePath;
    
    //文件在Document里还是在Resource里
    FlashViewFileType mFileType;
    //动画是json还是二进制
    FlashViewFileDataType mFileDataType;
    
    //动画描述文件json数据
    NSDictionary *mJson;
    //动画描述文件二进制数据
    NSData *mData;
    
    //播放速率
    NSInteger mFrameRate;
    
    //解析后得到的动画播放数据
    NSMutableDictionary *mParsedData;
    
    //图片缓存
    NSMutableDictionary *mImages;
    
    //动画运转参数
    NSTimer *mTimer;
    //播放一帧需要的时间
    float mOneFrameTime;
    //当前动画播放从开始到现在所度过的时间
    double mStartTime;
    //上次更新时间
    double mLastUpdateTime;
    //当前动画播放到了第几帧
    NSInteger mCurrFrameIndex;
    
    //上一帧
    NSInteger mLastFrameIndex;
    
    //上次更新LoopTime帧数
    NSInteger mLastUpdateLoopTimeSec;
    
    //当前动画播放起始／结束帧
    NSInteger mFromIndex;
    NSInteger mToIndex;
    
    //当前运行的动画名
    NSString *mRunningAnimName;
    //是否已开始播放
    BOOL isStarted;
    //是否已暂停播放
    BOOL isPause;
    
    //设置循环的次数
    NSUInteger mSetLoopTimes;
    //当前循环的次数
    NSUInteger mLoopTimes;
    
    //解析过程中使用的中间变量
    NSInteger mParseLastIndex;
    BOOL mParseLastIsTween;
    NSDictionary *mParseLastFrame;
    NSInteger mParseFrameMaxIndex;
    
    //设计分辨率 默认为 640x1136
    CGSize mDesignResolution;
    //设计分辨率换算出缩放值
    CGPoint mDesignResolutionScale;
    //缩放模型
    FlashViewScaleMode mScaleMode;
    
    //stop at函数所需要的参数
    NSInteger mStopAtFrameIndex;
    NSString *mStopAtAnimName;
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

//默认初始化缩放比例
-(void) initDesignResolutionScale{
    CGRect screenBound = [[UIScreen mainScreen] bounds];
    
    switch (mScaleMode) {
        case FlashViewScaleModeWidthFit:
            mDesignResolutionScale = CGPointMake(mDesignResolution.width / screenBound.size.width, mDesignResolution.width / screenBound.size.width);
            break;
        case FlashViewScaleModeHeightFit:
            mDesignResolutionScale = CGPointMake(mDesignResolution.height / screenBound.size.height, mDesignResolution.height / screenBound.size.height);
            break;
        case FlashViewScaleModeRespective:
            mDesignResolutionScale = CGPointMake(mDesignResolution.width / screenBound.size.width, mDesignResolution.height / screenBound.size.height);
            break;
        default:
            mDesignResolutionScale = CGPointMake(1, 1);
            break;
    }
}

//私有初始化函数
-(BOOL) innerInit{
    mFileManager = [NSFileManager defaultManager];
    mMainBundle = [NSBundle mainBundle];
    //document根目录
    mWritablePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    mFileType = FlashViewFileTypeNone;
    mSetLoopTimes = 1;
    mLoopTimes = 0;
    
    mParseLastIndex = -1;
    mParseLastIsTween = NO;
    mParseLastFrame = nil;
    mParseFrameMaxIndex = 0;
    
    mStopAtFrameIndex = 0;
    mStopAtAnimName = nil;
    
    mLastUpdateTime = -1;
    
    self.isInitOk = NO;
    
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
        mJson = [self readJson];
        
        if (!mJson) {
            NSLog(@"FlashView init error file %@.flajson is not json format", mFlashName);
            return NO;
        }
        
        [self parseJson];
    }else{
        mData = [self readData];
        if (!mData) {
            NSLog(@"FlashView init error file %@.flabin is not valid", mFlashName);
            return NO;
        }
        [self parseData];
    }
    
    mOneFrameTime = 1.f/mFrameRate;
    isPause = NO;
    
    //默认设计分辨率为iPhone5。可以使用 setScaleMode 修改此值。
    mDesignResolution = CGSizeMake(640, 1136);
    mScaleMode =FlashViewScaleModeRespective;
    [self initDesignResolutionScale];
    
    //默认frame设置为全屏
    CGRect screenBound = [[UIScreen mainScreen] bounds];
    self.frame = CGRectMake(0, 0, screenBound.size.width, screenBound.size.height);
    self.backgroundColor = [UIColor clearColor];
    self.userInteractionEnabled = NO;
    
    self.isInitOk = YES;
    
    return YES;
}

//设置缩放模式和设计分辨率，如果不是使用640x1136分辨率制作的flash，务必调用此方法。
-(void) setScaleMode:(FlashViewScaleMode)mode andDesignResolution:(CGSize)resolution{
    mDesignResolution = resolution;
    mScaleMode = mode;
    [self initDesignResolutionScale];
}

//设置动画缩放比例，绝对数值
-(void) setScaleWithX:(CGFloat)x y:(CGFloat) y isDesignResolutionEffect:(BOOL)isDREffect{
    if (isDREffect) {
        mDesignResolutionScale = CGPointMake(mDesignResolutionScale.x / x, mDesignResolutionScale.y / y);
    }else{
        mDesignResolutionScale = CGPointMake(1 / x, 1 / y);
    }
}

//设置动画循环次数
-(void) setLoopTimes:(NSInteger) times{
    mSetLoopTimes = times;
}

//替换动画中的某一张图片
-(void) replaceImage:(NSString *)texName image:(UIImage *)image{
    [mImages setObject:image forKey:texName];
}

-(NSArray *)animNames{
    return mParsedData.allKeys;
}

-(NSDictionary *)images{
    return mImages;
}

// 开始播放动画，
// animName就是在flash的library栏中anims文件夹内的movie clip的动画名称。
// times表示循环次数
-(void) play:(NSString *)animName loopTimes:(NSUInteger) times{
    [self play:animName loopTimes:times fromIndex:0];
}

-(void)play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger)from{
    [self play:animName loopTimes:times fromIndex:from toIndex:mParseFrameMaxIndex];
}

-(void)play:(NSString *)animName loopTimes:(NSUInteger)times fromIndex:(NSInteger)from toIndex:(NSInteger)to{
    if (![mParsedData objectForKey:animName]) {
        NSLog(@"error 找不到对应的动画名：%@", animName);
        return;
    }
    [self stop];
    mStartTime = 0;
    mCurrFrameIndex = from;
    [self startTimer];
    mRunningAnimName = animName;
    mSetLoopTimes = times;
    mLoopTimes = 0;
    mLastUpdateLoopTimeSec = -1;
    mFromIndex = from;
    mToIndex = to;
    if (self.delegate) {
        [self.delegate onEvent:FlashViewEventStart data:nil];
    }
    if (self.onEventBlock) {
        [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventStart)} waitUntilDone:NO];
    }
}

-(void) stopAtFrameIndex:(NSInteger) frameIndex animName:(NSString *) animName{
    [self stop];
    mStopAtFrameIndex = frameIndex;
    mStopAtAnimName = animName;
    [self setNeedsDisplay];
}

//timer begin
//使用新线程内的runloop处理动画循环，防止ScrollView滑动时动画停止。
-(void) startTimer{
    [self stopTimer];
    isStarted = YES;
    if (self.runMode == FlashViewRunModeMainThread) {
        mTimer = [NSTimer scheduledTimerWithTimeInterval:mOneFrameTime target:self selector:@selector(runTask) userInfo:nil repeats:YES];
    }else{
        NSThread *timerThread = [[NSThread alloc] initWithTarget:self selector:@selector(runThread) object:nil];
        [timerThread start];
    }
}

-(void) stopTimer{
    [mTimer invalidate];
    isStarted = NO;
}

-(void) runThread{
    NSRunLoop *runLoop = [NSRunLoop currentRunLoop];
    if (self.updateMode == FlashViewUpdateModeRealtimeTime) {
        mTimer = [NSTimer scheduledTimerWithTimeInterval:0 target:self selector:@selector(runTask) userInfo:nil repeats:YES];
    }else{
        mTimer = [NSTimer scheduledTimerWithTimeInterval:mOneFrameTime target:self selector:@selector(runTask) userInfo:nil repeats:YES];
    }
    [runLoop run];
}

-(NSTimeInterval)currentTime{
    return [[NSDate date]timeIntervalSince1970];
}

-(void) runTask{
    if (self.updateMode == FlashViewUpdateModeRealtimeTime) {
        double currentTime = self.currentTime;
        if (mStartTime <= 0) {
            mStartTime = currentTime;
        }
        if (mLastUpdateTime < 0) {
            mLastUpdateTime = currentTime;
        }else{
            double delayTime = mOneFrameTime - (currentTime - mLastUpdateTime);
            if (delayTime > 0) {
                usleep(delayTime * 1000000);
            }
            mLastUpdateTime = self.currentTime;
        }
    }
    [self update];
}
//timer end

//停止当前动画，并重新加载一个新的动画文件。
-(BOOL) reload:(NSString *)flashName{
    return [self reload:flashName andAnimDir:FLASH_VIEW_DEFAULT_DIR_NAME];
}

//停止当前动画，并重新加载一个新的动画文件。
-(BOOL)reload:(NSString *)flashName andAnimDir:(NSString *)animDir{
    [self cleanData];
    mFlashName = flashName;
    mFlashAnimDir = animDir;
    return [self innerInit];
}

//停止动画，清空数据
-(void)cleanData{
    [self stop];
    mFlashName = nil;
    mFlashAnimDir = nil;
    mFileDataType = FlashViewFileDataTypeNone;
    mFileType = FlashViewFileTypeNone;
    mJson = nil;
    mData = nil;
    mRunningAnimName = nil;
    mParsedData = nil;
    mImages = nil;
    
    mParseFrameMaxIndex = 0;
    mParseLastIndex = -1;
    mParseLastIsTween = false;
    mParseLastFrame = nil;
    
    mRunningAnimName = nil;
    mStartTime = 0;
    mCurrFrameIndex = 0;
    
    mSetLoopTimes = 1;
    mLoopTimes = 0;
    
    mStopAtFrameIndex = 0;
    mStopAtAnimName = nil;
    
    mLastFrameIndex = -1;
    mLastUpdateTime = -1;
}

//停止动画
-(void) stop{
    [self stopTimer];
    mRunningAnimName = nil;
    mSetLoopTimes = 1;
    mStartTime = 0;
    mCurrFrameIndex = 0;
    mLoopTimes = 0;
    
    mStopAtFrameIndex = 0;
    mStopAtAnimName = nil;
    
    mLastFrameIndex = -1;
    
    mLastUpdateTime = -1;
    mLastUpdateLoopTimeSec = -1;
}

//暂停
-(void)pause{
    isPause = YES;
}

//恢复
-(void) resume{
    isPause = NO;
}

//通知系统调用drawRect
-(void) update{
    if (!isPause && !self.hidden && self.window) {
        [self setNeedsDisplay];
        if (self.updateMode != FlashViewUpdateModeRealtimeTime) {
            mCurrFrameIndex++;
            if (mCurrFrameIndex > mParseFrameMaxIndex) {
                mCurrFrameIndex = mParseFrameMaxIndex;
            }
        }
    }
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

//计算补间动画的帮助宏
#define calcPerValue(x) \
[self getPerValue: oneFrame old:mParseLastFrame key: x per:per] \

#define calcPerValueColor(x) \
[self getPerValue: oneFrameColor old:lastFrameColor key: x per:per] \

/**
 * 获取线性插值
 * 其中旋转（skewX，skewY）同其他值有所不通。
 * flash中的逻辑是，这两个值一定在[-180, 180]之间，前后两个值相减的绝对值不能超过180才可以使用正常的线性插值，超过180的则需要将线性插值分为2部分：
 *   一是，先让oldValue同-180（或180，根据不通情况选择，见代码）进行插值
 *   二是，让-180（或180，根据不通情况选择，见代码）同newValue进行插值
 **/
-(NSNumber *)getPerValue: (NSDictionary *)new old:(NSDictionary*)old key:(NSString *)key per:(float) per{
    float oldValue = [[old objectForKey:key] floatValue];
    float newValue = [[new objectForKey:key] floatValue];
    float ret = -1;
    //计算跨度
    float span = fabsf(newValue - oldValue);
    if (span > 180 && ([key isEqualToString:@"skewX"] || [key isEqualToString:@"skewY"])) {//说明2个值不能直接变化需要跳板
        float realSpan = 360 - span;
        float mark = (oldValue < 0) ? -1 : 1;
        float mid = 180 * mark;
        float newStart = -mid;
        float midPer = fabsf(mid - oldValue) / realSpan;
        if (per < midPer) {
            ret = oldValue + per * realSpan * mark;
        }else{
            ret = newStart + (per - midPer) * realSpan * mark;
        }
    }else{
        ret = oldValue + per * (newValue - oldValue);
    }
    
    return @(ret);
}

//创建解析数据对象
-(NSMutableArray *) getParsedAnimWithIndex:(NSInteger) idx andParent:(NSMutableDictionary *)parent {
    NSMutableArray *arr = [parent objectForKey:@(idx)];
    if (!arr) {
        arr = [[NSMutableArray alloc] init];
        [parent setObject:arr forKey:@(idx)];
    }
    return arr;
}

//把解析的某一帧数据添加到结果数据中
-(void) addOneFrameToParsedAnimWithArr:(NSMutableArray *)arr frame:(NSDictionary *)oneFrame{
    if (!oneFrame) {
        return;
    }
    float x = [[oneFrame objectForKey:@"x"] floatValue];
    float y = [[oneFrame objectForKey:@"y"] floatValue];
    float alpha = [oneFrame[@"alpha"] floatValue];
    NSDictionary *color = [oneFrame objectForKey:@"color"];
    [arr addObject:@{
                     @"texName": [oneFrame objectForKey:@"texName"],
                     @"x": @(x),
                     @"y": @(y),
                     @"sx": [oneFrame objectForKey:@"scaleX"],
                     @"sy": [oneFrame objectForKey:@"scaleY"],
                     @"skewX": [oneFrame objectForKey:@"skewX"],
                     @"skewY": [oneFrame objectForKey:@"skewY"],
                     @"mark":[oneFrame objectForKey:@"mark"],
                     @"alpha": @(alpha),
                     @"r": [color objectForKey:@"r"],
                     @"g": [color objectForKey:@"g"],
                     @"b": [color objectForKey:@"b"],
                     @"a": [color objectForKey:@"a"],
                     }];
}

/***
 * 从二进制动画描述文件中（xxx.flabin），读取某一帧的动画图片的信息（位置旋转缩放等等）
 * 重新把二进制数据读取成一个dict是为了和Json数据读取方式共用一套生成关键帧对象的代码。
 * 因为两种数据解析方式，当读取到数据后，处理方式一摸一样，二者统一的关键就在于这个方法。
 */
-(NSMutableDictionary *) readKeyFrame:(FlashViewDataReader *)reader imageArr:(NSMutableArray *)imageArr{
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    BOOL isEmpty = [reader readBool];
    [dict setObject:@(isEmpty) forKey:@"isEmpty"];
    [dict setObject:@([reader readUShort]) forKey:@"frameIndex"];
    if (!isEmpty) {
        [dict setObject:@([reader readUShort]) forKey:@"duration"];
        [dict setObject:@([reader readBool]) forKey:@"isTween"];
        [dict setObject:[imageArr objectAtIndex: [reader readUShort]] forKey:@"texName"];
        [dict setObject:[reader readNSString] forKey:@"mark"];
        [dict setObject:@([reader readUChar]) forKey:@"alpha"];
        [dict setObject: @{
                           @"r": @([reader readUChar]),
                           @"g": @([reader readUChar]),
                           @"b": @([reader readUChar]),
                           @"a": @([reader readUChar]),
                           }
                 forKey:@"color"];
        [dict setObject:@([reader readFloat]) forKey:@"scaleX"];
        [dict setObject:@([reader readFloat]) forKey:@"scaleY"];
        [dict setObject:@([reader readFloat]) forKey:@"skewX"];
        [dict setObject:@([reader readFloat]) forKey:@"skewY"];
        [dict setObject:@([reader readFloat]) forKey:@"x"];
        [dict setObject:@([reader readFloat]) forKey:@"y"];
    }
    
    return dict;
}

/***
 * 解析关键帧数据，并将帧数据对应信息加入到parsedAnim中的对应索引内。
 * 播放的时候，播放到哪一帧就把对应的帧数据取出，然后显示里面的图片
 * 本方法处理的帧为：当前关键帧和上一个关键帧之间的所有帧数据。
 */
-(void) parseKeyFrame:(NSDictionary *)oneFrame parsedAnim:(NSMutableDictionary *)parsedAnim isLastKeyFrame:(BOOL) isLastKeyFrame{
    NSInteger index = [[oneFrame objectForKey:@"frameIndex"] integerValue];
    BOOL isEmpty = [[oneFrame objectForKey:@"isEmpty"] boolValue];
    if (isEmpty) {
        mParseLastIndex = index;
        mParseLastIsTween = NO;
        mParseLastFrame = oneFrame;
        return;
    }
    
    BOOL lastFrameIsEmpty = mParseLastFrame ? [mParseLastFrame[@"isEmpty"] boolValue] : YES;
    
    NSInteger duration = [[oneFrame objectForKey:@"duration"] integerValue];
    
    BOOL isTween = [[oneFrame objectForKey:@"isTween"] boolValue];
    
    NSInteger fromIdx = mParseLastIndex + 1;
    NSInteger toIdx = index;
    
    NSInteger len = toIdx - fromIdx + 1;
    
    for (NSInteger l = fromIdx; l <= toIdx; l++) {
        NSMutableArray *arr = [self getParsedAnimWithIndex:l andParent:parsedAnim];
        if (!mParseLastIsTween) {
            if (l == toIdx) {
                [self addOneFrameToParsedAnimWithArr:arr frame:oneFrame];
            }else{
                if(!lastFrameIsEmpty){
                    [self addOneFrameToParsedAnimWithArr:arr frame:mParseLastFrame];
                }
            }
        }else{
            float per = (float)(l - fromIdx + 1) / len;
            NSDictionary *oneFrameColor = [oneFrame objectForKey:@"color"];
            NSDictionary *lastFrameColor = [mParseLastFrame objectForKey:@"color"];
            NSString *mark = @"";
            if (l == toIdx) {
                mark = [oneFrame objectForKey:@"mark"];
            }
            NSDictionary *onePerFrame = @{
                                          @"texName": [mParseLastFrame objectForKey:@"texName"],
                                          @"x": calcPerValue(@"x"),
                                          @"y": calcPerValue(@"y"),
                                          @"sx": calcPerValue(@"scaleX"),
                                          @"sy": calcPerValue(@"scaleY"),
                                          @"skewX": calcPerValue(@"skewX"),
                                          @"skewY": calcPerValue(@"skewY"),
                                          @"alpha": calcPerValue(@"alpha"),
                                          @"r": calcPerValueColor(@"r"),
                                          @"g": calcPerValueColor(@"g"),
                                          @"b": calcPerValueColor(@"b"),
                                          @"a": calcPerValueColor(@"a"),
                                          @"mark": mark
                                          };
            [arr addObject:onePerFrame];
        }
    }
    
    
    //如果是最后一个关键帧 需要检测从这一帧到本层结束是否有图像
    if(isLastKeyFrame){
        for (NSInteger m = index; m < index + duration; m++) {
            NSMutableArray *arr = [self getParsedAnimWithIndex:m andParent:parsedAnim];
            [self addOneFrameToParsedAnimWithArr:arr frame:oneFrame];
        }
    }
    
    //记录当前的index，留给下次解析使用
    mParseLastIndex = index;
    mParseLastIsTween = isTween;
    mParseLastFrame = oneFrame;
}

/***
 * 解析二进制数据，并存储所需的图片
 * 无论使用json还是二进制数据，最终mParsedData的数据格式都是一样的。
 * mParsedData 结构为：
 * --mParsedData（Map，对应多个动画）
 *    --[key]: anim1
 *    --[value]:
 *      --AnimData
 *        --keyFrameData（数组）
 *          --[key]:0（表示第几帧）
 *          --[value]:（表示这一帧上不同层的所有图片信息）
 *              --image1
 *                  --xxxx1.png
 *                  --position:{100,100},
 *                  --scale:{1,1}
 *                  -- ... ...
 *              --image2
 *                  --xxxx2.png
 *                  --position:{100,100},
 *                  --scale:{1,1}
 *                  -- ... ...
 *              ... ...
 *          --[key]:1
 *          --[value]:
 *              --image1
 *                  --xxxx1.png
 *                  --position:{100,100},
 *                  --scale:{1,1}
 *                  -- ... ...
 *              --image2
 *                  --xxxx2.png
 *                  --position:{100,100},
 *                  --scale:{1,1}
 *                  -- ... ...
 *              ... ...
 *          ... ...
 *    --[key]: anim2
 *    --[value]:
 *      ... ...
 *    ... ...
 */
-(void) parseData{
    mParsedData = [[NSMutableDictionary alloc] init];
    mImages = [[NSMutableDictionary alloc] init];
    NSMutableArray *imagesArr = [[NSMutableArray alloc] init];
    
    FlashViewDataReader *reader = [[FlashViewDataReader alloc] initWithNSData:mData];
    
    mFrameRate = [reader readUShort];
    //解析images
    NSInteger imageNum = [reader readUShort];
    for (int i = 0; i < imageNum; i++) {
        NSString *texName = [reader readNSString];
        [mImages setObject:[self readImage:texName] forKey:texName];
        [imagesArr addObject:texName];
    }
    //解析anims
    NSInteger animNum = [reader readUShort];
    for (int j = 0; j < animNum; j++) {
        NSMutableDictionary *parsedAnim = [[NSMutableDictionary alloc] init];
        NSString *animName = [reader readNSString];
        mParseFrameMaxIndex = [reader readUShort] - 1;
        NSInteger layerNum = [reader readUShort];
        for (int k = 0; k < layerNum; k++) {
            NSInteger keyFrameNum = [reader readUShort];
            mParseLastIndex = -1;
            mParseLastIsTween = NO;
            mParseLastFrame = nil;
            for (int l = 0; l < keyFrameNum; l++) {
                NSMutableDictionary *oneFrame = [self readKeyFrame:reader imageArr:imagesArr];
                [self parseKeyFrame:oneFrame parsedAnim:parsedAnim isLastKeyFrame:l == keyFrameNum - 1];
            }
        }
        [mParsedData setObject:parsedAnim forKey:animName];
    }
}

//解析json数据
-(void) parseJson{
    mParsedData = [[NSMutableDictionary alloc] init];
    mImages = [[NSMutableDictionary alloc] init];
    
    mFrameRate = [[mJson objectForKey:@"frameRate"] integerValue];
    
    NSArray *textures = [mJson objectForKey:@"textures"];
    //解析images
    for (int n = 0; n < textures.count; n++) {
        NSString *texName = [textures objectAtIndex:n];
        [mImages setObject:[self readImage:texName] forKey:texName];
    }
    //解析anims
    NSArray *anims = [mJson objectForKey:@"anims"];
    for (int i = 0; i < anims.count; i++) {
        NSMutableDictionary *parsedAnim = [[NSMutableDictionary alloc] init];
        NSDictionary *oneAnim = [anims objectAtIndex:i];
        NSString *animName = [oneAnim objectForKey:@"animName"];//当前动画
        mParseFrameMaxIndex = [[oneAnim objectForKey:@"frameMaxNum"] integerValue] - 1;//动画一共多少帧
        
        NSArray *layers = [oneAnim objectForKey:@"layers"];
        for (int j = 0; j < layers.count; j++) {
            NSDictionary *oneLayer = [layers objectAtIndex: j];
            NSArray *frames = [oneLayer objectForKey:@"frames"];
            mParseLastIndex = -1;
            mParseLastIsTween = NO;
            mParseLastFrame = nil;
            for (int k = 0; k < frames.count; k++) {
                NSDictionary *oneFrame = [frames objectAtIndex: k];
                [self parseKeyFrame:oneFrame parsedAnim:parsedAnim isLastKeyFrame:k == frames.count - 1];
            }
        }
        
        [mParsedData setObject:parsedAnim forKey:animName];
    }
}

//回调事件在主线程中处理
-(void) onEventOnMainThread:(id)data{
    if (self.onEventBlock) {
        self.onEventBlock((FlashViewEvent)[[data objectForKey:@"event"] unsignedIntegerValue], [data objectForKey:@"data"]);
    }
}

/***
 * 绘制某一帧的所有数据，遍历着一帧上的所有frame，调用drawImage绘制每一层上的每一帧
 */
-(void) drawRectForFrameIndex:(NSInteger) frameIndex animName:(NSString *)animName isTriggerEvent:(BOOL) isTriggerEvent{
    if (isTriggerEvent) {
        if (self.delegate) {
            [self.delegate onEvent:FlashViewEventFrame data:@(frameIndex)];
        }
        if (self.onEventBlock) {
            [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventFrame), @"data":@(frameIndex)} waitUntilDone:NO];
        }
    }
    
    //防止动画停止前多播放了一帧。此处为临时修改，正确方法应该先判断动画完成事件，然后再drawIamge。
    if (mSetLoopTimes == FlashViewLoopTimeForever || mLoopTimes < mSetLoopTimes || (mLoopTimes == mSetLoopTimes - 1 && mLastFrameIndex <= frameIndex)) {
        NSDictionary *animDict = [mParsedData objectForKey:animName];
        
        NSArray *frameArray = [animDict objectForKey:@(frameIndex)];
        for (NSInteger i = frameArray.count - 1; i >= 0; i--) {
            NSDictionary *oneImageDict = [frameArray objectAtIndex:i];
            NSString *imagePath = [oneImageDict objectForKey:@"texName"];
            CGPoint drawPoint = CGPointMake([[oneImageDict objectForKey:@"x"] floatValue] / mDesignResolutionScale.x, [[oneImageDict objectForKey:@"y"] floatValue] / mDesignResolutionScale.y);
            CGPoint anchorPoint = CGPointMake(0.5f, 0.5f);
            CGPoint scale = CGPointMake([[oneImageDict objectForKey:@"sx"] floatValue], [[oneImageDict objectForKey:@"sy"] floatValue]);
            CGPoint rotation = CGPointMake([[oneImageDict objectForKey:@"skewX"] floatValue], [[oneImageDict objectForKey:@"skewY"] floatValue]);
            FlashColor color = FlashColorMake([[oneImageDict objectForKey:@"r"] floatValue] / 255,
                                              [[oneImageDict objectForKey:@"g"] floatValue] / 255,
                                              [[oneImageDict objectForKey:@"b"] floatValue] / 255,
                                              [[oneImageDict objectForKey:@"a"] floatValue] / 255
                                              );
            
            CGFloat alpha = [[oneImageDict objectForKey:@"alpha"] floatValue] / 255;
            
            //居中
            drawPoint = CGPointMake(drawPoint.x + self.frame.size.width / 2, drawPoint.y + self.frame.size.height / 2);
            
            //        YYLog(@"draw image %@ for index = %ld", imagePath, frameIndex);
            [self drawImage:imagePath atPoint:drawPoint anchor:anchorPoint scale:scale rotation:rotation color: color alpha:alpha];
            
            if (isTriggerEvent) {
                NSString *mark = [oneImageDict objectForKey:@"mark"];
                if (mark && mark.length > 0) {
                    NSLog(@"遇到事件！currIndex = %ld, dict = %@", (long)frameIndex, oneImageDict);
                    if (self.delegate) {
                        [self.delegate onEvent:FlashViewEventMark data:@{@"index": @(frameIndex), @"mark": mark, @"data": oneImageDict}];
                    }
                    if (self.onEventBlock) {
                        [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventMark), @"data":@{@"index": @(frameIndex), @"mark": mark, @"data": oneImageDict}} waitUntilDone:NO];
                    }
                }
            }
        }
    }
    if (isTriggerEvent) {
        NSInteger animLen = mToIndex - mFromIndex;
        NSTimeInterval currTime = self.currentTime;
        if (mCurrFrameIndex != mLastFrameIndex && (mLastUpdateLoopTimeSec < 0 || currTime - mLastUpdateLoopTimeSec >= mOneFrameTime * (animLen - 1))) {
            if (frameIndex == animLen - 1 || mLastFrameIndex > frameIndex) {
                if (self.delegate) {
                    [self.delegate onEvent:FlashViewEventOneLoopEnd data:@(mLoopTimes)];
                }
                if (self.onEventBlock) {
                    [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventOneLoopEnd), @"data":@(mLoopTimes)} waitUntilDone:NO];
                }
                if (mSetLoopTimes >= FlashViewLoopTimeOnce) {
                    if (++mLoopTimes >= mSetLoopTimes) {
                        if (self.delegate) {
                            [self.delegate onEvent:FlashViewEventStop data:nil];
                        }
                        if (self.onEventBlock) {
                            [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventStop)} waitUntilDone:NO];
                        }
                        [self stop];
                    }
                    mLastUpdateLoopTimeSec = currTime;
                }
                mCurrFrameIndex = mFromIndex;
            }
        }
        mLastFrameIndex = frameIndex;
    }
}

/**
 * 有时候会因为卡顿或浮点数计算等原因产生漏帧现象，但是mark事件不能漏，这个方法就是一旦产生漏帧现象，把漏掉的事件找回来
 * @param animName 动画名称
 * @param frameIndex 第几帧
 */
-(void) checkMark:(NSString *)animName frameIndex:(NSInteger) frameIndex{
    NSDictionary *animDict = [mParsedData objectForKey:animName];
    if (animDict[@(frameIndex)]) {
        NSArray *frameArray = [animDict objectForKey:@(frameIndex)];
        for (NSDictionary *dict in frameArray) {
            NSString *mark = dict[@"mark"];
            if (mark && mark.length > 0) {
                NSLog(@"又遇到事件！currIndex = %ld, dict = %@", (long)frameIndex, dict);
                if (self.delegate) {
                    [self.delegate onEvent:FlashViewEventMark data:@{@"index": @(frameIndex), @"mark": mark, @"data": dict}];
                }
                if (self.onEventBlock) {
                    [self performSelectorOnMainThread:@selector(onEventOnMainThread:) withObject:@{@"event": @(FlashViewEventMark), @"data":@{@"index": @(frameIndex), @"mark": mark, @"data": dict}} waitUntilDone:NO];
                }
            }
        }
    }
}

//drawRect的实际调用方法。
-(void) drawRectForAnim:(CGRect) rect{
    if (!mRunningAnimName) {
        return;
    }
    
    NSInteger currFrameIndex = -1;
    if (self.updateMode == FlashViewUpdateModeRealtimeTime) {
        if (mStartTime == 0) {
            return;
        }
        
        currFrameIndex = mFromIndex + (NSInteger)((self.currentTime - mStartTime) / mOneFrameTime) % (mToIndex - mFromIndex + 1);
        
        NSInteger mid = -1;
        if (mLastFrameIndex > currFrameIndex) {
            mid = mParseFrameMaxIndex;
        }
        if (mid != -1) {
            for (NSInteger i = mLastFrameIndex + 1; i <= mid; i++) {
                [self checkMark: mRunningAnimName frameIndex:i];
            }
            for (NSInteger i = 0; i < currFrameIndex; i++) {
                [self checkMark: mRunningAnimName frameIndex:i];
            }
        } else {
            for (NSInteger i = mLastFrameIndex + 1; i < currFrameIndex; i++) {
                [self checkMark: mRunningAnimName frameIndex:i];
            }
        }
    }else{
        currFrameIndex = mCurrFrameIndex;
    }
    
    //    YYLog(@"---------currentFrameIndex=%ld animLen=%ld", currFrameIndex, mToIndex - mFromIndex + 1);
    
    [self drawRectForFrameIndex:currFrameIndex animName:mRunningAnimName isTriggerEvent:YES];
}

//角度转弧度
#define ANGLE_TO_RADIUS(angle) (0.01745329252f * (angle))

/***
 * 在画布上绘制一张图片
 * @param imagePath 图片路径
 * @param drawPoint 带锚点位置
 * @param anchorPoint 锚点
 * @param scale 缩放
 * @param rotate 旋转
 * @param color 颜色叠加
 * @param alpha 透明度
 */
-(void) drawImage:(NSString *)imagePath atPoint: (CGPoint)drawPoint anchor:(CGPoint) anchorPoint scale:(CGPoint) scale rotation:(CGPoint)rotation color:(FlashColor)color alpha:(CGFloat)alpha{
    //正向图片绘制
    UIImage *uiImage = [mImages objectForKey:imagePath];
    CGImageRef imageRef = uiImage.CGImage;
    CGSize imageSize = CGSizeMake(uiImage.size.width / mDesignResolutionScale.x, uiImage.size.height / mDesignResolutionScale.y);
    
    CGRect drawRect = CGRectMake(drawPoint.x - imageSize.width * anchorPoint.x, drawPoint.y - imageSize.height * anchorPoint.y, imageSize.width, imageSize.height);
    
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    //开启变换环境
    CGContextSaveGState(context);
    
    //首先为正确做drawImage做变换，因为core graphics同UIView的坐标系不同，此处做坐标系同一
    CGContextTranslateCTM(context, drawRect.origin.x, self.frame.size.height - drawRect.origin.y);
    CGContextScaleCTM(context, 1.0, -1.0);
    
    //移动画布至锚点
    CGContextTranslateCTM(context, drawRect.size.width * anchorPoint.x, drawRect.size.height * anchorPoint.y);
    
    //首先旋转
    CGAffineTransform transform = CGAffineTransformIdentity;
    if(rotation.x == rotation.y){
        float radius = -ANGLE_TO_RADIUS(rotation.x);
        transform = CGAffineTransformMake(cosf(radius), sinf(radius), -sinf(radius), cosf(radius), 0, 0);
    } else {
        float radiusX = -ANGLE_TO_RADIUS(rotation.x);
        float radiusY = -ANGLE_TO_RADIUS(rotation.y);
        float cx = cosf(radiusX);
        float sx = sinf(radiusX);
        float cy = cosf(radiusY);
        float sy = sinf(radiusY);
        
        float a = cy * transform.a - sx * transform.b;
        float b = sy * transform.a + cx * transform.b;
        float c = cy * transform.c - sx * transform.d;
        float d = sy * transform.c + cx * transform.d;
        float tx = cy * transform.tx - sx * transform.ty;
        float ty = sy * transform.tx + cx * transform.ty;
        
        transform = CGAffineTransformMake(a, b, c, d, tx, ty);
    }
    
    //缩放
    transform = CGAffineTransformScale(transform, scale.x, scale.y);
    
    //应用变换
    CGContextConcatCTM(context, transform);
    
    //移动画布回归至drawPoint的位置
    CGContextTranslateCTM(context, -drawRect.size.width * anchorPoint.x, -drawRect.size.height * anchorPoint.y);//-
    CGContextTranslateCTM(context, -drawRect.origin.x, -drawRect.origin.y);
    
    //图片透明度
    CGContextSetAlpha(context, alpha);
    //设置混合模式
    CGContextSetBlendMode(context, kCGBlendModeNormal);
    //绘制图片
    CGContextDrawImage(context, drawRect, imageRef);
    
    //处理颜色混合，这里是画一个同图片同样大小的rect，通过特殊的颜色混合模式，令图片透明的地方，rect也透明。
    CGContextSetBlendMode(context, kCGBlendModeSourceAtop);
    CGContextSetRGBFillColor(context, color.r, color.g, color.b, color.a);
    CGContextFillRect(context, drawRect);
    
    //关闭变换环境
    CGContextRestoreGState(context);
}

//drawRect
-(void)drawRect:(CGRect)rect{
    BOOL isMainThread = [[NSThread currentThread] isMainThread];
    if((self.runMode == FlashViewRunModeMainThread && !isMainThread) ||
       (self.runMode == FlashViewRunModeBackgroundThread && isMainThread)
       ){
        return;
    }
    if (mRunningAnimName) {
        [self drawRectForAnim:rect];
    }else if(mStopAtAnimName){
        [self drawRectForFrameIndex:mStopAtFrameIndex animName:mStopAtAnimName isTriggerEvent:NO];
    }
}

////被移除时清理数据
//-(void)willMoveToSuperview:(UIView *)newSuperview{
//    if(!newSuperview){
//        [self cleanData];
//    }
//}

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

@end
