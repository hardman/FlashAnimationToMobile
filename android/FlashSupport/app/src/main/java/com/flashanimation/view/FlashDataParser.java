/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
package com.flashanimation.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
public class FlashDataParser{
    private static final String TAG = "FlashDataParser";
    private Context mContext;
    private String mFlashName = null;
    private String mFlashDir = DEFAULT_FLASH_DIR;
    private float mDPIRate = -1;
    private float mScaleX = -1;
    private float mScaleY = -1;
    private int mDesignDPI = DEFAULT_FLASH_DPI;

    private int mSetLoopTimes = FlashLoopTimeOnce;
    private int mLoopTimes = 0;

    public static final int DEFAULT_FLASH_DPI = 326;
    // 默认sd卡中动画文件存储文件夹
    public static final String DEFAULT_FLASH_DIR = "flashAnims";
    public static final String DEFAULT_FLASH_ZIP_DIR = "flashAnimZips";
    // 动画循环模式：播放一次
    public static final int FlashLoopTimeOnce = 1;
    // 动画循环模式：永久循环
    public static final int FlashLoopTimeForever = 0;

    // 动画事件：动画过程会发生的事件 通过 FlashViewEventCallback 回调
    public enum FlashViewEvent{//动画事件
        START,
        FRAME,
        ONELOOPEND,
        STOP,
        MARK,
    }

    private enum FileType{//表示动画文件在sd卡还是assets
        ASSETS,
        SDCARD,
        NONE
    }

    private enum FileDataType{//描述文件是json还是二进制
        JSON,
        BIN,
        NONE
    }

    //事件回调数据，内容可为空
    public static class FlashViewEventData{
        public int index;
        public String mark;
        public KeyFrameData data;
    }

    //事件回调接口
    public interface IFlashViewEventCallback{
        void onEvent(FlashViewEvent e, FlashViewEventData data);
    }

    private IFlashViewEventCallback mEventCallback;

    private FileDataType mFileDataType = FileDataType.NONE;
    private FileType mFileType = FileType.NONE;
    private AssetManager mAssetManager;
    private String mSdcardPath = null;

    private static class Data{
        String string;
        byte [] bytes;
    }

    private enum DataType{
        STRING,
        BYTES
    }

    //使用new初始化使用
    public FlashDataParser(Context c, String flashName) {
        this(c, flashName, DEFAULT_FLASH_DIR);
    }

    public FlashDataParser(Context c, String flashName, String flashDir) {
        this(c, flashName, flashDir, DEFAULT_FLASH_DPI);
    }

    public FlashDataParser(Context c, String flashName, String flashDir, int designDPI){
        mContext = c;
        mFlashName = flashName;
        mFlashDir = flashDir;
        mDesignDPI = designDPI;
        init();
    }

    public static void log(String msg){
        Log.i(TAG, msg);
    }

    public static void log(Throwable tx){
        log(tx.toString());
        for (StackTraceElement ele : tx.getStackTrace()) {
            log("\tat " + ele.toString());
        }
    }

    private String readStringFromInStream(InputStream in){
        StringBuilder ret = null;
        try {
            byte b[] = new byte[8096];
            int readRet = -1;
            ret = new StringBuilder();
            while((readRet = in.read(b)) > 0){
                ret.append(new String(b, 0, readRet));
            }
        }catch(IOException e){
            ret = null;
            log(e);
        }
        return ret != null ? ret.toString() : null;
    }

    private byte[] readBytesFromInStream(InputStream in){
        byte [] bytes = null;
        try {
            bytes = new byte[in.available()];
            in.read(bytes);
        }catch (IOException e) {
            bytes = null;
            log(e);
        }
        return bytes;
    }

