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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by wanghongyu on 10/12/15.
 */
public class FlashDataParser{
    //log tag
    private static final String TAG = "FlashDataParser";
    //用于获取Resource和Assets等资源
    private Context mContext;

    //flash文件名
    private String mFlashName = null;

    //flash文件目录，可能在Asset中（Assets/[flash dir]/[flash name]），也可能在sdard中（/sdcard/.[package name]/[flash dir]/[flash name]）。
    private String mFlashDir = DEFAULT_FLASH_DIR;

    //表示dpi比例，也就是[本机dpi/默认设计dpi(326)]的值
    private float mDPIRate = -1;

    //每张图片需要缩放的比例，如果没有特别指定，这个比例默认为mDPIRate
    private float mScaleX = -1;
    private float mScaleY = -1;

    //设计DPI，默认为326，iPhone5s的dpi，制作flash时画布大小为640x1136时不用变，否则需要修改此值。
    //如果不懂此值的意思，请查阅dpi相关的更多资料
    private int mDesignDPI = DEFAULT_FLASH_DPI;

    //指定的动画重复次数，默认为1次
    private int mSetLoopTimes = FlashLoopTimeOnce;

    //当前动画播放中已经重复播放的动画次数
    private int mLoopTimes = 0;

    //当前动画数据是否加载成功，如果加载不成功，那么所有对外函数(public)都不该调用。
    private boolean isInitOk = false;

    //默认设计DPI
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
        START,//动画开始
        FRAME,//每一帧回调
        ONELOOPEND,//任意一次循环结束
        STOP,//动画自然停止
        MARK,//帧上带事件
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

    //回调事件
    private IFlashViewEventCallback mEventCallback;

    //文件数据格式
    private FileDataType mFileDataType = FileDataType.NONE;

    //文件类型是在sdcard还是在assets
    private FileType mFileType = FileType.NONE;

    //asset管理器
    private AssetManager mAssetManager;

    //...文件在sd卡中的路径
    private String mSdcardPath = null;

    //读取flash描述文件的帮助类
    private static class Data{
        String string;
        byte [] bytes;
    }

    //读取flash描述文件的帮助类
    private enum DataType{
        STRING,
        BYTES
    }

    /***
     * 3个构造方法
     */
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

    /***
     * 打印log
     * @param msg 字符串
     */
    public static void log(String msg){
        Log.i(TAG, msg);
    }

    /**
     * 打印log
     * @param tx 异常对象
     */
    public static void log(Throwable tx){
        log(tx.toString());
        for (StackTraceElement ele : tx.getStackTrace()) {
            log("\tat " + ele.toString());
        }
    }

    /***
     * 从流中读取字符串
     * @param in 输入流
     * @return 读取到的字符串
     */
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

    /***
     * 从流中读取二进制数据
     * @param in 输入流
     * @return 读取到的二进制数据
     */
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

