//
//  FlashViewTool.m
//  FlashView
//
//  Created by kaso on 24/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashViewTool.h"


@interface FlashViewTool()
@property (nonatomic, strong) NSMutableDictionary<NSString*, UIImage *> *images;
@end

@implementation FlashViewTool
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

-(void) replaceImage:(UIImage *)image withName:(NSString *)name{
    self.images[name] = image;
}

-(UIImage *) imageWithName:(NSString *) name{
    return self.images[name];
}

@end