    private Data getData(String path, DataType dataType){
        String string = null;
        byte [] bytes = null;
        switch (mFileType){
            case ASSETS:
                InputStream in = null;
                try {
                    in = mAssetManager.open(path);
                    switch (dataType){
                        case STRING:
                            string = readStringFromInStream(in);
                            break;
                        case BYTES:
                            bytes = readBytesFromInStream(in);
                            break;
                    }
                }catch(IOException e){
                    log(e);
                }finally {
                    if(in != null){
                        try {
                            in.close();
                        }catch (IOException e){
                            log(e);
                        }
                    }
                }
                break;
            case SDCARD:
                File f = new File(mSdcardPath + "/" + path);
                if(f.isFile()){
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(f);
                        switch (dataType){
                            case STRING:
                                string = readStringFromInStream(fis);
                                break;
                            case BYTES:
                                bytes = readBytesFromInStream(fis);
                                break;
                        }
                    }catch (IOException e){
                        log(e);
                    }
                    finally {
                        try {
                            fis.close();
                        }catch (IOException e){
                            log(e);
                        }
                    }
                }
                break;
            default:
                break;
        }
        Data d = new Data();
        d.bytes = bytes;
        d.string = string;
        return d;
    }

    private byte[] readData(){
        return getData(mFlashDir + "/" + mFlashName + ".flabin", DataType.BYTES).bytes;
    }

    private JSONObject readJson(){
        JSONObject jsonObj = null;
        String jsonAssetPath = mFlashDir + "/" + mFlashName + ".flajson";
        try {
            jsonObj = new JSONObject(getData(jsonAssetPath, DataType.STRING).string);
        }catch(JSONException e){
            log(e);
        }
        return jsonObj;
    }

    private Bitmap readImage(String imageName){
        String imageFullPath = mFlashDir + "/" + mFlashName + "/" + imageName;
        byte bytes[] = getData(imageFullPath, DataType.BYTES).bytes;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

    private boolean guessFileType(String assetFilePathPre, FileDataType fileDataType){
        String assetFilePathExt = ".flajson";
        if(fileDataType == FileDataType.BIN){
            assetFilePathExt = ".flabin";
        }
        try {
            mAssetManager.open(assetFilePathPre + assetFilePathExt);
            mFileType = FileType.ASSETS;
            mFileDataType = fileDataType;
            return true;
        }catch(Exception e){
            if(createSDCardPath() && null != mSdcardPath){
                String sdcardFilePath = mSdcardPath + "/" + assetFilePathPre + assetFilePathExt;
                File sdcardFile = new File(sdcardFilePath);
                if(sdcardFile.isFile()){
                    mFileType = FileType.SDCARD;
                    mFileDataType = fileDataType;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean initFileType(){
        String assetFilePathPre = mFlashDir + "/" + mFlashName;
        return guessFileType(assetFilePathPre, FileDataType.JSON) || guessFileType(assetFilePathPre, FileDataType.BIN);
    }

    /***
     * 下载动画文件的帮助类
     */
    public static abstract class Downloader{

        /***
         * @param url: 下载地址
         * @param outFile: 目标路径
         * @return 是否下载成功，所以这个应该用阻塞模式
         */
        public abstract void download(String url, String outFile, DownloadCallback cb);

        public enum DownloadType{
            IMAGE,
            DESCRIPTION,
            ZIP,
        }

        public interface DownloadCallback{
            void onComplete(boolean succ);
            void onProgress(float per);
        }

        /***
         *
         * @param ctx: Context
         * @param url: 下载地址
         * @param fileName: 下载的文件全名
         * @param animName: 下载文件所属的动画名
         * @param type: 下载的文件类型，是图片还是描述文件
         * @return 返回是否下载成功。
         */
        public boolean downloadAnimFile(final Context ctx, String url, String fileName, String animName, final DownloadType type, final DownloadCallback cb){
            String outFile = null;

            switch (type){
                case IMAGE:
                    String imageDirFile = createDirInSdcard(ctx, DEFAULT_FLASH_DIR + "/" + animName);
                    if(imageDirFile != null){
                        outFile = imageDirFile + "/" + fileName;
                    }
                    break;
                case DESCRIPTION:
                    String desDirFile = createDirInSdcard(ctx, DEFAULT_FLASH_DIR);
                    if(desDirFile != null){
                        outFile = desDirFile + "/" + fileName;
                    }
                case ZIP:
                    String zipDirFile = createDirInSdcard(ctx, DEFAULT_FLASH_ZIP_DIR);
                    if(zipDirFile != null){
                        outFile = zipDirFile + "/" + fileName;
                    }
                    break;
            }

            if(outFile == null){
                log("[ERROR] outFile is null when downloadAnimFile");
                return false;
            }

            File file = new File(outFile);
            if(file.exists()){
                if(file.isFile() && isValidFile(file)) {
                    cb.onComplete(true);
                    return true;
                }else{
                    deleteFile(file);
                }
            }

            if(outFile != null) {
                final String outFileStr = outFile;
                download(url, outFile, new DownloadCallback() {
                    @Override
                    public void onComplete(boolean succ) {
                        log("download outFile=" + outFileStr +" is completed! succ=" + succ);
                        if (succ && type == DownloadType.ZIP){
                            String desDirFile = createDirInSdcard(ctx, DEFAULT_FLASH_DIR);
                            boolean zipRet = Downloader.upZipFile(new File(outFileStr), desDirFile);
                            cb.onComplete(zipRet);
                            log("unzip zip file:" + outFileStr + " to " + desDirFile + ", ret=" + zipRet);
                        }else{
                            cb.onComplete(succ);
                        }
                    }

                    @Override
                    public void onProgress(float per) {
                        log("download outFile=" + outFileStr + ", per=" + per);
                        cb.onProgress(per);
                    }
                });
            }

            return true;
        }

        private static boolean isValidFile(File f){
            return f != null && f.isFile() && f.length() > 100;
        }

        /**
         * 解压缩一个文件
         *
         * @param zipFile 压缩文件
         * @param folderPath 解压缩的目标目录
         * @throws IOException 当解压缩过程出错时抛出
         */
        public static boolean upZipFile(File zipFile, String folderPath) {
            boolean ret = true;
            File desDir = new File(folderPath);
            if (!desDir.exists()) {
                desDir.mkdirs();
            }else if(!desDir.isDirectory()){
                deleteFile(desDir);
                desDir.mkdirs();
            }
            InputStream in = null;
            OutputStream out = null;
            ZipFile zf = null ;
            try {
                zf = new ZipFile(zipFile);
            }catch (IOException e){
                log(e);
                ret = false;
            }
            if (zf != null) {
                for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
                    try {
                        ZipEntry entry = ((ZipEntry) entries.nextElement());
                        String str = folderPath + File.separator + entry.getName();
                        str = new String(str.getBytes("8859_1"), "GB2312");
                        File desFile = new File(str);
                        if (entry.isDirectory()){
                            continue;
                        }
                        if (desFile.exists() && !isValidFile(desFile)){
                            desFile.delete();
                        }
                        if (!desFile.exists()) {
                            File fileParentDir = desFile.getParentFile();
                            if (!fileParentDir.exists()) {
                                fileParentDir.mkdirs();
                            }
                            desFile.createNewFile();
                            out = new FileOutputStream(desFile);
                            byte buffer[] = new byte[1024 * 1024];
                            int realLength;
                            in = zf.getInputStream(entry);
                            while ((realLength = in.read(buffer)) > 0) {
                                out.write(buffer, 0, realLength);
                            }
                        }else{
                            in = null;
                            out = null;
                        }
                    } catch (IOException e) {
                        log(e);
                        ret = false;
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                            if(out != null){
                                out.close();
                            }
                        } catch (IOException e) {
                            log(e);
                            ret = false;
                        }
                    }
                }
            }
            return ret;
        }
    }

    private static void deleteFile(File file){
        if (file.isFile()) {
            file.delete();
            return;
        }

        if(file.isDirectory()){
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                deleteFile(childFiles[i]);
            }
            file.delete();
        }
    }

    private static String createDirInSdcard(Context ctx, String flashDir){
        String path;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }else{
            return null;
        }
        path = path + "/" + "." + ctx.getPackageName();
        String animDir = path + "/" + flashDir;
        File file = new File(animDir);
        if(file.exists()){
            if(!file.isDirectory()){
                file.delete();
                if(file.mkdirs()){
                    return animDir;
                }else{
                    return null;
                }
            }
        }else{
            if(file.mkdirs()){
                return animDir;
            }else{
                return null;
            }
        }
        return animDir;
    }

    private boolean createSDCardPath(){
        String flashDir = createDirInSdcard(mContext, mFlashDir);
        if(flashDir != null) {
            mSdcardPath = flashDir.replace(mFlashDir, "");
            return true;
        }else{
            return false;
        }
    }

    private void cleanData(){
        stop();
        mFlashName = null;
        mFlashDir = null;
        mAssetManager = null;
        mFileDataType = FileDataType.NONE;
        mFileType = FileType.NONE;
        mJson = null;
        mData = null;
        mParsedData = null;
        mImages = null;

        mParseFrameMaxIndex = 0;
        mParseLastIndex = -1;
        mParseLastIsTween = false;
        mParseLastFrame = null;

        mRunningAnimName = null;
        mTotalTime = 0;

        mDPIRate = -1;
        mScaleX = -1;
        mScaleY = -1;
        mDesignDPI = DEFAULT_FLASH_DPI;

        mSetLoopTimes = FlashLoopTimeOnce;
        mLoopTimes = 0;
    }

    private boolean init(){
        if(mFlashName == null || mFlashDir == null){
            log("[ERROR] mFlashName/mFlashDir is null");
            return false;
        }

        if(mAssetManager == null){
            mAssetManager = mContext.getAssets();
        }

        if (!initFileType() || mFileType == FileType.NONE){
            log("[ERROR] file is not found in assets and sdcard");
            return false;
        }

        if(mFileDataType == FileDataType.JSON) {
            mJson = readJson();

            if (mJson == null) {
                log("[ERROR] flajson file read error");
                return false;
            }

            parseJson();
        }else{
            mData = readData();

            if (mData == null) {
                log("[ERROR] flabin file read error");
                return false;
            }

            parseData();
        }

        log("haha mFrameRate=" + mFrameRate + ", " + mFileDataType);

        mOneFrameTime = 1.0 / mFrameRate;

        isPause = false;

        mDPIRate = 1.0f * mContext.getResources().getDisplayMetrics().densityDpi / mDesignDPI;

        setScale(1, 1, true);

        return true;
    }

    public void setScale(float x, float y){
        setScale(x, y, true);
    }

    public void setScale(float x, float y, boolean isDpiEffect){
        if(isDpiEffect){
            mScaleX = mDPIRate * x;
            mScaleY = mDPIRate * y;
        }else{
            mScaleX = x;
            mScaleY = y;
        }
    }

    private JSONObject readKeyFrame(FlaDataReader reader, ArrayList<String> imageArr){
        try{
            JSONObject ret = new JSONObject();
            boolean isEmpty = reader.readBool();
            ret.put("isEmpty", isEmpty);
            ret.put("frameIndex", reader.readUShort());
            if(!isEmpty){
                ret.put("duration", reader.readUShort());
                ret.put("isTween", reader.readBool());
                ret.put("texName", imageArr.get(reader.readUShort()));
                ret.put("mark", reader.readString());
                ret.put("alpha", reader.readUChar());
                JSONObject colorJson = new JSONObject();
                colorJson.put("r", reader.readUChar());
                colorJson.put("g", reader.readUChar());
                colorJson.put("b", reader.readUChar());
                colorJson.put("a", reader.readUChar());
                ret.put("color", colorJson);
                ret.put("scaleX", reader.readFloat());
                ret.put("scaleY", reader.readFloat());
                ret.put("skewX", reader.readFloat());
                ret.put("skewY", reader.readFloat());
                ret.put("x", reader.readFloat());
                ret.put("y", reader.readFloat());
            }
            return ret;
        }catch (JSONException e){
            log(e);
        }
        return null;
    }

    private void parseJson(){
        mParsedData = new HashMap<String, AnimData>();
        mImages = new HashMap<String, Bitmap>();
        try {
            mFrameRate = mJson.getInt("frameRate");

            //解析images
            JSONArray textures = mJson.getJSONArray("textures");
            for(int i = 0; i < textures.length(); i++){
                String texName = textures.getString(i);
                mImages.put(texName, readImage(texName));
            }

            //解析anims
            JSONArray anims = mJson.getJSONArray("anims");
            for(int j = 0; j < anims.length(); j++){
                AnimData parsedAnim = new AnimData();
                JSONObject animData = anims.getJSONObject(j);
                String animName = animData.getString("animName");
                mParseFrameMaxIndex = animData.getInt("frameMaxNum");
                parsedAnim.animFrameLen = mParseFrameMaxIndex;

                JSONArray layers = animData.getJSONArray("layers");
                for(int k = 0; k < layers.length(); k++){
                    JSONObject oneLayer = layers.getJSONObject(k);
                    JSONArray frames = oneLayer.getJSONArray("frames");
                    mParseLastIndex = -1;
                    mParseLastIsTween = false;
                    mParseLastFrame = null;
                    for(int l = 0; l < frames.length(); l++){
                        JSONObject oneFrame = frames.getJSONObject(l);
                        parseKeyFrame(oneFrame, parsedAnim);
                    }
                }
                mParsedData.put(animName, parsedAnim);
            }

        }catch(JSONException e){
            log(e);
        }
    }

    private void parseData(){
        mParsedData = new HashMap<String, AnimData>();
        mImages = new HashMap<String, Bitmap>();
        ArrayList<String> imagesArr = new ArrayList<String>();

        FlaDataReader dataReader = new FlaDataReader();

        mFrameRate = dataReader.readUShort();
        //解析images
        int imageNum = dataReader.readUShort();
        for(int i = 0; i < imageNum; i++){
            String texName = dataReader.readString();
            mImages.put(texName, readImage(texName));
            imagesArr.add(texName);
        }
        //解析anims
        int animNum = dataReader.readUShort();
        for(int j = 0; j < animNum; j++){
            AnimData parsedAnim = new AnimData();
            String animName = dataReader.readString();
            mParseFrameMaxIndex = dataReader.readUShort();
            parsedAnim.animFrameLen = mParseFrameMaxIndex;
            int layerNum = dataReader.readUShort();
            for(int k = 0; k < layerNum; k++){
                int keyFrameNum = dataReader.readUShort();
                mParseLastIndex = -1;
                mParseLastIsTween = false;
                mParseLastFrame = null;
                for(int l = 0; l < keyFrameNum; l++){
                    JSONObject oneFrame = readKeyFrame(dataReader, imagesArr);
                    parseKeyFrame(oneFrame, parsedAnim);
                }
            }
            mParsedData.put(animName, parsedAnim);
        }
    }

    private void parseKeyFrame(JSONObject oneFrame, AnimData parsedAnim){
        try {
            int index = oneFrame.getInt("frameIndex");
            boolean isEmpty = oneFrame.getBoolean("isEmpty");
            if (isEmpty) {
                return;
            }

            int duration = oneFrame.getInt("duration");

            boolean isTween = oneFrame.getBoolean("isTween");

            int fromIdx = mParseLastIndex + 1;
            int toIdx = index;
            int len = toIdx - fromIdx + 1;

            for (int m = fromIdx; m <= toIdx; m++) {
                if (!mParseLastIsTween) {
                    if (m == toIdx) {
                        addOneFrameDataToIdx(oneFrame, m, parsedAnim);
                    } else {
                        addOneFrameDataToIdx(mParseLastFrame, m, parsedAnim);
                    }
                } else {
                    float per = (float) (m - fromIdx + 1) / len;
                    JSONObject newFrame = new JSONObject();
                    JSONObject lastFrameColor = mParseLastFrame.getJSONObject("color");
                    JSONObject oneFrameColor = oneFrame.getJSONObject("color");
                    newFrame.put("texName", oneFrame.getString("texName"));
                    newFrame.put("x", calcPercentValue(mParseLastFrame, oneFrame, "x", per));
                    newFrame.put("y", calcPercentValue(mParseLastFrame, oneFrame, "y", per));
                    newFrame.put("scaleX", calcPercentValue(mParseLastFrame, oneFrame, "scaleX", per));
                    newFrame.put("scaleY", calcPercentValue(mParseLastFrame, oneFrame, "scaleY", per));
                    newFrame.put("skewX", calcPercentValue(mParseLastFrame, oneFrame, "skewX", per));
                    newFrame.put("skewY", calcPercentValue(mParseLastFrame, oneFrame, "skewY", per));
                    newFrame.put("alpha", calcPercentValue(mParseLastFrame, oneFrame, "alpha", per));
                    JSONObject colorJSONObj = new JSONObject();
                    colorJSONObj.put("r", calcPercentValue(lastFrameColor, oneFrameColor, "r", per));
                    colorJSONObj.put("g", calcPercentValue(lastFrameColor, oneFrameColor, "g", per));
                    colorJSONObj.put("b", calcPercentValue(lastFrameColor, oneFrameColor, "b", per));
                    colorJSONObj.put("a", calcPercentValue(lastFrameColor, oneFrameColor, "a", per));
                    newFrame.put("color", colorJSONObj);
                    addOneFrameDataToIdx(newFrame, m, parsedAnim);
                }
            }

            if (duration > 1 && index + duration >= mParseFrameMaxIndex) {
                for (int n = index; n <= mParseFrameMaxIndex - 1; n++) {
                    addOneFrameDataToIdx(oneFrame, n, parsedAnim);
                }
            }

            mParseLastIndex = index;
            mParseLastIsTween = isTween;
            mParseLastFrame = oneFrame;
        }catch (JSONException e){
            log(e);
        }
    }

    private float calcPercentValue(JSONObject lastFrame, JSONObject newFrame, String key, float per){
        try {
            float oldValue = (float) lastFrame.getDouble(key);
            float newValue = (float) newFrame.getDouble(key);
            float ret = -1;

            float span = Math.abs(newValue - oldValue);
            if(span > 180 && (key.equals("skewX") || key.equals("skewY"))){
                float realSpan = 360 - span;
                float mark = (oldValue < 0) ? -1 : 1;
                float mid = 180 * mark;
                float newStart = -mid;
                float midPer = (mid - oldValue) / realSpan;
                if (per < midPer) {
                    ret = oldValue + per * realSpan * mark;
                }else{
                    ret = newStart + (per - midPer) * realSpan * mark;
                }
            }else{
                ret = oldValue + per * (newValue - oldValue);
            }
            return ret;
        }catch(JSONException e){
            log(e);
        }
        return 0;
    }

    private ArrayList<KeyFrameData> createArrForIdx(int idx, AnimData parsedAnim){
        String sIdx = "" + idx;
        if(parsedAnim.keyFrameData == null){
            parsedAnim.keyFrameData = new HashMap<String, ArrayList<KeyFrameData>>();
        }
        ArrayList<KeyFrameData> arr = null;
        if(!parsedAnim.keyFrameData.containsKey(sIdx)){
            arr = new ArrayList<KeyFrameData>();
            parsedAnim.keyFrameData.put(sIdx, arr);
        }else{
            arr = parsedAnim.keyFrameData.get(sIdx);
        }
        return arr;
    }

    private void addOneFrameDataToIdx(JSONObject oneFrame, int idx, AnimData parsedAnim){
        ArrayList<KeyFrameData> arr = createArrForIdx(idx, parsedAnim);
        if(oneFrame != null) {
            arr.add(new KeyFrameData(oneFrame));
        }
    }

    public boolean reload(String flashName){
        return reload(flashName, DEFAULT_FLASH_DIR);
    }

    public boolean reload(String flashName, String flashDir){
        return reload(flashName, flashDir, DEFAULT_FLASH_DPI);
    }

    /***
     * 使用这个对象，重新加载一个新的动画
     * @param flashName: 动画名
     * @param flashDir: 动画目录
     * @param designDPI: 设计dpi 默认iphone5 为326
     * @return
     */
    public boolean reload(String flashName, String flashDir, int designDPI){
        cleanData();
        mFlashName = flashName;
        mFlashDir = flashDir;
        mDesignDPI = designDPI;
        return init();
    }

    /***
     * 事件回调
     * @param callback
     */
    public void setEventCallback(IFlashViewEventCallback callback){
        mEventCallback = callback;
    }

    /***
     * 替换动画中所有相同图片
     * @param texName: 对应的图片名
     * @param bitmap: 新的Bitmap
     */
    public void replaceBitmap(String texName, Bitmap bitmap){
        mImages.put(texName, bitmap);
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
        if(!mParsedData.containsKey(animName)){
            log("[ERROR] play() cant find the animName " + animName);
            return;
        }
        stop();
        isStop = false;
        mTotalTime = 0;
        mRunningAnimName = animName;
        mSetLoopTimes = loopTimes;
        mLoopTimes = 0;

        mFromIndex = fromIndex;
        mToIndex = toIndex;

        if(mEventCallback != null){
            mEventCallback.onEvent(FlashViewEvent.START, null);
        }
    }

    public boolean isPlaying(){
        return !isStop && !isPause;
    }

    public boolean isPaused(){
        return isPause;
    }

    public boolean isStoped(){
        return isStop;
    }

    public void increaseTotalTime(double increaseValue){
        mTotalTime += increaseValue;
    }

    public double getTotalTime(){
        return mTotalTime;
    }

    public double getOneFrameTime(){
        return mOneFrameTime;
    }

    public int getParseFrameMaxIndex(){
        return mParseFrameMaxIndex;
    }

    /***
     * 获取动画帧数
     * @return 动画帧数
     */
    public int getLength(){
        return mParseFrameMaxIndex + 1;
    }

    /***
     * 停止动画
     */
    public void stop(){
        mTotalTime = 0;
        mRunningAnimName = null;
        mSetLoopTimes = FlashLoopTimeOnce;
        mLoopTimes = 0;
        isStop = true;
        mLastFrameIndex = -1;
    }

    /***
     * 暂停
     */
    public void pause(){
        isPause = true;
    }

    /***
     * 恢复
     */
    public void resume(){
        isPause = false;
    }

    private class FlaDataReader{
        private int mIndex;

        private boolean readBool(){
            boolean b = mData[mIndex] == 0x01;
            mIndex += 1;
            return b;
        }

        private int readUShort(){
            int s = (mData[mIndex] & 0xff) | ((mData[mIndex + 1] << 8) & 0xff00);
            mIndex += 2;
            return s;
        }

        private int readInt(){
            int i = (mData[mIndex] & 0xff | ((mData[mIndex + 1] << 8) & 0xff00)) |
                    ((mData[mIndex + 2] << 16) & 0xff0000) | (mData[mIndex + 3] << 24);
            mIndex += 4;
            return i;
        }

        private float readFloat(){
            return Float.intBitsToFloat(readInt());
        }

        private short readUChar(){
            short c = (short)(mData[mIndex] & 0xff);
            mIndex += 1;
            return c;
        }

        private String readString(){
            int strLen = readUShort();
            String str = new String(mData, mIndex, strLen);
            mIndex += strLen;
            return str;
        }
    }

    private static class BlendColor{
        private int r;
        private int g;
        private int b;
        private int a;
        private BlendColor(int r, int g, int b, int a){
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public String toString() {
            return "{r=" + r + ",g=" + g + ",b=" + b + ",a=" + a + "}";
        }
    }

    /***
     * 表示flash中某一个关键帧的类
     */
    public static class KeyFrameData{
        public String texName; // image name
        public float x; // pos x
        public float y; // pos y
        public float sx; // scale x
        public float sy; // scale y
        public float skewX; // rotate x
        public float skewY; // rotate y
        public float alpha; // image alpha
        public float r; // color overlay r
        public float g; // color overlay g
        public float b; // color overlay b
        public float a; // color overlay a
        public String mark; // mark at this key frame
        private KeyFrameData(JSONObject oneFrame){
            try {
                texName = oneFrame.getString("texName");
                x = (float)oneFrame.getDouble("x");
                y = (float)oneFrame.getDouble("y");
                sx = (float)oneFrame.getDouble("scaleX");
                sy = (float)oneFrame.getDouble("scaleY");
                skewX = (float)oneFrame.getDouble("skewX");
                skewY = (float)oneFrame.getDouble("skewY");
                alpha = (float)oneFrame.getDouble("alpha");
                JSONObject colorJsonObj = oneFrame.getJSONObject("color");
                r = (float)colorJsonObj.getDouble("r");
                g = (float)colorJsonObj.getDouble("g");
                b = (float)colorJsonObj.getDouble("b");
                a = (float)colorJsonObj.getDouble("a");
                if(oneFrame.has("mark")) {
                    mark = oneFrame.getString("mark");
                }
            } catch (JSONException e){
                log(e);
            }
        }

        @Override
        public String toString() {
            return "{" +
                    "texName=" + texName +
                    ",x=" + x +
                    ",y=" + y +
                    ",sx=" + sx +
                    ",sy=" + sy +
                    ",skewX=" + skewX +
                    ",skewY=" + skewY +
                    ",alpha=" + alpha +
                    ",r=" + r +
                    ",g=" + g +
                    ",b=" + b +
                    ",a=" + a +
                    ",mark=" + mark +
                    "}"
                    ;
        }
    }
    private static class AnimData{
        private HashMap<String, ArrayList<KeyFrameData>> keyFrameData;
        private int animFrameLen;
    }
    private HashMap<String, AnimData> mParsedData;
    private HashMap<String, Bitmap> mImages;
    private int mFrameRate;
    private double mOneFrameTime;
    private boolean isPause;
    private boolean isStop;

    //解析数据中用到的过程变量
    private int mParseFrameMaxIndex = 0;
    private int mParseLastIndex = -1;
    private boolean mParseLastIsTween = false;
    private JSONObject mParseLastFrame = null;

    private String mRunningAnimName = null;
    private double mTotalTime = 0;

    //从第几帧播放到第几帧
    private int mFromIndex;
    private int mToIndex;

    private JSONObject mJson = null;
    private byte [] mData = null;

    //绘制begin
    private void drawImage(Canvas c, String imagePath, Point point, PointF anchor, PointF scale, PointF rotate, int alpha, BlendColor color){
        Bitmap bitmap = mImages.get(imagePath);
        float imageWidth = bitmap.getWidth() * mScaleX;
        float imageHeight = bitmap.getHeight() * mScaleY;

        c.save();

        PointF anchorPos = new PointF(point.x, c.getHeight() - point.y);
        PointF finalDrawPos = new PointF(-anchor.x * imageWidth, -(1-anchor.y) * imageHeight);

        c.translate(anchorPos.x, anchorPos.y);//画布移动到锚点

        Matrix matrix = new Matrix();

        if (rotate.x == rotate.y) {
            matrix.postRotate(rotate.x);
        }else{
            float radiusX = (float)angleToRadius(rotate.x);
            float radiusY = (float)angleToRadius(rotate.y);
            float cx = (float)Math.cos(radiusX);
            float sx = (float)Math.sin(radiusX);
            float cy = (float)Math.cos(radiusY);
            float sy = (float)Math.sin(radiusY);

            float matrixValues[] = new float[9];
            matrix.getValues(matrixValues);

            float newValue0 = cy * matrixValues[0] - sx * matrixValues[3];
            float newValue3 = sy * matrixValues[0] + cx * matrixValues[3];
            float newValue1 = cy * matrixValues[1] - sx * matrixValues[4];
            float newValue4 = sy * matrixValues[1] + cx * matrixValues[4];
            float newValue2 = cy * matrixValues[2] - sx * matrixValues[5];
            float newValue5 = sy * matrixValues[2] + cx * matrixValues[5];

            matrixValues[0] = newValue0;
            matrixValues[1] = newValue1;
            matrixValues[2] = newValue2;
            matrixValues[3] = newValue3;
            matrixValues[4] = newValue4;
            matrixValues[5] = newValue5;

            matrix.setValues(matrixValues);
        }

        c.concat(matrix);

        c.scale(scale.x, scale.y);

        c.translate(finalDrawPos.x, finalDrawPos.y);

        RectF bitmapDrawRect = new RectF(0, 0, imageWidth, imageHeight);

        //draw bitmap
        Paint bitmapPaint = new Paint();
        bitmapPaint.setAlpha(alpha);
        bitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        c.drawBitmap(bitmap, null, bitmapDrawRect, bitmapPaint);

        //draw rect
        Paint bitmapRectPaint = new Paint();
        bitmapRectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        bitmapRectPaint.setColor(Color.argb(color.a, color.r, color.g, color.b));
        c.drawRect(bitmapDrawRect, bitmapRectPaint);

        c.restore();
    }

    public void cleanScreen(Canvas c){
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    public void drawCanvas(Canvas c, int frameIndex, String animName, boolean isTriggerEvent){
        AnimData animData = mParsedData.get(animName);

        if(isTriggerEvent && mEventCallback != null){
            FlashViewEventData eventData = new FlashViewEventData();
            eventData.index = frameIndex;
            mEventCallback.onEvent(FlashViewEvent.FRAME, eventData);
        }

        //此判断为了防止，当动画结束时，会多播放一帧。这是临时修改，最好的方式是先判断动画完成的事件，然后再画frame。
        if (mSetLoopTimes == FlashLoopTimeForever || (mLoopTimes == mSetLoopTimes - 1 && mLastFrameIndex <= frameIndex)) {
            ArrayList<KeyFrameData> frameArr = animData.keyFrameData.get("" + frameIndex);
            for(int i = frameArr.size() - 1; i >= 0; i--){
                KeyFrameData frameData = frameArr.get(i);
                String imagePath = frameData.texName;
                Point point = new Point((int)(frameData.x * mScaleX + c.getWidth() / 2),(int)(frameData.y * mScaleY + c.getHeight() / 2));
                PointF anchor = new PointF(0.5f, 0.5f);
                PointF scale = new PointF(frameData.sx, frameData.sy);
                PointF rotation = new PointF(frameData.skewX, frameData.skewY);
                int alpha = (int)frameData.alpha;
                BlendColor color = new BlendColor((int)frameData.r, (int)frameData.g, (int)frameData.b, (int)frameData.a);

                drawImage(c, imagePath, point, anchor, scale, rotation, alpha, color);

                if (isTriggerEvent) {
                    if (frameData.mark != null && frameData.mark.trim().length() > 0) {
                        if (mEventCallback != null) {
                            FlashViewEventData eventData = new FlashViewEventData();
                            eventData.index = frameIndex;
                            eventData.mark = frameData.mark;
                            eventData.data = frameData;
                            mEventCallback.onEvent(FlashViewEvent.MARK, eventData);
                        }
                    }
                }
            }
        }

        if(isTriggerEvent) {
            if (mLastFrameIndex > frameIndex) {
                if (mEventCallback != null) {
                    mEventCallback.onEvent(FlashViewEvent.ONELOOPEND, null);
                }

                if (mSetLoopTimes >= FlashLoopTimeOnce) {
                    if (++mLoopTimes >= mSetLoopTimes) {
                        if (mEventCallback != null) {
                            mEventCallback.onEvent(FlashViewEvent.STOP, null);
                        }
                        stop();
                    }
                }
            }

            mLastFrameIndex = frameIndex;
        }
    }

    public boolean drawCanvas(Canvas c){
        if(mRunningAnimName == null || isPause || isStop){
            return false;
        }
        int animLen = mToIndex - mFromIndex;
        int currFrameIndex = mFromIndex + (int)(mTotalTime / mOneFrameTime) % animLen;

        drawCanvas(c, currFrameIndex, mRunningAnimName, true);
        return true;
    }

    private int mLastFrameIndex = -1;

    private double angleToRadius(float angle){
        return 0.01745329252 * angle;
    }
    //绘制end

    private double getCurrTime(){
        return System.currentTimeMillis() / 1000.0;
    }
}
