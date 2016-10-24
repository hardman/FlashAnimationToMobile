//
//  FlashViewAnim.m
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashViewNode.h"
#import "FlashViewImageCache.h"

@implementation FlashViewBaseNode
-(void)updateToIndex:(NSInteger)index{}
-(void)trigerEventWithIndex:(NSInteger)index{}
@end
//帧数据
@implementation FlashViewFrameNode
@end

//层数据
@interface FlashViewLayerNode()
@property (nonatomic, weak) UIView *baseView;
@property (nonatomic, strong) CALayer *layer;
@property (nonatomic, strong) NSMutableArray<FlashViewFrameNode *> *keyFrames;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, FlashViewFrameNode *> *frameDict;
@end

@implementation FlashViewLayerNode

-(NSMutableDictionary<NSNumber *,FlashViewFrameNode *> *)frameDict{
    if (!_frameDict) {
        _frameDict = [[NSMutableDictionary alloc] init];
    }
    return _frameDict;
}

-(CALayer *)layer{
    if (!_layer) {
        _layer = [[CALayer alloc] init];
    }
    return _layer;
}

-(NSNumber *)getPerValue: (FlashViewFrameNode *)new old:(FlashViewFrameNode*)old key:(NSString *)key per:(float) per{
    float oldValue = [[old valueForKey:key] floatValue];
    float newValue = [[new valueForKey:key] floatValue];
    float ret = -1;
    //计算跨度
    float span = fabsf(newValue - oldValue);
    if (span > 180 && ([key isEqualToString:@"skewX"] || [key isEqualToString:@"skewY"])) {//说明2个值不能直接变化需要跳板
        float realSpan = 360 - span;
        float mark = (oldValue < 0) ? -1 : 1;
        float mid = 180 * mark;
        float newStart = -mid;
        float midPer = (mid - oldValue) / realSpan;
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

-(FlashViewFrameNode *)frameNodeWithIndex:(NSInteger) index{
    FlashViewFrameNode *targetFrameNode = self.frameDict[@(index)];
    if (!targetFrameNode) {
        FlashViewFrameNode *currFrameNode = nil;
        FlashViewFrameNode *nextFrameNode = nil;
        for (int i = 0; i < self.keyFrames.count; i++) {
            FlashViewFrameNode *frameNode = self.keyFrames[i];
            if (index >= frameNode.frameIndex) {
                currFrameNode = frameNode;
                if (i < self.keyFrames.count - 1) {
                    nextFrameNode = self.keyFrames[i + 1];
                }
                break;
            }
        }
        if (index <= currFrameNode.frameIndex + currFrameNode.duration) {
            if (!nextFrameNode || !currFrameNode.isTween) {
                targetFrameNode = currFrameNode;
            }else{
                CGFloat per = (index - currFrameNode.frameIndex) * 1.0 / (nextFrameNode.frameIndex - currFrameNode.frameIndex);
                targetFrameNode = [[FlashViewFrameNode alloc] init];
                targetFrameNode.x = [[self getPerValue:nextFrameNode old:currFrameNode key:@"x" per:per] floatValue];
                targetFrameNode.y = [[self getPerValue:nextFrameNode old:currFrameNode key:@"y" per:per] floatValue];
                targetFrameNode.scaleX = [[self getPerValue:nextFrameNode old:currFrameNode key:@"scaleX" per:per] floatValue];
                targetFrameNode.scaleY = [[self getPerValue:nextFrameNode old:currFrameNode key:@"scaleY" per:per] floatValue];
                
                targetFrameNode.skewX = [[self getPerValue:nextFrameNode old:currFrameNode key:@"skewX" per:per] floatValue];
                targetFrameNode.skewY = [[self getPerValue:nextFrameNode old:currFrameNode key:@"skewY" per:per] floatValue];
                
                targetFrameNode.alpha = [[self getPerValue:nextFrameNode old:currFrameNode key:@"alpha" per:per] floatValue];
                targetFrameNode.r = [[self getPerValue:nextFrameNode old:currFrameNode key:@"r" per:per] charValue];
                targetFrameNode.g = currFrameNode.g + (nextFrameNode.g - currFrameNode.g) * per;
                targetFrameNode.b = currFrameNode.b + (nextFrameNode.b - currFrameNode.b) * per;
                targetFrameNode.a = currFrameNode.a + (nextFrameNode.a - currFrameNode.a) * per;
            }
        }
        self.frameDict[@(index)] = targetFrameNode;
    }
    
    return targetFrameNode;
}

-(void) updateLayerViewWithFrameNode:(FlashViewFrameNode *)frameNode{
    if (!frameNode) {
        [self.layer removeFromSuperlayer];
        return;
    }
    CALayer *layer = self.layer;
    if (!layer.superlayer) {
        [self.baseView.layer addSublayer:layer];
    }
    if (!self.imageName || ![frameNode.imageName isEqualToString:self.imageName]) {
        FlashViewImageCache *cache = [FlashViewImageCache cache];
        UIImage *image = [cache imageWithName:frameNode.imageName];
        if (!image) {
            image = [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@/%@", cache.imagePath, frameNode.imageName]];
            [cache addImage:image withName:frameNode.imageName];
        }
        layer.contents = (__bridge id _Nullable)(image.CGImage);
        self.imageName = frameNode.imageName;
    }
    CATransform3D t = CATransform3DIdentity;
    t.m11 = frameNode.scaleX;
    t.m12 = frameNode.skewX;
    t.m13 = 0;
    t.m14 = 0;
    t.m21 = frameNode.skewY;
    t.m22 = frameNode.scaleX;
    t.m23 = 0;
    t.m24 = 0;
    t.m31 = 0;
    t.m32 = 0;
    t.m33 = 1;
    t.m34 = 0;
    t.m41 = frameNode.x;
    t.m42 = frameNode.y;
    t.m43 = self.index;
    t.m44 = 1;
    layer.transform = t;
}

-(void)updateToIndex:(NSInteger)index{
    [self updateLayerViewWithFrameNode:[self frameNodeWithIndex:index]];
}

-(void)trigerEventWithIndex:(NSInteger)index{
    FlashViewFrameNode *frameNode = [self frameNodeWithIndex:index];
    if (frameNode.mark && frameNode.mark) {
        NSLog(@"trigger mark %@", frameNode.mark);
    }
}

-(void)addKeyFrame:(FlashViewFrameNode *)keyFrame{
    [(NSMutableArray *)self.keyFrames addObject:keyFrame];
}

-(NSMutableArray<FlashViewFrameNode *> *)keyFrames{
    if (!_keyFrames) {
        _keyFrames = [[NSMutableArray alloc] init];
    }
    return _keyFrames;
}
@end

//单个动画数据
@interface FlashViewAnimNode()
@property (nonatomic, strong) NSMutableArray<FlashViewLayerNode *> *layers;
@end

@implementation FlashViewAnimNode

-(void) addLayer:(FlashViewLayerNode *) layer baseView:(UIView *)baseView{
    [(NSMutableArray *)self.layers addObject:layer];
    if (!layer.baseView) {
        layer.baseView = baseView;
    }
}

-(NSMutableArray<FlashViewLayerNode *> *)layers{
    if (!_layers) {
        _layers = [[NSMutableArray alloc] init];
    }
    return _layers;
}

-(void)updateToIndex:(NSInteger)index{
    for (FlashViewLayerNode *layerNode in self.layers) {
        [layerNode updateToIndex:index];
    }
}

-(void)trigerEventWithIndex:(NSInteger)index{
    for (FlashViewLayerNode *layerNode in self.layers) {
        [layerNode trigerEventWithIndex:index];
    }
}

@end

//所有动画
@interface FlashViewNode()
@property (nonatomic, strong) NSMutableDictionary<NSString *, FlashViewAnimNode*> *anims;
@end

@implementation FlashViewNode

-(void) addAnim:(FlashViewAnimNode *)anim{
    [(NSMutableDictionary *)self.anims setObject:anim forKey:anim.animName];
}

-(NSMutableDictionary<NSString *,FlashViewAnimNode *> *)anims{
    if (!_anims) {
        _anims = [[NSMutableDictionary alloc] init];
    }
    return _anims;
}
@end


