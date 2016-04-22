/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
#ifndef __WINDY_ANIM_LAYER_H__
#define __WINDY_ANIM_LAYER_H__

#include "windy.h"
#include "animationPlayer/AnimNode.h"
#include "animationPlayer/AnimLayer.h"
#include "animationPlayer/AnimKeyFrame.h"

NS_WINDY_BEGIN
class AnimNode;
class AnimKeyFrame;
class Anim;
class AnimLayer{
    vector<AnimKeyFrame*> mKeyFrames;
    unordered_map<string, unordered_map<int, AnimKeyFrame*>> mKeyFramesByTexName;
	unordered_map<string, Node*> mSpritesMap;
	int mIndex;
public:
	bool updateToMarkFrame( const string & mark, const string &animName);
	void updateToLastKeyFrame( int index );
	void addOneSprite(const string &name, AnimNode* animNode = nullptr, Node *sp = nullptr);
	void removeOneSprite(const string &name);
    Node* getSpriteByName(const string &name);
    AnimKeyFrame* getKeyFrameByName(const string &name, int atFrame);
	int getIndex() const { return mIndex; }
	void setIndex(int val) { mIndex = val; }
	void addOneKeyFrame(AnimKeyFrame* keyFrame, const char* texName = nullptr, int atFrame = -1);
	void updateToFrame(int currFrame);
	void updateToPercent(float per);
	void initFrameInfo(unsigned short maxFrameNum);
	AnimLayer* clone(Anim* anim, AnimNode* root);
	AnimKeyFrame* getNextKeyFrame(AnimKeyFrame*frame);
	~AnimLayer();
};
NS_WINDY_END
#endif
