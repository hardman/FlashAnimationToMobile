package com.flashanimation.newAnim;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import com.flashanimation.newAnim.FlashAnimCommon.*;
import com.flashanimation.newAnim.data.FlashAnimData;
import com.flashanimation.newAnim.data.FlashAnimDataAnim;
import com.flashanimation.newAnim.data.FlashAnimDataKeyFrame;
import com.flashanimation.newAnim.data.FlashAnimDataLayer;
import com.flashanimation.newAnim.data.FlashDataReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashAnim{
    //flash文件名
    private String mFlashName = null;
    //flash文件目录，可能在Asset中（Assets/[flash dir]/[flash name]），也可能在sdcard中（/sdcard/.[package name]/[flash dir]/[flash name]）。
    private String mFlashDir = null;

    //下面3个变量为加载完成后默认播放动画时的属性
    private String mDefaultAnimName = null;//默认播放的动画名

    private int mFromIndex = -1;//起始帧
    private int mToIndex = -1;//结束帧

    //设计DPI，默认为326，iPhone5s的dpi，制作flash时画布大小为640x1136时不用变，否则需要修改此值。
    //如果不懂此值的意思，请查阅dpi相关的更多资料
    private int mDesignDPI = 0;

    //构造函数
    public FlashAnim(Context ctx, FlashViewRender render, String flashName, String flashDir, String defaultAnimName, int designDpi){
        if (flashName == null || flashDir == null){
            FlashAnimCommon.log("[E] FlashAnim construct failed flashName/flashDir is null");
            return;
        }
        mContext = ctx;
        mRender = render;
        mFlashName = flashName;
        mFlashDir = flashDir;
        mDefaultAnimName = defaultAnimName;
        mDesignDPI = designDpi;
        if(!init()){
            FlashAnimCommon.log("[Error] FlashAnim construct failed");
        }
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case HANDLER_MSG_TYPE_ON_EVENT:
                        onEvent(FlashViewEvent.values()[msg.arg1], msg.obj);
                        break;
                    case HANDLER_MSG_TYPE_UPDATE_FRAME:
                        updateToFrameIndex(msg.arg1);
                        break;
                }
                return true;
            }
        });
    }

    public final int HANDLER_MSG_TYPE_UPDATE_FRAME = 0;
    public final int HANDLER_MSG_TYPE_ON_EVENT = 1;

    private Handler mHandler;

    //构造函数
    public FlashAnim(Context ctx, FlashViewRender render, String flashName, String defaultAnimName){
        this(ctx, render, flashName, FlashAnimCommon.DEFAULT_FLASH_DIR, defaultAnimName, FlashAnimCommon.DEFAULT_FLASH_DPI);
    }

    //用于获取Resource和Assets等资源
    private Context mContext;

    private boolean isInitOk;
    //回调事件
    private FlashAnimCommon.IFlashViewEventCallback mEventCallback;

    public void setEventCallback(IFlashViewEventCallback eventCallback) {
        this.mEventCallback = eventCallback;
    }

    private void onEvent(FlashViewEvent e, Object data){
        FlashAnimCommon.log("[D] FlashAnim.onEvent " + e);
        if (mEventCallback != null){
            mEventCallback.onEvent(e, data);
        }
    }

    private void onEventInMainThread(FlashViewEvent e, Object data){
        Message msg = Message.obtain();
        msg.what = HANDLER_MSG_TYPE_ON_EVENT;
        msg.arg1 = e.ordinal();
        msg.obj = data;
        mHandler.sendMessage(msg);
    }

    //文件类型是在sdcard还是在assets
    private FlashFileLocationType mFileLocation = FlashFileLocationType.NONE;

    //文件数据格式
    private FlashFileType mFileType = FlashFileType.NONE;

    //...文件在sd卡中的路径
    private String mSdcardPath = null;

    private FlashAnimCommon mAnimCommon;

    /***
     * 猜测动画文件的类型，并检查需要的文件／文件夹是否存在
     * @param assetFilePathPre 文件前缀（文件名除后缀）
     * @param fileDataType 文件数据类型
     * @return
     */
    private boolean guessFileType(String assetFilePathPre, FlashFileType fileDataType){
        String assetFilePathExt = FlashAnimCommon.JSONEXT;
        if(fileDataType == FlashFileType.BIN){
            assetFilePathExt = FlashAnimCommon.BINEXT;
        }
        try {
            mContext.getAssets().open(assetFilePathPre + assetFilePathExt);
            mFileLocation = FlashFileLocationType.ASSETS;
            mFileType = fileDataType;
            return true;
        }catch(Exception e){
            if (mSdcardPath == null){
                mSdcardPath = FlashAnimCommon.getExternalStorageDirectory(mContext);
            }
            File flashDirFile = new File(mSdcardPath + "/" + mFlashDir);
            if(flashDirFile.isDirectory()){
                String sdcardFilePath = mSdcardPath + "/" + assetFilePathPre + assetFilePathExt;
                File sdcardFile = new File(sdcardFilePath);
                if(sdcardFile.isFile()){
                    mFileLocation = FlashFileLocationType.SDCARD;
                    mFileType = fileDataType;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean guessFileType(){
        String assetFilePathPre = mFlashDir + "/" + mFlashName;
        return guessFileType(assetFilePathPre, FlashFileType.JSON) || guessFileType(assetFilePathPre, FlashFileType.BIN);
    }

    //原始动画数据
    private boolean isPlaying = false;
    private float mDPIRate;
    private float mScaleX, mScaleY;

    public boolean init(){
        isInitOk = false;
        if(mFlashName == null || mFlashDir == null){
            FlashAnimCommon.log("[ERROR] mFlashName/mFlashDir is null");
            return false;
        }

        if (mAnimCommon == null){
            mAnimCommon = new FlashAnimCommon(mContext);
        }

        if (!guessFileType() || mFileType == FlashFileType.NONE){
            FlashAnimCommon.log("[ERROR] file is not found in assets and sdcard");
            return false;
        }

        mAnimCommon.setLocationType(mFileLocation);

        if(mFileType == FlashFileType.JSON) {
            if (!parseJsonFile()){
                FlashAnimCommon.log("[Error] flajson file parse error");
                return false;
            }
        }else{
            if (!parseBinFile()){
                FlashAnimCommon.log("[Error] flabin file parse error");
                return false;
            }
        }

        isPlaying = false;

        mDPIRate = 1.0f * mContext.getResources().getDisplayMetrics().densityDpi / mDesignDPI;

        isInitOk = true;

        FlashAnimCommon.log("[D] 初始化成功！");

        return true;
    }

    /***
     * 设置图像的scale
     * @param x: scale x
     * @param y: scale y
     * @param isDpiEffect: 是否乘以dpi
     */
    public void setScale(float x, float y, boolean isDpiEffect){
        if(isDpiEffect){
            mScaleX = mDPIRate * x;
            mScaleY = mDPIRate * y;
        }else{
            mScaleX = x;
            mScaleY = y;
        }
    }

    public void setScale(float x, float y){
        setScale(x, y, true);
    }

    private FlashAnimData mAnimData;

    private boolean parseJsonFile(){
        JSONObject jsonObject = mAnimCommon.readJson(mFlashDir + "/" + mFlashName + FlashAnimCommon.JSONEXT);
        if (jsonObject == null){
            FlashAnimCommon.log("[E] read json file failed");
            return false;
        }

        try {
            int frameRate = jsonObject.getInt("frameRate");
            long oneFrameTimeMs = 1000 / frameRate;

            if (mAnimData == null){
                mAnimData = new FlashAnimData();
                mAnimData.oneFrameDurationMs = oneFrameTimeMs;
                mAnimData.frameRate = frameRate;
            }

            //图片
            JSONArray textureArr = jsonObject.getJSONArray("textures");
            for (int i = 0; i < textureArr.length(); i++){
                String texName = textureArr.getString(i);
                Bitmap bitmap = mAnimCommon.readImage(mFlashDir + "/" + mFlashName + "/" + texName);
                mAnimCommon.addImage(texName, bitmap);
            }

            //读取动画
            JSONArray jsonAnims = jsonObject.getJSONArray("anims");
            for (int j = 0; j < jsonAnims.length(); j++){
                JSONObject animObj = jsonAnims.getJSONObject(j);
                FlashAnimDataAnim oneAnim = new FlashAnimDataAnim();
                oneAnim.animName = animObj.getString("animName");
                oneAnim.frameCount = animObj.getInt("frameMaxNum");
                mAnimData.addAnim(oneAnim);
                JSONArray layerArr = animObj.getJSONArray("layers");
                for (int k = 0; k < layerArr.length(); k++){
                    JSONObject layerObj = layerArr.getJSONObject(k);
                    FlashAnimDataLayer animLayer = new FlashAnimDataLayer();
                    animLayer.index = layerArr.length() - k;
                    animLayer.animName = oneAnim.animName;
                    oneAnim.addLayer(animLayer);
                    JSONArray keyFrameArr = layerObj.getJSONArray("frames");
                    for (int l = 0; l < keyFrameArr.length(); l++){
                        JSONObject keyFrameObj = keyFrameArr.getJSONObject(l);
                        FlashAnimDataKeyFrame keyFrame = new FlashAnimDataKeyFrame();
                        animLayer.addKeyFrame(keyFrame);
                        keyFrame.frameIndex = keyFrameObj.getInt("frameIndex");
                        keyFrame.isEmpty = keyFrameObj.getBoolean("isEmpty");
                        if (!keyFrame.isEmpty){
                            keyFrame.duration = keyFrameObj.getInt("duration");
                            keyFrame.isTween = keyFrameObj.getBoolean("isTween");
                            keyFrame.imageName = keyFrameObj.getString("texName");
                            keyFrame.x = (float)keyFrameObj.getDouble("x");
                            keyFrame.y = (float)keyFrameObj.getDouble("y");
                            keyFrame.scaleX = (float)keyFrameObj.getDouble("scaleX");
                            keyFrame.scaleY = (float)keyFrameObj.getDouble("scaleY");
                            keyFrame.skewX = (float)keyFrameObj.getDouble("skewX");
                            keyFrame.skewY = (float)keyFrameObj.getDouble("skewY");
                            keyFrame.mark = keyFrameObj.getString("mark");
                            keyFrame.alpha = (float)keyFrameObj.getDouble("alpha");

                            JSONObject colorObj = keyFrameObj.getJSONObject("color");
                            keyFrame.r = (short)colorObj.getInt("r");
                            keyFrame.g = (short)colorObj.getInt("g");
                            keyFrame.b = (short)colorObj.getInt("b");
                            keyFrame.a = (short)colorObj.getInt("a");
                        }else{
                            keyFrame.duration = 1;
                        }
                    }

                    //layer 准备计算补间动画
                    animLayer.genTweenAnimData();
                }
            }
        } catch (JSONException e) {
            FlashAnimCommon.log(e);
            return false;
        }

        return true;
    }

    private boolean parseBinFile(){
        byte[] binData = mAnimCommon.readData(mFlashDir + "/" + mFlashName + FlashAnimCommon.BINEXT);
        if (binData == null){
            FlashAnimCommon.log("[E] read bin data failed0");
            return false;
        }

        FlashDataReader dataReader = new FlashDataReader(binData);
        int frameRate = dataReader.readUShort();
        long oneFrameTimeMs = 1000 / frameRate;

        if (mAnimData == null){
            mAnimData = new FlashAnimData();
            mAnimData.oneFrameDurationMs = oneFrameTimeMs;
            mAnimData.frameRate = frameRate;
        }

        int imageCount = dataReader.readUShort();
        ArrayList<String> imageNameArr = new ArrayList<>();
        for (int i = 0; i < imageCount; i++){
            String texName = dataReader.readString();
            mAnimCommon.addImage(texName, mAnimCommon.readImage(mFlashDir + "/" + mFlashName + "/" + texName));
            imageNameArr.add(texName);
        }

        int animCount = dataReader.readUShort();
        for (int j = 0; j < animCount; j++){
            FlashAnimDataAnim oneAnim = new FlashAnimDataAnim();
            oneAnim.animName = dataReader.readString();
            oneAnim.frameCount = dataReader.readUShort();
            mAnimData.addAnim(oneAnim);
            int layerCount = dataReader.readUShort();
            for (int k = 0; k < layerCount; k++){
                FlashAnimDataLayer oneLayer = new FlashAnimDataLayer();
                oneLayer.index = layerCount - k;
                oneLayer.animName = oneAnim.animName;
                oneAnim.addLayer(oneLayer);
                int keyFrameCount = dataReader.readUShort();
                for (int l = 0; l < keyFrameCount; l++){
                    FlashAnimDataKeyFrame oneKeyFrame = new FlashAnimDataKeyFrame();
                    oneLayer.addKeyFrame(oneKeyFrame);
                    boolean isEmpty = dataReader.readBool();
                    oneKeyFrame.isEmpty = isEmpty;
                    oneKeyFrame.frameIndex = dataReader.readUShort();
                    if (!isEmpty){
                        oneKeyFrame.duration = dataReader.readUShort();
                        oneKeyFrame.isTween = dataReader.readBool();
                        oneKeyFrame.imageName = imageNameArr.get(dataReader.readUShort());
                        oneKeyFrame.mark = dataReader.readString();
                        oneKeyFrame.alpha = dataReader.readUChar();
                        oneKeyFrame.r = dataReader.readUChar();
                        oneKeyFrame.g = dataReader.readUChar();
                        oneKeyFrame.b = dataReader.readUChar();
                        oneKeyFrame.a = dataReader.readUChar();
                        oneKeyFrame.scaleX = dataReader.readFloat();
                        oneKeyFrame.scaleY = dataReader.readFloat();
                        oneKeyFrame.skewX = dataReader.readFloat();
                        oneKeyFrame.skewY = dataReader.readFloat();
                        oneKeyFrame.x = dataReader.readFloat();
                        oneKeyFrame.y = dataReader.readFloat();
                    }else{
                        oneKeyFrame.duration = 1;
                    }
                }

                //layer 准备计算补间动画
                oneLayer.genTweenAnimData();
            }
        }

        return true;
    }

    public List<String> getAnimNames(){
        Set<String> animNameSet = mAnimData.anims.keySet();
        String s[] = new String[animNameSet.size()];
        animNameSet.toArray(s);
        return Arrays.asList(s);
    }

    private FlashViewRender mRender;

    public void setRender(FlashViewRender render) {
        this.mRender = render;
    }

    /**
     * 播放第frameIndex帧动画
     * @param frameIndex
     */
    private void updateToFrameIndex(int frameIndex){
        FlashAnimCommon.log("[D] FlashAnim.updateToFrameIndex " + frameIndex);
        if (!isPlaying){
            return;
        }
        if (mRender != null) {
            mRender.updateToFrameIndex(mAnimData, mPlayingAnimName, frameIndex);
        }else{
            FlashAnimCommon.log("[E] FlashAnim.updateToFrameIndex [" + frameIndex + "] failed as mRender is null");
        }
    }

    private void updateToFrameIndexInMainThread(int frameIndex){
        Message msg = Message.obtain();
        msg.what = HANDLER_MSG_TYPE_UPDATE_FRAME;
        msg.arg1 = frameIndex;
        mHandler.sendMessage(msg);
    }

    private long getCurrentTimeMs(){
        return System.currentTimeMillis();
    }

    /**
     * 触发某一帧上的事件
     * @param checkFrameIndex
     */
    private void triggerEventWithIndex(int checkFrameIndex) {
        if (!isPlaying) {
            return;
        }
        FlashAnimDataAnim animData = mAnimData.anims.get(mPlayingAnimName);
        for (int i = 0; i < animData.layers.size(); i++){
            FlashAnimDataLayer layer = animData.layers.get(i);
            FlashAnimDataKeyFrame keyFrame = layer.generatedKeyFrameDict.get(checkFrameIndex);
            if (keyFrame != null && keyFrame.mark != null && keyFrame.mark.length() > 0){
                onEventInMainThread(FlashViewEvent.MARK, keyFrame);
            }
        }
    }

    /**
     * 触发某一时刻[应该]触发的事件
     * @param currTime
     */
    private void triggerEventWithTime(long currTime){
        if (!isPlaying){
            return;
        }

        //自上次循环到现在，应该播放多少帧
        int shouldPlayFrameCount = (int)Math.floor((currTime - mLastFrameTimeMs) / mAnimData.oneFrameDurationMs);
        if (shouldPlayFrameCount == 0){
            return;
        }

        for (int i = 0; i < shouldPlayFrameCount; i++){
            int checkFrameIndex = mLastPlayIndex + i + 1;
            if (checkFrameIndex > mToIndex){
                checkFrameIndex = checkFrameIndex - mToIndex + mFromIndex;
            }

            triggerEventWithIndex(checkFrameIndex);
        }
    }

    public void play(String animName, int loopTimes){
        play(animName, loopTimes, 0);
    }

    public void play(String animName, int loopTimes, int fromIndex){
        play(animName, loopTimes, fromIndex, -1);
    }

    private String mPlayingAnimName = null;
    private int mTotalLoopTimes = 0;
    private int mLoopTimes = 0;
    private long mStartTimeMs = 0;
    private long mLastFrameTimeMs = 0;
    //上次循环结束时间
    private long mLastLoopEndTimeMs = 0;

    private ScheduledExecutorService mScheduleExecutor;
    public Runnable mAnimPlayerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying){
                animMainLoop();
            }
        }
    };

    private void startTimer(){
        stopTimer();
        mScheduleExecutor = Executors.newSingleThreadScheduledExecutor();
        mScheduleExecutor.scheduleAtFixedRate(mAnimPlayerRunnable, 0, mAnimData.oneFrameDurationMs, TimeUnit.MILLISECONDS);
    }

    private void stopTimer(){
        if (mScheduleExecutor == null){
            return;
        }
        mScheduleExecutor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!mScheduleExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                mScheduleExecutor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!mScheduleExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            mScheduleExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        mScheduleExecutor = null;
    }

    public void play(String animName, int loopTimes, int fromIndex, int toIndex){
        if (!isInitOk){
            FlashAnimCommon.log("[E] FlashAnim.play init is not ok");
            return;
        }
        if (animName == null || !getAnimNames().contains(animName)){
            FlashAnimCommon.log("[E] play anim failed, animName is error");
            return;
        }

        if (!isPlaying){
            startTimer();
        }

        mPlayingAnimName = animName;
        int maxIndex = mAnimData.anims.get(mPlayingAnimName).frameCount - 1;
        mFromIndex = fromIndex;
        if (mFromIndex < 0 || mFromIndex > maxIndex){
            mFromIndex = 0;
        }
        mToIndex = toIndex;
        if (mToIndex < 0 || mToIndex > maxIndex){
            mToIndex = maxIndex;
        }

        mTotalLoopTimes = loopTimes;
        mLoopTimes = 0;

        mStartTimeMs = getCurrentTimeMs();

        mLastFrameTimeMs = mStartTimeMs;

        mLastLoopEndTimeMs = -1;

        onEventInMainThread(FlashViewEvent.START, null);

        isPlaying = true;
    }

    private int mLastPlayIndex = -1;
    private void stopInner(){
        isPlaying = false;
        mPlayingAnimName = null;
        mStartTimeMs = 0;
        mFromIndex = 0;
        mToIndex = 0;
        mLoopTimes = 0;
        mTotalLoopTimes = 0;
        mLastPlayIndex = -1;
        mLastLoopEndTimeMs = -1;

        if (mScheduleExecutor != null && !mScheduleExecutor.isTerminated() && !mScheduleExecutor.isShutdown()){
            mScheduleExecutor.shutdown();
        }
    }

    public void stop(){
        if (isPlaying){
            stopInner();
            onEventInMainThread(FlashViewEvent.STOP, null);
        }
    }

    public void pause(){
        isPlaying = true;
    }

    public void resume(){
        isPlaying = false;
    }

    private void animMainLoop(){
        //计算
        long currTime = getCurrentTimeMs();
        double passedTime = currTime - mStartTimeMs;
        double passedCount = passedTime / mAnimData.oneFrameDurationMs;
        int animLen = mToIndex - mFromIndex + 1;
        int currIndex = mFromIndex + (int)passedCount % animLen;

        //播放currIndex帧
        if (currIndex != mLastPlayIndex){
            updateToFrameIndexInMainThread(currIndex);
            onEventInMainThread(FlashViewEvent.FRAME, currIndex);
        }

        //初始化 mLastPlayIndex
        if (mLastPlayIndex < 0){
            mLastPlayIndex = 0;
        }

        //触发事件
        triggerEventWithTime(currTime);

        //结束事件
        if (currIndex != mLastPlayIndex &&
                (mLastLoopEndTimeMs < 0 || currTime - mLastLoopEndTimeMs >= mAnimData.oneFrameDurationMs * (animLen - 1))){
            FlashAnimDataAnim animData = mAnimData.anims.get(mPlayingAnimName);
            if (currIndex + 1 >= animData.frameCount || currIndex < mLastPlayIndex){
                mLoopTimes++;
                mLastLoopEndTimeMs = currTime;
                if (mTotalLoopTimes != FlashAnimCommon.FlashLoopTimeForever && mLoopTimes >= mTotalLoopTimes){
                    stop();
                    return;
                }

                onEventInMainThread(FlashViewEvent.STOP, mLoopTimes);
            }
        }

        //重置状态
        mLastPlayIndex = currIndex;

        //向前对齐
        if (passedCount != (int)passedCount){
            mLastFrameTimeMs = (long)Math.floor(passedCount) * mAnimData.oneFrameDurationMs + mStartTimeMs;
        }else{
            mLastFrameTimeMs = currTime;
        }
    }

}
