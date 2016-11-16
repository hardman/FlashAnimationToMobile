package com.flashanimation.newAnim.data;

import java.util.ArrayList;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashAnimDataAnim {
    public String animName;
    public int frameCount;
    public ArrayList<FlashAnimDataLayer> layers;
    public void addLayer(FlashAnimDataLayer layer){
        if (layers == null){
            layers = new ArrayList<>();
        }
        layers.add(layer);
    }
}
