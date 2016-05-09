/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

package com.flashanimation.view;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.flashanimation.R;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wanghongyu on 10/12/15.
 * usage:
 <?xml version="1.0" encoding="utf-8"?>
 <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
 xmlns:tools="http://schemas.android.com/tools"
 xmlns:FlashView="http://schemas.android.com/apk/res-auto"
 android:layout_width="match_parent"
 android:layout_height="match_parent"
 tools:context="com.xcyo.yoyo.flashsupport.MainActivity">

 <com.xcyo.yoyo.flashsupport.view.FlashView
 android:layout_width="match_parent"
 android:layout_height="match_parent"
 FlashView:flashDir="flashAnims"
 FlashView:flashFileName="bieshu"
 FlashView:defaultAnim="bieshu"
 FlashView:designDPI="326"
 FlashView:loopTimes="0"
 android:id="@+id/flashview"
 />

 </RelativeLayout>

 */
public class FlashView extends View {
    private String mFlashName = null;
    private String mFlashDir = FlashDataParser.DEFAULT_FLASH_DIR;
    private String mDefaultAnimName = null;
    private String mStopAtAnimName = null;
    private int mStopAtIndex = 0;
    private int mDesignDPI = FlashDataParser.DEFAULT_FLASH_DPI;

    private int mSetLoopTimes = FlashDataParser.FlashLoopTimeOnce;

    private FlashDataParser mDataParser;

    //使用new初始化使用
    public FlashView(Context c, String flashName){
        this(c, flashName, FlashDataParser.DEFAULT_FLASH_DIR);
    }

    public FlashView(Context c, String flashName, String flashDir){
        this(c, flashName, flashDir, FlashDataParser.DEFAULT_FLASH_DPI);
    }

    public FlashView(Context c, String flashName, String flashDir, int designDPI){
        super(c);
        mFlashName = flashName;
        mFlashDir = flashDir;
        mDesignDPI = designDPI;
        init();
    }

    public FlashView(Context context) {
        super(context);
        init();
    }

    public FlashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        init();
    }

    public FlashView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        init();
    }

    private void initAttrs(AttributeSet attrs){
        TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.FlashView);
        mFlashName = arr.getString(R.styleable.FlashView_flashFileName);
        mFlashDir = arr.getString(R.styleable.FlashView_flashDir);
        if(mFlashDir == null){
            mFlashDir = FlashDataParser.DEFAULT_FLASH_DIR;
        }
        mDefaultAnimName = arr.getString(R.styleable.FlashView_defaultAnim);
        mSetLoopTimes = arr.getInt(R.styleable.FlashView_loopTimes, FlashDataParser.FlashLoopTimeOnce);

        mDesignDPI = arr.getInt(R.styleable.FlashView_designDPI, FlashDataParser.DEFAULT_FLASH_DPI);//326为iphone5的dpi
    }

    private boolean init(){
        mDataParser = new FlashDataParser(getContext(), mFlashName, mFlashDir, mDesignDPI);

        if(mDefaultAnimName != null){
            play(mDefaultAnimName, mSetLoopTimes);
        }

        return true;
    }

    public boolean reload(String flashName){
        stop();
        return mDataParser.reload(flashName);
    }

    public boolean reload(String flashName, String flashDir){
        stop();
        return mDataParser.reload(flashName, flashDir);
    }

    /***
     * 使用这个对象，重新加载一个新的动画
     * @param flashName: 动画名
     * @param flashDir: 动画目录
     * @param designDPI: 设计dpi 默认iphone5 为326
     * @return
     */
    public boolean reload(String flashName, String flashDir, int designDPI){
        stop();
        return mDataParser.reload(flashName, flashDir, designDPI);
    }

    /***
     * 事件回调
     * @param callback
     */
    public void setEventCallback(FlashDataParser.IFlashViewEventCallback callback){
        mDataParser.setEventCallback(callback);
    }

    /***
     * 替换动画中所有相同图片
     * @param texName: 对应的图片名
     * @param bitmap: 新的Bitmap
     */
    public void replaceBitmap(String texName, Bitmap bitmap){
        mDataParser.replaceBitmap(texName, bitmap);
    }

    /***
     *
     * 播放动画
     * @param animName: 动画名
     * @param loopTimes: 循环次数，0表示无限循环
     * @param fromIndex: 从第几帧开始播放
     * @param toIndex: 播放到第几帧结束
     */
    public void play(String animName, int loopTimes, int fromIndex, int toIndex) {
        mDataParser.play(animName, loopTimes, fromIndex, toIndex);
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleAtFixedRate(mUpdateRunnable, 0, (int) (mDataParser.getOneFrameTime() * 1000000), TimeUnit.MICROSECONDS);
    }

    private Handler mHandler = new Handler();

    //暂时没用，因为这样会让第一次显示的比较慢，起初的10几帧都不见了。
    private Runnable mUpdateOnMainThreadRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.post(mUpdateRunnable);
        }
    };

    private ScheduledExecutorService mScheduledExecutorService;
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShown() || isPaused() || isStoped()){
                return;
            }
            mDataParser.increaseTotalTime(mDataParser.getOneFrameTime());
            postInvalidate();
        }
    };

    public void play(String animName, int loopTimes, int fromIndex){
        play(animName, loopTimes, fromIndex, mDataParser.getParseFrameMaxIndex());
    }

    public void play(String animName, int loopTimes){
        play(animName, loopTimes, 0);
    }

    public void play(){
        play(mDefaultAnimName, mSetLoopTimes);
    }

    public boolean isPlaying(){
        return mDataParser.isPlaying();
    }

    public boolean isStoped(){
        return mDataParser.isStoped();
    }

    public boolean isPaused(){
        return mDataParser.isPaused();
    }

    /***
     * 显示动画的某一帧，这个不同于pause，此函数在动画停止时强制显示某一帧。
     * @param animName：动画名
     * @param index：停在哪一帧
     */
    public void stopAt(String animName, int index){
        stop();
        mStopAtAnimName = animName;
        mStopAtIndex = index;
        postInvalidate();
    }

    public void setScale(float x, float y){
        setScale(x, y, true);
    }

    /***
     * 设置图像的scale
     * @param x: scale x
     * @param y: scale y
     * @param isDpiEffect: 是否乘以dpi
     */
    public void setScale(float x, float y, boolean isDpiEffect){
        mDataParser.setScale(x, y, isDpiEffect);
    }

    /***
     * 获取动画帧数
     * @return 动画帧数
     */
    public int getLength(){
        return mDataParser.getLength();
    }

    /***
     * 停止动画
     */
    public void stop(){
        mDataParser.stop();
        if(mScheduledExecutorService != null && !mScheduledExecutorService.isTerminated() && !mScheduledExecutorService.isShutdown()) {
            mScheduledExecutorService.shutdown();
        }
        mStopAtAnimName = null;
        mStopAtIndex = 0;
    }

    /***
     * 暂停
     */
    public void pause(){
        mDataParser.pause();
    }

    /***
     * 恢复
     */
    public void resume(){
        mDataParser.resume();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!mDataParser.drawCanvas(canvas) && mStopAtAnimName != null){
            mDataParser.drawCanvas(canvas, mStopAtIndex, mStopAtAnimName, false);
        }
    }
}
