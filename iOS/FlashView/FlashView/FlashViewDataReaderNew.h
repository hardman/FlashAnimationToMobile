//
//  FlashViewDataReaderNew.h
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface FlashViewDataReaderNew : NSObject

- (instancetype)initWithNSData:(NSData *)data;

-(BOOL) readBool;

-(uint16_t) readUShort;

-(Float32) readFloat;

-(uint8_t) readUChar;

-(NSString *) readNSString;

@end
