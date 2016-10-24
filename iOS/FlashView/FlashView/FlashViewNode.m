//
//  FlashViewAnim.m
//  FlashView
//
//  Created by kaso on 19/10/16.
//  Copyright © 2016年 kaso. All rights reserved.
//

#import "FlashViewNode.h"

@implementation FlashViewBaseNode
-(void)updateToIndex:(NSInteger)index{}
@end
//帧数据
@implementation FlashViewFrameNode
@end

//层数据
@interface FlashViewLayerNode()
@property (nonatomic, strong) UIView *layerView;
@property (nonatomic, strong) NSMutableArray<FlashViewFrameNode *> *keyFrames;
@end

@implementation FlashViewLayerNode

-(UIView *)layerView{
    if (!_layerView) {
        _layerView = [[UIView alloc] init];
    }
    return _layerView;
}

-(void) updateLayerViewWithFrameNode:(FlashViewFrameNode *)frameNode{
}

-(void)updateToIndex:(NSInteger)index{
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
    if (!nextFrameNode) {
    }else{
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

-(void) addLayer:(FlashViewLayerNode *) layer{
    [(NSMutableArray *)self.layers addObject:layer];
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