    /***
     * 根据数据类型，读取文件内容
     * @param path 文件路径
     * @param dataType 数据类型
     * @return Data
     */
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
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                log(e);
                            }
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

    /***
     * 真正使用的读取二进制数据
     * @return bin bytes
     */
    private byte[] readData(){
        return getData(mFlashDir + "/" + mFlashName + ".flabin", DataType.BYTES).bytes;
    }

    /***
     * 读取json数据
     * @return json object
     */
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

    /***
     * 读取图片
     * @param imageName
     * @return bitmap
     */
    private Bitmap readImage(String imageName){
        String imageFullPath = mFlashDir + "/" + mFlashName + "/" + imageName;
        byte bytes[] = getData(imageFullPath, DataType.BYTES).bytes;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /***
     * 猜测动画文件的类型，并检查需要的文件／文件夹是否存在
     * @param assetFilePathPre 文件前缀（文件名除后缀）
     * @param fileDataType 文件数据类型
     * @return
     */
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

    /**
     * 判断文件是在sdcard中还是assets，是json还是二进制。
     * @return 是否能确定上述信息，如果不能确定说明此动画无法加载
     */
    private boolean initFileType(){
        String assetFilePathPre = mFlashDir + "/" + mFlashName;
        return guessFileType(assetFilePathPre, FileDataType.JSON) || guessFileType(assetFilePathPre, FileDataType.BIN);
    }

    /***
     * 下载动画文件的帮助类
     */
    public static abstract class Downloader{
        //下载目标flash文件目录
        private String mDownloadFlashDir = DEFAULT_FLASH_DIR;
        //下载的目标flash 压缩文件目录
        private String mDownloadFlashZipDir = DEFAULT_FLASH_ZIP_DIR;

        /***
         * 设置下载目标flash文件目录
         * @param flashDir
         */
        public void setDownloadFlashDir(String flashDir){
            mDownloadFlashDir = flashDir;
        }

        /***
         * 设置下载目标压缩文件目录
         * @param flashZipDir
         */
        public void setDownloadFlashZipDir(String flashZipDir){
            mDownloadFlashZipDir = flashZipDir;
        }

        /***
         * @param url: 下载地址
         * @param outFile: 目标路径
         * @return 是否下载成功，所以这个应该用阻塞模式
         */
        public abstract void download(String url, String outFile, DownloadCallback cb);

        /***
         * 下载动画文件类型
         */
        public enum DownloadType{
            IMAGE,//动画图片
            DESCRIPTION,//动画描述文件
            ZIP,//动画压缩成zip文件
        }

        /***
         * 下载文件回调事件
         */
        public interface DownloadCallback{
            void onComplete(boolean succ);
            void onProgress(float per);
        }

        /***
         * 移除某个动画，当某个动画样式改变的时候需调用这个函数，删除掉老动画
         * @param ctx：Context
         * @param animName：也就是filename
         */
        public void removeAnimFiles(Context ctx, String animName){
            String path = FlashDataParser.getExternalStorageDirectory(ctx);
            if (path != null){
                //zipFile
                String zipFileName = path + "/" + mDownloadFlashZipDir + "/" + animName + ".zip";
                deleteFile(new File(zipFileName));
                //animImgsDirName
                String animImgsDirName = path + "/" + mDownloadFlashDir + "/" + animName;
                deleteFile(new File(animImgsDirName));
                //.flajson
                String flajsonFileName = path + "/" + mDownloadFlashDir + "/" + animName + ".flajson";
                deleteFile(new File(flajsonFileName));
                //.flabin
                String flabinFileName = path + "/" + mDownloadFlashDir + "/" + animName + ".flabin";
                deleteFile(new File(flabinFileName));
            }else{
                log("sd卡不可用");
            }
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
                    String imageDirFile = createDirInSdcard(ctx, mDownloadFlashDir + "/" + animName);
                    if(imageDirFile != null){
                        outFile = imageDirFile + "/" + fileName;
                    }
                    break;
                case DESCRIPTION:
                    String desDirFile = createDirInSdcard(ctx, mDownloadFlashDir);
                    if(desDirFile != null){
                        outFile = desDirFile + "/" + fileName;
                    }
                case ZIP:
                    String zipDirFile = createDirInSdcard(ctx, mDownloadFlashZipDir);
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

            final String outFileStr = outFile;
            download(url, outFile, new DownloadCallback() {
                @Override
                public void onComplete(boolean succ) {
                    log("download outFile=" + outFileStr +" is completed! succ=" + succ);
                    if (succ && type == DownloadType.ZIP){
                        String desDirFile = createDirInSdcard(ctx, mDownloadFlashDir);
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

            return true;
        }

        /***
         * 是否是一个合法的文件
         * @param f
         * @return
         */
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
                if(!desDir.mkdirs()){
                    return false;
                }
            }else if(!desDir.isDirectory()){
                deleteFile(desDir);
                if(!desDir.mkdirs()){
                    return false;
                }
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
    }/*Downloader*/

    /***
     * 删除文件／文件夹，包含子文件和子文件夹
     * @param file
     */
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

    /***
     * 获取文件在sd卡中的存储根目录，一般是：/sdcard/.[package name]/
     * @param ctx
     * @return
     */
    private static String getExternalStorageDirectory(Context ctx){
        String path;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }else{
            return null;
        }
        return path + "/" + "." + ctx.getPackageName();
    }

    /***
     * 在sdk卡的动画目录中创建文件夹，在这里是为了创建存储图片的文件夹，和flashAnims这个文件夹
     * @param ctx
     * @param flashDir
     * @return
     */
    private static String createDirInSdcard(Context ctx, String flashDir){
        String path = getExternalStorageDirectory(ctx);
        if (path == null){
            return null;
        }
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

    /***
     * 在getAnimFileDir返回的目录中创建 flashAnims 目录
     * @return
     */
    private boolean createSDCardPath(){
        String flashDir = createDirInSdcard(mContext, mFlashDir);
        if(flashDir != null) {
            mSdcardPath = flashDir.replace(mFlashDir, "");
            return true;
        }else{
            return false;
        }
    }

    /**
     * 停止动画，并清除所有数据
     */
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

    /***
     * 初始化变量，解析动画数据，读取动画需要的图片，最重要的方法
     * @return 返回是否正确初始化，只有正确初始化的动画才能够播放
     */
    private boolean init(){
        isInitOk = false;
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

        isInitOk = true;
        return true;
    }

    /***
     * 动画数据是否初始化成功
     * @return
     */
    public boolean isInitOk(){
        return isInitOk;
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

    /***
     * 从二进制动画描述文件中（xxx.flabin），读取某一帧的动画图片的信息（位置旋转缩放等等）
     * 重新把二进制数据读取成一个JSONObject是为了和Json数据读取方式共用一套生成关键帧对象的代码。
     * 因为两种数据解析方式，当读取到数据后，处理方式一摸一样，二者统一的关键就在于这个方法。
     * @param reader 数据读取对象
     * @param imageArr 图片名字数组
     * @return 返回一个JSONObject对象
     */
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

    /***
     * 解析json数据，并存储需要的图片
     */
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

    /***
     * 解析二进制数据，并存储所需的图片
     * mParsedData 结构为：
     * --mParsedData（Map，对应多个动画）
     *    --[key]: anim1
     *    --[value]:
     *      --AnimData
     *        --keyFrameData（数组）
     *          --[key]:0（表示第几帧）
     *          --[value]:（表示这一帧上不同层的所有图片信息）
     *              --image1
     *                  --xxxx1.png
     *                  --position:{100,100},
     *                  --scale:{1,1}
     *                  -- ... ...
     *              --image2
     *                  --xxxx2.png
     *                  --position:{100,100},
     *                  --scale:{1,1}
     *                  -- ... ...
     *              ... ...
     *          --[key]:1
     *          --[value]:
     *              --image1
     *                  --xxxx1.png
     *                  --position:{100,100},
     *                  --scale:{1,1}
     *                  -- ... ...
     *              --image2
     *                  --xxxx2.png
     *                  --position:{100,100},
     *                  --scale:{1,1}
     *                  -- ... ...
     *              ... ...
     *          ... ...
     *    --[key]: anim2
     *    --[value]:
     *      ... ...
     *    ... ...
     */
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

    /***
     * 解析关键帧数据，并将帧数据对应信息加入到parsedAnim中的对应索引内。
     * 播放的时候，播放到哪一帧就把对应的帧数据取出，然后显示里面的图片
     * 本方法处理的帧为：当前关键帧和上一个关键帧之间的所有帧数据。
     * @param oneFrame jsonObject
     * @param parsedAnim 解析完成的动画数据
     */
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

            for (int i = fromIdx; i <= toIdx; i++) {
                if (!mParseLastIsTween) {
                    if (i == toIdx) {
                        addOneFrameDataToIdx(oneFrame, i, parsedAnim);
                    } else {
                        addOneFrameDataToIdx(mParseLastFrame, i, parsedAnim);
                    }
                } else {
                    float per = (float) (i - fromIdx + 1) / len;
                    JSONObject newFrame = new JSONObject();
                    JSONObject lastFrameColor = mParseLastFrame.getJSONObject("color");
                    JSONObject oneFrameColor = oneFrame.getJSONObject("color");
                    String mark = null;
                    if(i == toIdx && oneFrame.has("mark")){
                        mark = oneFrame.getString("mark");
                    }
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
                    if (mark != null){
                        newFrame.put("mark", mark);
                    }
                    addOneFrameDataToIdx(newFrame, i, parsedAnim);
                }
            }

            if (duration > 1 && index + duration >= mParseFrameMaxIndex) {
                for (int i = index; i <= mParseFrameMaxIndex - 1; i++) {
                    addOneFrameDataToIdx(oneFrame, i, parsedAnim);
                }
            }

            mParseLastIndex = index;
            mParseLastIsTween = isTween;
            mParseLastFrame = oneFrame;
        }catch (JSONException e){
            log(e);
        }
    }

    /***
     * 计算补间动画关键帧的过度值，其实就是求取线性数据的插值
     * 其中旋转（skewX，skewY）同其他值有所不通。
     * flash中的逻辑是，这两个值一定在[-180, 180]之间，前后两个值相减的绝对值不能超过180才可以使用正常的线性插值，超过180的则需要将线性插值分为2部分：
     *   一是，先让oldValue同-180（或180，根据不通情况选择，见代码）进行插值
     *   二是，让-180（或180，根据不通情况选择，见代码）同newValue进行插值
     * @param lastFrame 上一帧的数据
     * @param newFrame 这一帧的数据
     * @param key 数据类型
     * @param per 百分比
     * @return
     */
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

    /***
     * 创建解析数据对象
     * @param idx 第几帧
     * @param parsedAnim 当前解析的动画map
     * @return
     */
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

    /***
     * 把解析的某一帧数据，添加到结果数据中
     * @param oneFrame 帧数据
     * @param idx 第几帧
     * @param parsedAnim 当前解析的anim
     */
    private void addOneFrameDataToIdx(JSONObject oneFrame, int idx, AnimData parsedAnim){
        ArrayList<KeyFrameData> arr = createArrForIdx(idx, parsedAnim);
        if(oneFrame != null) {
            arr.add(new KeyFrameData(oneFrame));
        }
    }

    /***
     * 可以用此方法重新加载一个新的flash动画文件。
     * @param flashName 动画文件名
     * @return
     */
    public boolean reload(String flashName){
        return reload(flashName, DEFAULT_FLASH_DIR);
    }

    /***
     * 可以用此方法重新加载一个新的flash动画文件。
     * @param flashName 动画文件名
     * @param flashDir 动画所在文件夹名
     * @return
     */
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
        if (!isInitOk()){
            log("[ERROR] call setEventCallback when init error");
            return;
        }
        mEventCallback = callback;
    }

    /***
     * 替换动画中所有相同图片
     * @param texName: 对应的图片名
     * @param bitmap: 新的Bitmap
     */
    public void replaceBitmap(String texName, Bitmap bitmap){
        if (!isInitOk()){
            log("[ERROR] call replaceBitmap when init error");
            return;
        }
        if (mImages != null) {
            Bitmap oldBitmap = null;
            if (mImages.containsKey(texName)){
                oldBitmap = mImages.get(texName);
                mImages.put(texName, bitmap);
                if (oldBitmap != null){
                    oldBitmap.recycle();
                    oldBitmap = null;
                }
            }
        }
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
        if (!isInitOk()){
            log("[ERROR] call play when init error");
            return;
        }
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

    /***
     * @return 是否动画正在播放
     */
    public boolean isPlaying(){
        return !isStop && !isPause && isInitOk();
    }

    /***
     *
     * @return 是否暂停
     */
    public boolean isPaused(){
        return isPause || !isInitOk();
    }

    /***
     * @return 是否动画已停止，或还未开始播放
     */
    public boolean isStoped(){
        return isStop || !isInitOk();
    }

    /***
     * 增加当前时间，这个由View层决定时间流逝的节奏
     * @param increaseValue 当前时间增加值
     */
    public void increaseTotalTime(double increaseValue){
        if (!isInitOk()){
            log("[ERROR] call increaseTotalTime when init error");
            return;
        }
        mTotalTime += increaseValue;
    }

    public String getDefaultAnimName(){
        if (isInitOk){
            String[] anims = mParsedData.keySet().toArray(new String[1]);
            Object a = 2;
            if (anims != null && anims.length > 0){
                return anims[0];
            }
        }
        return null;
    }

    public String getDefaultTexTureName(){
        if (isInitOk){
            String[] textures = mImages.keySet().toArray(new String[1]);
            if (textures != null && textures.length > 0){
                return textures[0];
            }
        }
        return null;
    }
    /***
     * 获取当前动画播放的总时间
     * @return
     */
    public double getTotalTime(){
        return isInitOk() ? mTotalTime : 0;
    }

    /***
     * 获取播放一帧动画需要多长时间
     * @return
     */
    public double getOneFrameTime(){
        return isInitOk() ? mOneFrameTime: 0;
    }

    /***
     * 获取动画最大下标
     * @return
     */
    public int getParseFrameMaxIndex(){
        return isInitOk() ? mParseFrameMaxIndex : 0;
    }

    /***
     * 获得动画最大帧数
     * @param animName
     * @return
     */
    public int getAnimFrameMaxIndex(String animName){
        if (isInitOk()){
            if (mParsedData.containsKey(animName)){
                return mParsedData.get(animName).animFrameLen;
            }
        }
        return 0;
    }
    /***
     * 获取动画帧数
     * @return 动画帧数
     */
    public int getLength(){
        return isInitOk() ? mParseFrameMaxIndex + 1: 0;
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
        if (!isInitOk()){
            log("[ERROR] call pause when init error");
            return;
        }
        isPause = true;
    }

    /***
     * 恢复
     */
    public void resume(){
        if (!isInitOk()){
            log("[ERROR] call resume when init error");
            return;
        }
        isPause = false;
    }

    /**
     * 读取二进制文件数据帮助类
     */
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

    /***
     * 自定义颜色类
     */
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

    /**
     * 表示flash的library中anims文件夹内某一个动画的数据
     */
    private static class AnimData{
        private HashMap<String, ArrayList<KeyFrameData>> keyFrameData;
        private int animFrameLen;
    }

    //原始动画数据
    private JSONObject mJson = null;
    private byte [] mData = null;

    //解析后的所有动画的数据
    private HashMap<String, AnimData> mParsedData = null;
    //所有用到的图片
    private HashMap<String, Bitmap> mImages = null;
    //动画帧率
    private int mFrameRate = -1;
    //播放一帧动画需要的时间
    private double mOneFrameTime = -1;
    //是否暂停
    private boolean isPause = true;
    //是否已停止
    private boolean isStop = true;

    //解析数据中用到的过程变量
    private int mParseFrameMaxIndex = 0;
    private int mParseLastIndex = -1;
    private boolean mParseLastIsTween = false;
    private JSONObject mParseLastFrame = null;

    //正在运行的动画名
    private String mRunningAnimName = null;
    //当前动画运行的总时间
    private double mTotalTime = 0;
    //当前动画从第几帧播放到第几帧
    private int mFromIndex;
    private int mToIndex;

    //上次循环绘制的是第几帧
    private int mLastFrameIndex = -1;

    /***
     * 在画布上绘制一张图片
     * @param c 画布
     * @param imagePath 图片路径
     * @param anchorPosition 带锚点位置
     * @param anchor 锚点
     * @param scale 缩放
     * @param rotate 旋转
     * @param alpha 透明度
     * @param color 颜色叠加
     */
    private void drawImage(Canvas c, String imagePath, Point anchorPosition, PointF anchor, PointF scale, PointF rotate, int alpha, BlendColor color){
        if (mImages == null || mImages.size() <= 0){
            return;
        }
        Bitmap bitmap = mImages.get(imagePath);
        //缩放体现着图片的宽高上
        float imageWidth = bitmap.getWidth() * mScaleX;
        float imageHeight = bitmap.getHeight() * mScaleY;

        //开始绘制
        c.save();

        //因为android中各种操作都是操作画布
        //而旋转缩放这些操作都应该在锚点上做，所以，首先把画布移动到锚点上
        c.translate(anchorPosition.x, anchorPosition.y);

        //旋转／切变
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

        //缩放
        c.scale(scale.x, scale.y);

        //由锚点移动回图片应该绘制的原点位置，imageWidth和imageHeight已经带有scale因素了
        PointF finalDrawPos = new PointF(-anchor.x * imageWidth, -anchor.y * imageHeight);
        c.translate(finalDrawPos.x, finalDrawPos.y);

        RectF bitmapDrawRect = new RectF(0, 0, imageWidth, imageHeight);

        //画bitmap
        Paint bitmapPaint = new Paint();
        //透明度
        bitmapPaint.setAlpha(alpha);
        //颜色混合模式
        bitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        c.drawBitmap(bitmap, null, bitmapDrawRect, bitmapPaint);

        //颜色叠加
        Paint bitmapRectPaint = new Paint();
        //颜色混合模式
        bitmapRectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        bitmapRectPaint.setColor(Color.argb(color.a, color.r, color.g, color.b));
        c.drawRect(bitmapDrawRect, bitmapRectPaint);

        //结束绘制
        c.restore();
    }

    /**
     * 有时候会因为卡顿或浮点数计算等原因产生漏帧现象，但是mark事件不能漏，这个方法就是一旦产生漏帧现象，把漏掉的事件找回来
     * @param animName 动画名称
     * @param frameIndex 第几帧
     */
    private void checkMark(String animName, int frameIndex){
        AnimData animData = mParsedData.get(animName);
        ArrayList<KeyFrameData> frameArr = animData.keyFrameData.get("" + frameIndex);

        for(int i = frameArr.size() - 1; i >= 0; i--) {
            KeyFrameData frameData = frameArr.get(i);
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

    /**
     * 清除屏幕
     * @param c 画布
     */
    public void cleanScreen(Canvas c){
        if (!isInitOk()){
            log("[ERROR] call cleanScreen when init error");
            return;
        }
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    /***
     * 绘制某一帧的所有数据，遍历着一帧上的所有frame，调用drawImage绘制每一层上的每一帧
     * @param c 画布
     * @param frameIndex 第几帧
     * @param animName 动画名
     * @param isTriggerEvent 是否触发事件，用与stopAt这种情况
     */
    public void drawCanvas(Canvas c, int frameIndex, String animName, boolean isTriggerEvent){
        if (!isInitOk()){
            log("[ERROR] call drawCanvas when init error");
            return;
        }
        AnimData animData = mParsedData.get(animName);

        if(isTriggerEvent && mEventCallback != null){
            FlashViewEventData eventData = new FlashViewEventData();
            eventData.index = frameIndex;
            mEventCallback.onEvent(FlashViewEvent.FRAME, eventData);
        }
        ArrayList<KeyFrameData> frameArr = animData.keyFrameData.get("" + frameIndex);
        for(int i = frameArr.size() - 1; i >= 0; i--){
            KeyFrameData frameData = frameArr.get(i);
            String imagePath = frameData.texName;
            Point point = new Point((int)(frameData.x * mScaleX + c.getWidth() / 2),(int)(-frameData.y * mScaleY + c.getHeight() / 2));
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
//                        stop();
                        pause();
                    }
                }
            }

            mLastFrameIndex = frameIndex;
        }
    }

    /**
     * 外部调用的绘制方法
     * @param c 画布
     * @return 是否绘制成功
     */
    public boolean drawCanvas(Canvas c){
        if(!isInitOk() || mRunningAnimName == null || isPause || isStop){
            return false;
        }
        int animLen = mToIndex - mFromIndex;
        int currFrameIndex = mFromIndex + (int)(mTotalTime / mOneFrameTime) % animLen;

        //检查漏下的帧事件 }
        if (mLastFrameIndex >= 0) {
            int mid = -1;
            if (mLastFrameIndex > currFrameIndex) {
                mid = mParseFrameMaxIndex;
            }
            if (mid != -1) {
                for (int i = mLastFrameIndex + 1; i <= mid; i++) {
                    checkMark(mRunningAnimName, i);
                }
                for (int i = 0; i < currFrameIndex; i++) {
                    checkMark(mRunningAnimName, i);
                }
            } else {
                for (int i = mLastFrameIndex + 1; i < currFrameIndex; i++) {
                    checkMark(mRunningAnimName, i);
                }
            }
        }

        drawCanvas(c, currFrameIndex, mRunningAnimName, true);
        return true;
    }

    /***
     * 将角度转为弧度
     * @param angle 角度
     * @return 弧度
     */
    private double angleToRadius(float angle){
        return 0.01745329252 * angle;
    }
    //绘制end

    public void clearBitmap(){
        for (Map.Entry<String,Bitmap> entry : mImages.entrySet()){
            entry.getValue().recycle();
        }
        mImages.clear();
    }
}
