package com.flashanimation.newAnim.data;

import java.util.HashMap;

/**
 * Created by wanghongyu on 16/11/16.
 */

public class FlashAnimData {
    public int frameRate;
    public long oneFrameDurationMs;
    public HashMap<String, FlashAnimDataAnim> anims;
    public void addAnim(FlashAnimDataAnim anim){
        if (anims == null){
            anims = new HashMap<>();
        }
        anims.put(anim.animName, anim);
    }
}

