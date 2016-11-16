package com.flashanimation.newAnim;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.flashanimation.newAnim.data.FlashAnimData;
import com.flashanimation.newAnim.data.FlashAnimDataKeyFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by wanghongyu on 16/11/16.
 */
public class FlashAnimCommon {
    /**
     * 系统上下文
     */
    private Context mContext;
    private AssetManager mAssetManager;

    /**
     * 动画所在的位置，sd卡还是assets
     */
    private FlashFileLocationType mLocationType;

    public void setLocationType(FlashFileLocationType mLocationType) {
        this.mLocationType = mLocationType;
    }

    public FlashFileLocationType getLocationType() {
        return mLocationType;
    }


    /**
     * 构造函数
     * @param context
     */
    public FlashAnimCommon(Context context){
        this(context, FlashFileLocationType.NONE);
    }


    /**
     * 构造函数
     * @param context
     * @param locationType
     */
    public FlashAnimCommon(Context context, FlashFileLocationType locationType){
        mContext = context;
        mAssetManager = context.getAssets();
        mLocationType = locationType;
    }

    public static String JSONEXT = ".flajson";
    public static String BINEXT = ".flabin";

    private static final String TAG = "FlashAnim";
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

    public enum FlashFileLocationType{//表示动画文件在sd卡还是assets
        ASSETS,
        SDCARD,
        NONE
    }

    public enum FlashFileType{//描述文件是json还是二进制
        JSON,
        BIN,
        NONE
    }

    //事件回调数据，内容可为空
    public static class FlashViewEventData{
        public int index;
        public String mark;
        public Object data;
    }

    //事件回调接口
    public interface IFlashViewEventCallback{
        void onEvent(FlashViewEvent e, Object data);
    }

    //render 回调
    public interface FlashViewRender{
        void updateToFrameIndex(FlashAnimData animData, String playingAnimName, int frameIndex);
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
     * 获取文件在sd卡中的存储根目录，一般是：/sdcard/.[package name]/
     * @param ctx
     * @return
     */
    public static String getExternalStorageDirectory(Context ctx){
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
    public static String createDirInSdcard(Context ctx, String flashDir){
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
        switch (mLocationType){
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
                File f = new File(getExternalStorageDirectory(mContext) + "/" + path);
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
    public byte[] readData(String file){
        return getData(file, DataType.BYTES).bytes;
    }

    /***
     * 读取json数据
     * @return json object
     */
    public JSONObject readJson(String file){
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(getData(file, DataType.STRING).string);
        }catch(JSONException e){
            log(e);
        }
        return jsonObj;
    }

    /***
     * 读取图片
     * @param imagePath
     * @return bitmap
     */
    public Bitmap readImage(String imagePath){
        byte bytes[] = getData(imagePath, DataType.BYTES).bytes;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }


    private HashMap<String, Bitmap> mStoredImages;
    /**
     * @param name
     * @param bitmap
     */
    public void addImage(String name, Bitmap bitmap){
        if (name == null || bitmap == null){
            return;
        }
        if (mStoredImages == null){
            mStoredImages = new HashMap<>();
        }
        mStoredImages.put(name, bitmap);
    }

    /**
     * @param name
     * @param replaceBitmap
     */
    public void replaceImage(String name, Bitmap replaceBitmap){
        if (name == null || replaceBitmap == null){
            return;
        }
        if (mStoredImages == null){
            return;
        }
        if (mStoredImages.containsKey(name)) {
            mStoredImages.remove(name);
            mStoredImages.put(name, replaceBitmap);
        }
    }

    /**
     * @param name
     * @return
     */
    public Bitmap getImage(String name){
        if (name == null){
            return null;
        }
        if (mStoredImages == null){
            return null;
        }
        return mStoredImages.get(name);
    }
}
