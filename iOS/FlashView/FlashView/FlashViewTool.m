/*
 copyright 2016 wanghongyu.
 The project page：https://github.com/hardman/FlashAnimationToMobile
 My blog page: http://blog.csdn.net/hard_man/
 */

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
    if (self.images[name]) {
        return;
    }
    self.images[name] = image;
}

-(void) replaceImage:(UIImage *)image withName:(NSString *)name{
    self.images[name] = image;
}

-(UIImage *) imageWithName:(NSString *) name{
    UIImage * image = self.images[name];
    if (!image) {
        image = [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@/%@", self.imagePath, name]];
        [self addImage:image withName:name];
    }
    return image;
}

+(UIImage *)imageWithColor:(UIColor *)color size:(CGSize)size{
    CGRect rect=CGRectMake(0, 0, size.width, size.height);
    UIGraphicsBeginImageContext(rect.size);
    CGContextRef context=UIGraphicsGetCurrentContext();
    CGContextSetFillColorWithColor(context, color.CGColor);
    CGContextFillRect(context, rect);
    UIImage *img=UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return img;
}

-(UIImage *)colorOverlayImageWithColor:(UIColor *) color srcImgName:(NSString *)srcImgName{
    CIFilter *blendFilter = [CIFilter filterWithName:@"CISourceAtopCompositing"];
    [blendFilter setDefaults];
    
    UIImage *srcImg = [self imageWithName:srcImgName];
    UIImage *colorOverlayImage = [self.class imageWithColor:color size:srcImg.size];
    
    [blendFilter setValue:[CIImage imageWithCGImage:colorOverlayImage.CGImage] forKey:kCIInputImageKey];
    [blendFilter setValue:[CIImage imageWithCGImage:srcImg.CGImage]forKey:kCIInputBackgroundImageKey];
    
    CIImage *filterOutputImg = blendFilter.outputImage;
    CIContext *ciContext = [CIContext contextWithOptions:nil];
    CGImageRef cgImg = [ciContext createCGImage:filterOutputImg fromRect:filterOutputImg.extent];
    
    return [UIImage imageWithCGImage:cgImg];
}

@end
