/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

#include "animationPlayer/AnimKeyFrame.h"

void windy::AnimKeyFrame::setTextureName( string &texName )
{
	mTexName = texName;
}

windy::AnimKeyFrame::~AnimKeyFrame()
{
}

windy::AnimKeyFrame::AnimKeyFrame( AnimLayer* layer, AnimNode * root):
	mLayer(layer),
	mRoot(root),
	isEmpty(true),
	isTween(false),
	mark(""),
	fromIndex(-1),
	duration(0)
{
}

float getPerValue(float oldValue, float newValue, float per, bool isSpecialRange){
    float ret = -1;
    //计算跨度
    float span = fabsf(newValue - oldValue);
    if (span > 180 && isSpecialRange) {//说明2个值不能直接变化需要跳板
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
}

void windy::AnimKeyFrame::updateToPercent(float per){
	auto currFrame = mRoot->getCurrFrame();
	if(currFrame >= mStartIndex && currFrame <= mEndIndex && !isEmpty){
		auto texIndex = currFrame - mStartIndex;
		if(isTween && mTexInfos.size() > texIndex){
			auto startTexInfo = mTexInfos[texIndex];
			auto nextTexIndex = texIndex + 1;
			TexInfo endTexInfo;
			if(nextTexIndex >= mTexInfos.size()){
				endTexInfo = mNextKeyFrame->texInfo;
			}else{
				endTexInfo = mTexInfos[nextTexIndex];
			}
			TexInfo tI;
            tI.r = getPerValue(startTexInfo.r, endTexInfo.r, per, false);
            tI.g = getPerValue(startTexInfo.g, endTexInfo.g, per, false);
            tI.b = getPerValue(startTexInfo.b, endTexInfo.b, per, false);
            tI.a = getPerValue(startTexInfo.a, endTexInfo.a, per, false);
            tI.alpha = getPerValue(startTexInfo.alpha, endTexInfo.alpha, per, false);
            tI.x = getPerValue(startTexInfo.x, endTexInfo.x, per, false);
            tI.y = getPerValue(startTexInfo.y, endTexInfo.y, per, false);
            tI.scaleX = getPerValue(startTexInfo.scaleX, endTexInfo.scaleX, per, false);
            tI.scaleY = getPerValue(startTexInfo.scaleY, endTexInfo.scaleY, per, false);
            tI.skewX = getPerValue(startTexInfo.skewX, endTexInfo.skewX, per, true);
            tI.skewY = getPerValue(startTexInfo.skewY, endTexInfo.skewY, per, true);
			setTexInfo(tI);
		}
	}
}

void windy::AnimKeyFrame::updateToFrame( int currFrame )
{
	auto sp = mLayer->getSpriteByName(mTexName);
	if(currFrame >= mStartIndex && currFrame <= mEndIndex && !isEmpty){
        if(sp){
            if(!sp->getParent()){
                mRoot->addChildInner(sp, mLayer->getIndex(), mTexName);
            }
            sp->setVisible(true);
        }
		auto texIndex = currFrame - mStartIndex;
		if(currFrame == mStartIndex){
			setTexInfo(texInfo);
			if(mark != ""){
				mRoot->onFrameEvent(mLayer->getIndex(), mTexName, mark);
			}
		}else if(isTween && mTexInfos.size() > texIndex){
			setTexInfo(mTexInfos[texIndex]);
		}
	}else{
		/*if(sp){
			sp->setVisible(false);
		}*/
	}
}

void windy::AnimKeyFrame::initFrameInfo(unsigned short maxFrameNum)
{
	mNextKeyFrame = mLayer->getNextKeyFrame(this);
	mStartIndex = fromIndex;
	mEndIndex = fromIndex == maxFrameNum - 1 ? fromIndex: maxFrameNum;
	if(mNextKeyFrame){
		mEndIndex = mNextKeyFrame->fromIndex - 1;
	}
	if(isTween && mNextKeyFrame && ! mNextKeyFrame->isEmpty && mEndIndex > mStartIndex){
		auto frameLen = mEndIndex - mStartIndex + 1;
		for(auto i = 0; i < frameLen; i++){
			auto per = i * 1.f / frameLen;
			TexInfo tI;
			tI.r = getPerValue(texInfo.r, mNextKeyFrame->texInfo.r, per, false);
			tI.g = getPerValue(texInfo.g, mNextKeyFrame->texInfo.g, per, false);
			tI.b = getPerValue(texInfo.b, mNextKeyFrame->texInfo.b, per, false);
            tI.a = getPerValue(texInfo.a, mNextKeyFrame->texInfo.a, per, false);
            tI.alpha = getPerValue(texInfo.alpha, mNextKeyFrame->texInfo.alpha, per, false);
			tI.x = getPerValue(texInfo.x, mNextKeyFrame->texInfo.x, per, false);
			tI.y = getPerValue(texInfo.y, mNextKeyFrame->texInfo.y, per, false);
			tI.scaleX = getPerValue(texInfo.scaleX, mNextKeyFrame->texInfo.scaleX, per, false);
			tI.scaleY = getPerValue(texInfo.scaleY, mNextKeyFrame->texInfo.scaleY, per, false);
			tI.skewX = getPerValue(texInfo.skewX, mNextKeyFrame->texInfo.skewX, per, true);
			tI.skewY = getPerValue(texInfo.skewY, mNextKeyFrame->texInfo.skewY, per, true);
			mTexInfos.push_back(tI);
		}
	}
}

windy::TexInfo &  windy::AnimKeyFrame::getCurrTexInfo(){
    return mCurrTexInfo;
}

void windy::AnimKeyFrame::setTexInfo( TexInfo texInfo )
{
	auto sp = mLayer->getSpriteByName(mTexName);
    if(sp){
        sp->setPosition(texInfo.x, texInfo.y);
        sp->setCascadeColorEnabled(true);
        sp->setCascadeOpacityEnabled(true);
        sp->setOpacity(texInfo.alpha);
        sp->setScaleX(texInfo.scaleX);
        sp->setScaleY(texInfo.scaleY);
        sp->setRotationSkewX(texInfo.skewX);
        sp->setRotationSkewY(texInfo.skewY);
        
        //处理颜色叠加
        //注意这个颜色叠加需要设置混合模式，默认的混合模式可能不是想要的效果
        //应该使用
        //   glBlendFunc(GL_DST_ALPHA, GL_ONE_MINUS_SRC_ALPHA )
        //这个混合模式比较合适
        //源颜色乘以目标颜色（背景）的alpha值，能够使透明部分仍然保持透明。
        sp->removeAllChildren();
        if (texInfo.a > 0) {
            auto colorNode = Node::create();
            colorNode->setCascadeColorEnabled(true);
            colorNode->setCascadeOpacityEnabled(true);
            colorNode->setColor(Color3B(texInfo.r, texInfo.g, texInfo.b));
            colorNode->setOpacity(texInfo.a);
            colorNode->setAnchorPoint(Vec2(0, 0));
            colorNode->setContentSize(sp->getContentSize());
            sp->addChild(colorNode);
        }
    }
    mCurrTexInfo = texInfo;
}

windy::AnimKeyFrame* windy::AnimKeyFrame::clone(AnimLayer* layer, AnimNode* root)
{
	auto frame = new AnimKeyFrame(layer, root);
	frame->isEmpty = isEmpty;
	frame->fromIndex = fromIndex;
	if(!isEmpty){
		layer->addOneSprite(mTexName, mRoot);
		frame->setTextureName(mTexName);
        frame->duration = duration;
		frame->isTween = isTween;
		frame->mark = mark;
		frame->texInfo.r = texInfo.r;
		frame->texInfo.g = texInfo.g;
		frame->texInfo.b = texInfo.b;
        frame->texInfo.a = texInfo.a;
        frame->texInfo.alpha = texInfo.alpha;
		frame->texInfo.x = texInfo.x;
		frame->texInfo.y = texInfo.y;
		frame->texInfo.scaleX = texInfo.scaleX;
		frame->texInfo.scaleY = texInfo.scaleY;
		frame->texInfo.skewX = texInfo.skewX;
		frame->texInfo.skewY = texInfo.skewY; 
	}
	return frame;
}

bool windy::AnimKeyFrame::updateToMarkFrame( const string & pmark, const string & animName)
{
	if(!this->mark.empty() && pmark == this->mark){
		mRoot->updateToFrame(animName, fromIndex);
		return true;
	}
	return false;
}
