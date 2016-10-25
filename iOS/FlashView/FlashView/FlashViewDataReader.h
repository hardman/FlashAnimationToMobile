/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

#import <Foundation/Foundation.h>

@interface FlashViewDataReader : NSObject

- (instancetype)initWithNSData:(NSData *)data;

-(BOOL) readBool;

-(uint16_t) readUShort;

-(Float32) readFloat;

-(uint8_t) readUChar;

-(NSString *) readNSString;

@end
