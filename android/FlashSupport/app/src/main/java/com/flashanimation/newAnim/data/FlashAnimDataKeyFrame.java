package com.flashanimation.newAnim.data;

import android.graphics.Matrix;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashAnimDataKeyFrame implements Cloneable {
    public int frameIndex;
    public boolean isEmpty;
    public boolean isTween;
    public int duration;
    public String imageName;
    public float x;
    public float y;
    public float scaleX;
    public float scaleY;
    public float skewX;
    public float skewY;
    public String mark;
    public float alpha;
    public short r;
    public short g;
    public short b;
    public short a;

    public Matrix matrix;

    @Override
    protected FlashAnimDataKeyFrame clone(){
        FlashAnimDataKeyFrame newData = new FlashAnimDataKeyFrame();
        newData.frameIndex = frameIndex;
        newData.isEmpty = isEmpty;
        newData.isTween = isTween;
        newData.duration = duration;
        newData.imageName = imageName;
        newData.x = x;
        newData.y = y;
        newData.scaleX = scaleX;
        newData.scaleY = scaleY;
        newData.skewX = skewX;
        newData.skewY = skewY;
        newData.mark = mark;
        newData.alpha = alpha;
        newData.r = r;
        newData.g = g;
        newData.b = b;
        newData.a = a;
        return newData;
    }

    private float angleToRadius(float angle){
        return 0.01745329252f * angle;
    }

    public void calcTransformMatrix(){
        Matrix matrix = new Matrix();
        matrix.postSkew(skewX, skewY);
        matrix.postScale(scaleX, scaleY);
        matrix.postTranslate(x, y);
        this.matrix = matrix;
    }
}
