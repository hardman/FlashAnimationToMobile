/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

#ifndef __WINDY_ANIMKEYFRAME_H__
#define __WINDY_ANIMKEYFRAME_H__

#include "windy.h"
#include "animationPlayer/AnimLayer.h"

NS_WINDY_BEGIN
class AnimLayer;
class AnimNode;
struct TexInfo{
    unsigned char r;
    unsigned char g;
    unsigned char b;
    unsigned char a;
    unsigned char alpha;
    float x;
    float y;
    float scaleX;
    float scaleY;
    float skewX;
    float skewY;
    TexInfo():
    r(255),
    g(255),
    b(255),
    a(255),
    x(0.f),
    y(0.f),
    scaleX(1.f),
    scaleY(1.f),
    skewX(0.f),
    skewY(0.f)
    {}
};
class AnimKeyFrame{
	string mTexName;
	AnimLayer * mLayer;
	AnimKeyFrame* mNextKeyFrame;
	AnimNode * mRoot;
    TexInfo mCurrTexInfo;
public:
	AnimKeyFrame(AnimLayer* layer, AnimNode* root);
	bool isEmpty;
	bool isTween;
	string mark;
	unsigned short fromIndex;
	unsigned short duration;
	TexInfo texInfo;
    TexInfo & getCurrTexInfo();
	void setTexInfo(TexInfo texInfo);
	void setTextureName(string &texName);
	void updateToFrame(int currFrame);
	void updateToPercent(float percent);
	bool updateToMarkFrame( const string & mark, const string & animName );
	void initFrameInfo(unsigned short maxFrameNum);
	AnimKeyFrame* clone(AnimLayer* layer, AnimNode* root);
	~AnimKeyFrame();
	AnimKeyFrame();
private:
	vector<TexInfo> mTexInfos;
	unsigned short mStartIndex;
	unsigned short mEndIndex;
};
NS_WINDY_END

#endif
