//
//  FlashViewImageCache.m
//  FlashView
//
//  Created by kaso on 24/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashViewImageCache.h"


@interface FlashViewImageCache()
@property (nonatomic, strong) NSMutableDictionary<NSString*, UIImage *> *images;
@end

@implementation FlashViewImageCache

+(instancetype) cache{
    static dispatch_once_t onceToken;
    static FlashViewImageCache *sCache = nil;
    dispatch_once(&onceToken, ^{
        sCache = [[FlashViewImageCache alloc] init];
    });
    return sCache;
}

+(void) purge{
    [[FlashViewImageCache cache].images removeAllObjects];
}

-(NSMutableDictionary<NSString *,UIImage *> *)images{
    if (!_images) {
        _images = [[NSMutableDictionary alloc] init];
    }
    return _images;
}

-(void) addImage:(UIImage *)image withName:(NSString *)name{
    if ([self imageWithName:name]) {
        return;
    }
    self.images[name] = image;
}

-(UIImage *) imageWithName:(NSString *) name{
    return self.images[name];
}

@end
