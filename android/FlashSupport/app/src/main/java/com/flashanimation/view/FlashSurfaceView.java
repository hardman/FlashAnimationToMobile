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
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.flashanimation.R;

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
public class FlashSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    //flash文件名
    private String mFlashName = null;
    //flash文件目录，可能在Asset中（Assets/[flash dir]/[flash name]），也可能在sdcard中（/sdcard/.[package name]/[flash dir]/[flash name]）。
    private String mFlashDir = FlashDataParser.DEFAULT_FLASH_DIR;

    //下面3个变量为加载完成后默认播放动画时的属性
    private String mDefaultAnimName = null;//默认播放的动画名
    private int mDefaultFromIndex = -1;//起始帧
    private int mDefaultToIndex = -1;//结束帧

    //设计DPI，默认为326，iPhone5s的dpi，制作flash时画布大小为640x1136时不用变，否则需要修改此值。
    //如果不懂此值的意思，请查阅dpi相关的更多资料
    private int mDesignDPI = FlashDataParser.DEFAULT_FLASH_DPI;

    //指定的动画重复次数，默认为1次
    private int mSetLoopTimes = FlashDataParser.FlashLoopTimeOnce;

    //用户解析动画描述文件和一些工具类，所有关键代码都在这里
    private FlashDataParser mDataParser;

    //下面两个变量用于stopAt函数
    private String mStopAtAnimName = null;
    private int mStopAtIndex = 0;

    /***
     *下面3个构造方法可以在纯代码初始化时使用
     */
    public FlashSurfaceView(Context c, String flashName){
        this(c, flashName, FlashDataParser.DEFAULT_FLASH_DIR);
    }

    public FlashSurfaceView(Context c, String flashName, String flashDir){
        this(c, flashName, flashDir, FlashDataParser.DEFAULT_FLASH_DPI);
    }

    public FlashSurfaceView(Context c, String flashName, String flashDir, int designDPI){
        super(c);
        mFlashName = flashName;
        mFlashDir = flashDir;
        mDesignDPI = designDPI;
        init();
    }

    /***
     * 以下3个构造方法为默认构造方法
     */
    public FlashSurfaceView(Context context) {
        super(context);
        init();
    }

    public FlashSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        init();
    }

    public FlashSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        init();
    }

    /***
     * 加载xml文件中定义的属性
     * @param attrs
     */
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

        mDefaultFromIndex = arr.getInt(R.styleable.FlashView_fromIndex, mDefaultFromIndex);
        mDefaultToIndex = arr.getInt(R.styleable.FlashView_toIndex, mDefaultToIndex);
    }

    /***
     * 开始解析数据，并自动播放动画
     * @return
     */
    private boolean init(){
        mDataParser = new FlashDataParser(getContext(), mFlashName, mFlashDir, mDesignDPI);

        if(mSurfaceHolder == null) {
            mSurfaceHolder = getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        if(mFlashRunnable == null) {
            mFlashRunnable = new FlashRunnable();
        }

        if(mDefaultAnimName != null){
            if (mDefaultFromIndex >= 0){
                if (mDefaultToIndex >= 0){
                    play(mDefaultAnimName, mSetLoopTimes, mDefaultFromIndex, mDefaultToIndex);
                }else{
                    play(mDefaultAnimName, mSetLoopTimes, mDefaultFromIndex);
                }
            }else{
                if (mDefaultToIndex >= 0){
                    play(mDefaultAnimName, mSetLoopTimes, 0, mDefaultToIndex);
                }else{
                    play(mDefaultAnimName, mSetLoopTimes);
                }
            }
            pause();
        }

        setZOrderOnTop(true);

        return true;
    }

    /***
     * 可以用此方法重新加载一个新的flash动画文件。
     * @param flashName 动画文件名
     * @return
     */
    public boolean reload(String flashName){
        stop();
        return mDataParser.reload(flashName);
    }

    /***
     * 可以用此方法重新加载一个新的flash动画文件。
     * @param flashName 动画文件名
     * @param flashDir 动画所在文件夹名
     * @return
     */
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
        if(mDrawThread == null || mDrawThread.isInterrupted() || !mDrawThread.isAlive()){
            mDrawThread = new Thread(mFlashRunnable);
            mDrawThread.start();
        }
    }

    public void play(String animName, int loopTimes, int fromIndex){
        play(animName, loopTimes, fromIndex, mDataParser.getParseFrameMaxIndex());
    }

    public void play(String animName, int loopTimes){
        play(animName, loopTimes, 0);
    }

    public void play(){
        play(mDefaultAnimName, mSetLoopTimes);
    }

    /***
     * @return 是否动画正在播放
     */
    public boolean isPlaying(){
        return mDataParser.isPlaying();
    }

    /***
     * @return 是否动画已停止，或还未开始播放
     */
    public boolean isStoped(){
        return mDataParser.isStoped();
    }

    /***
     *
     * @return 是否暂停
     */
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
        if(mDrawThread == null || mDrawThread.isInterrupted() || !mDrawThread.isAlive()){
            mDrawThread = new Thread(mFlashRunnable);
            mDrawThread.start();
        }
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
    public void setScale(float x, float y){
        setScale(x, y, true);
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
        isRealStartDrawImage = false;
        if(mDrawThread != null && mDrawThread.isAlive()) {
            try {
                mDrawThread.join();
            } catch (InterruptedException e) {
                FlashDataParser.log(e);
            }
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


    //下面这些方法是surfaceview的特有方法
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    private boolean isSurfaceCreated = false;
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceCreated = true;
        resume();
        FlashDataParser.log("surface created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceCreated = false;
        pause();
    }

    private SurfaceHolder mSurfaceHolder;
    private Thread mDrawThread;
    private FlashRunnable mFlashRunnable;
    private boolean isRealStartDrawImage;
    private class FlashRunnable implements Runnable{
        private double getCurrTime(){
            return System.currentTimeMillis() / 1000.0;
        }
        @Override
        public void run() {
            synchronized (this) {
                double lastUpdateTime = -1;
                while (!isStoped()) {
                    if (isPaused() || !isSurfaceCreated || !isShown()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            FlashDataParser.log(e);
                        }
                        isRealStartDrawImage = false;
                        continue;
                    }

                    if (!isRealStartDrawImage){
                        lastUpdateTime = -1;
                    }

                    double currTime = getCurrTime();
                    if (lastUpdateTime != -1) {
                        mDataParser.increaseTotalTime(currTime - lastUpdateTime);
                    }
                    lastUpdateTime = currTime;

                    while(!update());

                    try {
                        short threshold = 100;//让出的调度时间
                        double interval = getCurrTime() - lastUpdateTime;//本次循环所消耗的
                        long sleepTime = (long) (mDataParser.getOneFrameTime() * 1000 - interval - threshold);//需要延迟的时间
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                        }
                    } catch (InterruptedException e) {
                        FlashDataParser.log(e);
                    }
                }
                while(!update());//for clean screen
            }
        }

        private boolean update(){
            boolean ret = false;
            if (!isSurfaceCreated){
                return ret;
            }
            Canvas c = null;
            try {
                synchronized (mSurfaceHolder){
                    c = mSurfaceHolder.lockCanvas();
                    if(c == null){
                        FlashDataParser.log("[ERROR] canvas is null in update()");
                    } else {
                        if(isStoped()){
                            mDataParser.cleanScreen(c);
                            ret = true;
                        }else {
                            isRealStartDrawImage = true;
                            mDataParser.cleanScreen(c);
                            if(!mDataParser.drawCanvas(c) && mStopAtAnimName != null){
                                mDataParser.drawCanvas(c, mStopAtIndex, mStopAtAnimName, false);
                            }
                            ret = true;
                        }
                    }
                }
            }catch (Exception e){
                FlashDataParser.log(e);
            }finally {
                if (c != null){
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
            return ret;
        }
    }
}
