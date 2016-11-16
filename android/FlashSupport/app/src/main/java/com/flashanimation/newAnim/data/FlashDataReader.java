package com.flashanimation.newAnim.data;

/**
 * 读取二进制文件数据帮助类
 */
public class FlashDataReader{
    private byte [] mData = null;
    public FlashDataReader(byte[] data){
        mData = data;
        mIndex = 0;
    }
    private int mIndex;

    public boolean readBool(){
        boolean b = mData[mIndex] == 0x01;
        mIndex += 1;
        return b;
    }

    public int readUShort(){
        int s = (mData[mIndex] & 0xff) | ((mData[mIndex + 1] << 8) & 0xff00);
        mIndex += 2;
        return s;
    }

    public int readInt(){
        int i = (mData[mIndex] & 0xff | ((mData[mIndex + 1] << 8) & 0xff00)) |
                ((mData[mIndex + 2] << 16) & 0xff0000) | (mData[mIndex + 3] << 24);
        mIndex += 4;
        return i;
    }

    public float readFloat(){
        return Float.intBitsToFloat(readInt());
    }

    public short readUChar(){
        short c = (short)(mData[mIndex] & 0xff);
        mIndex += 1;
        return c;
    }

    public String readString(){
        int strLen = readUShort();
        String str = new String(mData, mIndex, strLen);
        mIndex += strLen;
        return str;
    }

    public void reset(){
        mIndex = 0;
    }
}