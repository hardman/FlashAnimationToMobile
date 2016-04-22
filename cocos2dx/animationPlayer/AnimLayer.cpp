/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
#include "animationPlayer/AnimLayer.h"

void windy::AnimLayer::addOneKeyFrame(AnimKeyFrame*attr, const char *texName, int atFrame)
{
	mKeyFrames.push_back(attr);
    if (texName && atFrame >= 0 && !string(texName).empty()) {
        if (mKeyFramesByTexName.find(texName) == mKeyFramesByTexName.end()) {
            auto frameMap = mKeyFramesByTexName[texName];
            if (frameMap.find(atFrame) == frameMap.end()) {
                mKeyFramesByTexName[texName][atFrame] = attr;
            }
        }
    }
}

void windy::AnimLayer::updateToFrame( int currFrame )
{
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		(*i)->updateToFrame(currFrame);
	}
}

void windy::AnimLayer::updateToPercent( float per )
{
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		(*i)->updateToPercent(per);
	}
}

void windy::AnimLayer::updateToLastKeyFrame( int index )
{
	int lastIndex = -1;
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		if((*i)->fromIndex > index){
			(*i)->updateToFrame(lastIndex);
		}else if((*i)->fromIndex == index){
			(*i)->updateToFrame(index);
		}else{
			lastIndex = (*i)->fromIndex;
		}
	}
}

windy::AnimLayer::~AnimLayer()
{
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		delete *i;
	}
	mKeyFrames.clear();

	for(auto i = mSpritesMap.begin(); i != mSpritesMap.end(); i++){
		CC_SAFE_RELEASE_NULL(i->second);
	}
	mSpritesMap.clear();
}

windy::AnimKeyFrame* windy::AnimLayer::getNextKeyFrame( AnimKeyFrame*frame )
{
	AnimKeyFrame * nextFrame = nullptr;
	for(auto i = 0; i < mKeyFrames.size(); i++){
		if(frame == mKeyFrames[i]){
			if(i < mKeyFrames.size() - 1){
				nextFrame = mKeyFrames[i + 1];
			}
		}
	}
	return nextFrame;
}

void windy::AnimLayer::initFrameInfo(unsigned short maxFrameNum)
{
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		(*i)->initFrameInfo(maxFrameNum);
	}
}

windy::AnimLayer* windy::AnimLayer::clone(Anim* anim, AnimNode* root)
{
	auto animLayer = new AnimLayer;
	animLayer->mIndex = mIndex;
	vector<AnimKeyFrame*> keyFrames;
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		keyFrames.push_back((*i)->clone(animLayer, root));
	}
	animLayer->mKeyFrames = keyFrames;

    unordered_map<string, unordered_map<int, AnimKeyFrame*>> mKeyFramesByTexName;
    for (auto i = mKeyFramesByTexName.begin(); i != mKeyFramesByTexName.end(); i++) {
        auto texName = i->first;
        auto frameMap = i->second;
        for (auto j = frameMap.begin(); j != frameMap.end(); j++) {
            auto atFrame = j->first;
            auto keyFrame = j->second;
            auto keyFrameIndex = find(mKeyFrames.begin(), mKeyFrames.end(), keyFrame);
            animLayer->mKeyFramesByTexName[texName][atFrame] = keyFrames[keyFrameIndex - mKeyFrames.begin()];
        }
    }

    animLayer->initFrameInfo(anim->getMaxFrameNum());
	return animLayer;
}

void windy::AnimLayer::addOneSprite( const string & name, AnimNode* animNode, Node *sp )
{
	auto iter = mSpritesMap.find(name);
	if(iter == mSpritesMap.end()){
        if(!sp){
            if(animNode && animNode->isSheet){
                SpriteFrame *frame = SpriteFrameCache::getInstance()->getSpriteFrameByName(name);
                if (frame) {
                    sp = Sprite::createWithSpriteFrame(frame);
                }
            }else{
                string spFilePath = animNode->mFileDir + animNode->mFileName + "/" + name;
                sp = Sprite::create(spFilePath);
            }
        }
        if(sp){
            sp->setName(name);
            mSpritesMap[name] = sp;
            CC_SAFE_RETAIN(sp);
        }
	}
}

void windy::AnimLayer::removeOneSprite( const string &name )
{
	auto iter = mSpritesMap.find(name);
	if(iter != mSpritesMap.end()){
		CC_SAFE_RELEASE(iter->second);
		mSpritesMap.erase(name);
	}
}

windy::AnimKeyFrame * windy::AnimLayer::getKeyFrameByName(const string &name, int atFrame){
    if(mKeyFramesByTexName.find(name) != mKeyFramesByTexName.end()){
        auto keyFrame = mKeyFramesByTexName[name];
        if (keyFrame.find(atFrame) != keyFrame.end()) {
            return keyFrame[atFrame];
        }
    }
    return nullptr;
}

Node* windy::AnimLayer::getSpriteByName( const string &name )
{
	auto iter = mSpritesMap.find(name);
	if(iter != mSpritesMap.end()){
		return iter->second;
	}
	return nullptr;
}

bool windy::AnimLayer::updateToMarkFrame( const string & mark, const string &animName )
{
	for(auto i = mKeyFrames.begin(); i != mKeyFrames.end(); i++){
		if (true == (*i)->updateToMarkFrame(mark, animName)){
			return true;
		}
	}
	return false;
}
