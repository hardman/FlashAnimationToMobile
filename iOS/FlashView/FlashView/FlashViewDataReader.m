/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import "FlashViewDataReader.h"

//读取二进制数据帮助宏
#define READ_DATA(type) \
type ret; \
int size = sizeof(ret); \
[mData getBytes:&ret range:NSMakeRange(mIndex, size)]; \
mIndex += size; \
return ret;

@implementation FlashViewDataReader{
    //当前读取到的位置
    int mIndex;
    //数据源
    NSData *mData;
}

- (instancetype)initWithNSData:(NSData *)data{
    if (self = [super init]) {
        mData = data;
        mIndex = 0;
    }
    return self;
}

-(BOOL) readBool{
    READ_DATA(BOOL);
}

-(uint8_t) readUChar{
    READ_DATA(uint8_t);
}

-(uint16_t) readUShort{
    READ_DATA(uint16_t);
}

-(Float32) readFloat{
    READ_DATA(Float32);
}

//字符串前需要有一个表示字符串长度的数字
-(NSString *)readNSString{
    uint16_t nameLen = [self readUShort];
    NSData * data = [mData subdataWithRange:NSMakeRange(mIndex, nameLen)];
    mIndex += nameLen;
    return [[NSString alloc] initWithData:data encoding: NSUTF8StringEncoding];
}
@end
