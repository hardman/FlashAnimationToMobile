package com.flashanimation.newAnim;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.flashanimation.R;
import com.flashanimation.newAnim.data.FlashAnimData;
import com.flashanimation.view.FlashDataParser;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashViewNew extends ViewGroup implements FlashAnimCommon.FlashViewRender{

    private FlashAnim mAnim;

    public FlashViewNew(Context context) {
        super(context);
        mAnim = new FlashAnim(null, null, null, null, null, 0);
    }

    public FlashViewNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public FlashViewNew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
    }

    /***
     * 加载xml文件中定义的属性
     * @param attrs
     */
    private void initAttrs(AttributeSet attrs){
        TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.FlashView);
        String flashName = arr.getString(R.styleable.FlashView_flashFileName);
        String flashDir = arr.getString(R.styleable.FlashView_flashDir);
        if(flashDir == null){
            flashDir = FlashAnimCommon.DEFAULT_FLASH_DIR;
        }
        String defaultAnimName = arr.getString(R.styleable.FlashView_defaultAnim);

        int designDPI = arr.getInt(R.styleable.FlashView_designDPI, FlashAnimCommon.DEFAULT_FLASH_DPI);//326为iphone5的dpi

        mAnim = new FlashAnim(getContext(), this, flashName, flashDir, defaultAnimName, designDPI);

        arr.recycle();

        play(mAnim.getAnimNames().get(0), 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO: 16/11/16
    }

    @Override
    public void updateToFrameIndex(FlashAnimData animData, String playingAnimName, int frameIndex) {
        // TODO: 16/11/16

    }

    public void play(String animName, int loopTimes) {
        mAnim.play(animName, loopTimes);
    }

    public void play(String animName, int loopTimes, int fromIndex) {
        mAnim.play(animName, loopTimes, fromIndex);
    }

    public void play(String animName, int loopTimes, int fromIndex, int toIndex) {
        mAnim.play(animName, loopTimes, fromIndex, toIndex);
    }

    public void stop() {
        mAnim.stop();
    }

    public void pause() {
        mAnim.pause();
    }

    public void resume() {
        mAnim.resume();
    }
}
