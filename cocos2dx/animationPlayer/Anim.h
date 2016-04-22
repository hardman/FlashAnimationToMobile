/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

#ifndef __WINDY_ANIM_H__
#define __WINDY_ANIM_H__

#include "windy.h"
#include "animationPlayer/AnimNode.h"
#include "animationPlayer/AnimLayer.h"

NS_WINDY_BEGIN
class AnimLayer;
class AnimNode;
class Anim{
	vector<AnimLayer*> mLayers;
	unsigned short mMaxFrameNum;
public:
	void updateToLastKeyFrame(int index);
	unsigned short getMaxFrameNum() const { return mMaxFrameNum; }
	void setMaxFrameNum(unsigned short val) { mMaxFrameNum = val; }
	void updateToFrame(int currFrame);
	void updateToPercent( float per );
	bool updateToMarkFrame(const string & mark, const string &animName);
	void addOneLayer(AnimLayer* layer);
	AnimLayer *getLayer(int idx);
	Anim * clone(AnimNode* root);
	~Anim();
};
NS_WINDY_END

#endif
