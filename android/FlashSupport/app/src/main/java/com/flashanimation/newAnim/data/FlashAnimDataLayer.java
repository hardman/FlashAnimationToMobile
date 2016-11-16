package com.flashanimation.newAnim.data;

import android.util.Size;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashAnimDataLayer {
    public int index;
    public String animName;
    public String imageName;
    public Size imageSize;
    public ArrayList<FlashAnimDataKeyFrame> keyFrames;
    public void addKeyFrame(FlashAnimDataKeyFrame keyFrame){
        if (keyFrames == null){
            keyFrames = new ArrayList<>();
        }
        keyFrames.add(keyFrame);
    }

    public HashMap<Integer, FlashAnimDataKeyFrame> generatedKeyFrameDict;
    public void genTweenAnimData() {
        if (generatedKeyFrameDict != null){
            return;
        }
        generatedKeyFrameDict = new HashMap<>();
        for (int i = 0; i < keyFrames.size(); i++){
            FlashAnimDataKeyFrame currKeyFrameData = keyFrames.get(i);
            FlashAnimDataKeyFrame nextKeyFrameData = null;
            if (currKeyFrameData.isTween && i < keyFrames.size() - 1){
                nextKeyFrameData = keyFrames.get(i + 1);
            }
            for (int j = currKeyFrameData.frameIndex; j < currKeyFrameData.frameIndex + currKeyFrameData.duration; j++){
                FlashAnimDataKeyFrame targetKeyFrameData = null;
                if (nextKeyFrameData == null){
                    targetKeyFrameData = currKeyFrameData.clone();
                    if (j != currKeyFrameData.frameIndex){
                        targetKeyFrameData.mark = null;
                    }
                }else{
                    float per = (j - currKeyFrameData.frameIndex) * 1.0f / (nextKeyFrameData.frameIndex - currKeyFrameData.frameIndex);
                    targetKeyFrameData = new FlashAnimDataKeyFrame();
                    targetKeyFrameData.x = getMidValue(currKeyFrameData.x, nextKeyFrameData.x, per);
                    targetKeyFrameData.y = getMidValue(currKeyFrameData.y, nextKeyFrameData.y, per);
                    targetKeyFrameData.scaleX = getMidValue(currKeyFrameData.scaleX, nextKeyFrameData.scaleX, per);
                    targetKeyFrameData.scaleY = getMidValue(currKeyFrameData.scaleY, nextKeyFrameData.scaleY, per);
                    targetKeyFrameData.skewX = getMidValueForSkew(currKeyFrameData.skewX, nextKeyFrameData.skewX, per);
                    targetKeyFrameData.skewY = getMidValueForSkew(currKeyFrameData.skewY, nextKeyFrameData.skewY, per);
                    targetKeyFrameData.alpha = getMidValue(currKeyFrameData.alpha, nextKeyFrameData.alpha, per);
                    targetKeyFrameData.r = (short)getMidValue(currKeyFrameData.r, nextKeyFrameData.r, per);
                    targetKeyFrameData.g = (short)getMidValue(currKeyFrameData.g, nextKeyFrameData.g, per);
                    targetKeyFrameData.b = (short)getMidValue(currKeyFrameData.b, nextKeyFrameData.b, per);
                    targetKeyFrameData.a = (short)getMidValue(currKeyFrameData.a, nextKeyFrameData.x, per);

                    targetKeyFrameData.imageName = currKeyFrameData.imageName;

                    if (j == currKeyFrameData.frameIndex){
                        targetKeyFrameData.mark = currKeyFrameData.mark;
                    }
                }

                generatedKeyFrameDict.put(j, targetKeyFrameData);

                targetKeyFrameData.calcTransformMatrix();
            }
        }
    }

    private float getMidValueForSkew(float newValue, float oldValue, float per){
        float ret = -1;
        float span = Math.abs(newValue - oldValue);
        if (span > 180){
            float realSpan = 360 - span;
            float mark = (oldValue < 0) ? -1: 1;
            float mid = 180 * mark;
            float newStart = -mid;
            float midPer = Math.abs(mid - oldValue) / realSpan;
            if (per < midPer){
                ret = oldValue + per * realSpan * mark;
            }else{
                ret = newStart + (per - midPer) * realSpan * mark;
            }
        }else{
            ret = getMidValue(newValue, oldValue, per);
        }

        return ret;
    }

    private float getMidValue(float newValue, float oldValue, float per){
        return oldValue + per * (newValue - oldValue);
    }
}
