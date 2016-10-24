//
//  FlashViewImageCache.h
//  FlashView
//
//  Created by kaso on 24/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface FlashViewImageCache : NSObject

+(instancetype) cache;

-(void) addImage:(UIImage *)image withName:(NSString *)name;

-(UIImage *) imageWithName:(NSString *) name;

/**
 *  内存警告或不再使用flashview时，务必调用此方法
 */
+(void) purge;

@end
