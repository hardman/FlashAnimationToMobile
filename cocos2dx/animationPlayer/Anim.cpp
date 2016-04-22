/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
#include "animationPlayer/Anim.h"

void windy::Anim::addOneLayer(AnimLayer* layer)
{
	mLayers.push_back(layer);
}

void windy::Anim::updateToFrame(int currFrame )
{
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		(*i)->updateToFrame(currFrame);
	}
}

void windy::Anim::updateToPercent( float per )
{
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		(*i)->updateToPercent(per);
	}
}

windy::Anim::~Anim()
{
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		delete *i;
	}
}

bool windy::Anim::updateToMarkFrame(const string & mark, const string &animName){
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		if(true == (*i)->updateToMarkFrame(mark, animName)){
			return true;
		}
	}
	return false;
}

void windy::Anim::updateToLastKeyFrame(int index){
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		(*i)->updateToLastKeyFrame(index);
	}
}

windy::Anim * windy::Anim::clone(AnimNode* root)
{
	auto anim = new Anim;
	anim->mMaxFrameNum = mMaxFrameNum;
	vector<AnimLayer*> layers;
	for(auto i = mLayers.begin(); i != mLayers.end(); i++){
		layers.push_back((*i)->clone(anim, root));
	}
	anim->mLayers = layers;
	return anim;
}

windy::AnimLayer * windy::Anim::getLayer( int idx )
{
	if(idx >= 0 && idx < mLayers.size()){
        for(auto i = mLayers.begin(); i != mLayers.end(); i++){
            if ((*i)->getIndex() == idx) {
                return *i;
            }
        }
	}
    return nullptr;
}
