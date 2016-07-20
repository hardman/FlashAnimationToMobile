/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
#include "animationPlayer/AnimNode.h"
void windy::AnimNode::load( const char * fileName )
{
	clear();
	initData();
    string fileNameStr = fileName;
    string fileNameWithTail = fileNameStr.substr(fileNameStr.find_last_of("/") + 1);
	string fileDir = fileNameStr.substr(0, fileNameStr.find_last_of("/") + 1);
    mFileDir = fileDir;
	mFileName = fileNameWithTail.substr(0, fileNameWithTail.find_last_of("."));
    
	//获取flabin的内容
	mData = FileUtils::getInstance()->getDataFromFile((fileDir + mFileName + ".flabin").c_str());
	//是否为图集
    string plistPath = fileDir + fileName + ".plist";
    isSheet = false;
    if (FileUtils::getInstance()->isFileExist(plistPath)) {
        CCSpriteFrameCache::getInstance()->addSpriteFramesWithFile(plistPath);
        isSheet = true;
    }
	mRoot = Node::create();
	mRoot->setCascadeColorEnabled(true);
	mRoot->setCascadeOpacityEnabled(true);
	addChild(mRoot, mRoot->getLocalZOrder(), "containerNode");
    
	//读取帧率
	unsigned short frameRate;
	readValue(&frameRate);
	setOneFrameTime(1.f/frameRate);
    
	//读取textures名字
    unsigned short textNum;
    readValue(&textNum);
    vector<string>texNames;
    for (int i = 0 ; i < textNum; i++) {
        texNames.push_back(readName());
    }
    
	unsigned short animNum;
	readValue(&animNum);
	for(unsigned short j = 0; j < animNum; j++){
		//‚àÇ¬°¬ª¬∞‚àÇ√ò¬™‚â†‚àöÀö‚â•‚àÜ
		string animName = readName();
		Anim* anim = new Anim;
		mAnimSet[animName] = anim;
		unsigned short maxFrameNum;
		readValue(&maxFrameNum);
		anim->setMaxFrameNum(maxFrameNum);
		unsigned short layerNum;
		readValue(&layerNum);
		for(unsigned short k = 0; k < layerNum; k++){
			AnimLayer * layer = new AnimLayer;
			layer->setIndex(layerNum - k - 1);
			unsigned short keyFrameNum;
			readValue(&keyFrameNum);
			for(unsigned short l = 0; l < keyFrameNum; l++){
				AnimKeyFrame * oneKeyFrame = new AnimKeyFrame(layer, this);
				bool isEmpty;
				readValue(&isEmpty);
				oneKeyFrame->isEmpty = isEmpty;
				unsigned short frameIndex;
				readValue(&frameIndex);
				oneKeyFrame->fromIndex = frameIndex;
				if(!isEmpty){
					unsigned short duration;
					readValue(&duration);
					oneKeyFrame->duration = duration;
					bool isTween;
					readValue(&isTween);
					oneKeyFrame->isTween = isTween;
                    
                    unsigned short texIndex;
                    readValue(&texIndex);
					string texName = texNames[texIndex];
                    if(texName.empty()){
                        log(1);
                    }
					layer->addOneSprite(texName, this);
					oneKeyFrame->setTextureName(texName);
                    
					string mark = readName();
					oneKeyFrame->mark = mark;
					if (mark != "") {
						addOneMark(animName, k, frameIndex, mark);
					}
                    unsigned char alpha;
                    readValue(&alpha);
                    oneKeyFrame->texInfo.alpha = alpha;
					unsigned char r;
					readValue(&r);
					oneKeyFrame->texInfo.r = r;
					unsigned char g;
					readValue(&g);
					oneKeyFrame->texInfo.g = g;
					unsigned char b;
					readValue(&b);
					oneKeyFrame->texInfo.b = b;
					unsigned char a;
					readValue(&a);
					oneKeyFrame->texInfo.a = a;
					float scaleX;
					readValue(&scaleX);
					oneKeyFrame->texInfo.scaleX = scaleX;
					float scaleY;
					readValue(&scaleY);
					oneKeyFrame->texInfo.scaleY = scaleY;
					float skewX;
					readValue(&skewX);
					oneKeyFrame->texInfo.skewX = skewX;
					float skewY;
					readValue(&skewY);
					oneKeyFrame->texInfo.skewY = skewY;
					float x;
					readValue(&x);
					oneKeyFrame->texInfo.x = x;
					float y;
					readValue(&y);
                    oneKeyFrame->texInfo.y = y;
                    layer->addOneKeyFrame(oneKeyFrame, texName.c_str(), frameIndex);
                }else{
                    layer->addOneKeyFrame(oneKeyFrame);
                }
			}
			layer->initFrameInfo(maxFrameNum);
			anim->addOneLayer(layer);
		}
	}
	calculateContentSizeAndMove();
}

bool windy::AnimNode::init()
{
	if(Node::init()){
		scheduleUpdate();
		return true; 
	}
	return false;
}

void windy::AnimNode::updateToPercent()
{
	float per = (mCurrTime - mCurrFrameStartTime) / getOneFrameTime();
	if (per != mCurrUpdatePercent){
		mCurrAnim->updateToPercent(per);
		mCurrUpdatePercent = per;
	}
}

void windy::AnimNode::update( float t )
{
	if(isPause) return;

	//¬∫‚àÜ√Ä‚Äû√∑¬∞¬†À?
	if(! mCurrAnim && isExistAnimation(mCurrAnimName.c_str())){
		mCurrAnim = mAnimSet[mCurrAnimName];
	}
    
    if(! mCurrAnim){
        return;
    }
    
    if(mCurrTime == 0 ){
        if(mMovementEventCallback)
            mMovementEventCallback(mCurrAnimName, 1);
    }

	//¬∫‚àÜ√Ä‚Äû‚â§‚Ä¢‚àë‚âà‚â•¬ß‚àÇ¬?
	int frameSize = mCurrAnim->getMaxFrameNum();
	if(mEndFrame > 0){
		if(mEndFrame <= frameSize){
			frameSize = mEndFrame - mStartFrame + 1;
		}
	}else{
		frameSize = frameSize - mStartFrame;
	}

	auto totalCurrFrame = int(mCurrTime / getOneFrameTime());

	if(totalCurrFrame != mTotalLastFrame){
		for(auto i = mTotalLastFrame + 1; i <= totalCurrFrame; i++){
			if(!mCurrAnim || isPause){
				return;
			}
			mCurrFrame = mStartFrame + int(i) % frameSize;
			hideAllChildren();
			mCurrAnim->updateToFrame(mCurrFrame);
			if(!mCurrAnim || isPause){
				return;
			}
			if(mEveryFrameCallback)
				mEveryFrameCallback(mCurrAnimName, mCurrFrame);
			doCallfuncAnimEnd(frameSize);
		}
		mTotalLastFrame = totalCurrFrame;
		mCurrFrameStartTime = mCurrTime;
		mCurrUpdatePercent = 0;
	} else{
		updateToPercent();
	}
	mCurrTime += t;
}

void windy::AnimNode::doCallfuncAnimEnd(int frameSize)
{
	if(mCurrFrame == mStartFrame + frameSize - 1){
		if(mLoopMode > 0){
			if(mCurrLoopCount >= mLoopMode){
				stop();
			}
			if(mLoopMode == 1){
				if(mMovementEventCallback)
					mMovementEventCallback(mCurrAnimName, 2);
			}else{
				if(mMovementEventCallback)
					mMovementEventCallback(mCurrAnimName, 3);
			}
		}else{
			if(mMovementEventCallback)
				mMovementEventCallback(mCurrAnimName, 4);
		}
		mCurrLoopCount++;
	}
}

void windy::AnimNode::pauseAtIndex(int index)
{
	if(mCurrAnim){
		mCurrAnim->updateToFrame(index);
		pause();
	}
}

void windy::AnimNode::setShaderProgram( GLProgram * glProgram )
{
	std::function<void (Node *node)> setNode = [&](Node* node){
		auto sprite = dynamic_cast<cocos2d::Sprite*>(node);
		if(sprite){
			sprite->setGLProgram(glProgram);
		}
	};

	for(auto &node: mRoot->getChildren()){
		setNode(node);
	}
}

void windy::AnimNode::playWithIndex( const char *name, int start, int to, int loop)
{	
	if(!isExistAnimation(name)){
		CCLOG("Error: anim named %s is not exist", name);
		return;
	}
	stop();
	//calculateContentSizeAndMove(name);
	mStartFrame = start;
	mEndFrame = to;
	mLoopMode = loop;
	mCurrAnimName = name;
	mAnimSet[name]->updateToLastKeyFrame(start);
	isPause = false;
	//update(0);
}

void windy::AnimNode::play( const char *name, int loop)
{
	if(!isExistAnimation(name)){
		CCLOG("Error: anim named %s is not exist", name);
		return;
	}
	stop();
	//calculateContentSizeAndMove(name);
	mCurrAnimName = name;
	mLoopMode = loop;
	isPause = false;
	//update(0);
}

void windy::AnimNode::setSpeedScale( float speed )
{
	mSpeedScale = speed;
}

float windy::AnimNode::getSpeedScale()
{
	return mSpeedScale;
}

void windy::AnimNode::setOneFrameTime( float f )
{
	mOneFrameTime = f;
}

float windy::AnimNode::getOneFrameTime()
{
	return mOneFrameTime / mSpeedScale;
}

void windy::AnimNode::setLoopMode( int mode )
{
	mLoopMode = mode;
}

void windy::AnimNode::stop()
{
	mCurrTime = 0;
	mCurrFrameStartTime = 0;
	mCurrUpdatePercent = 0;
	mCurrLoopCount = 1;
	mCurrFrame = 0;
	mTotalLastFrame = -1;
	mStartFrame = 0;
	mEndFrame = -1;
	mCurrAnim = nullptr;
	isPause = true;
    isIgnorResume = false;
}

void windy::AnimNode::onEnter(){
    isIgnorResume = !isIgnorResume;
    Node::onEnter();
    isIgnorResume = false;
}

void windy::AnimNode::onExit(){
    Node::onExit();
    isIgnorResume = true;
}

void windy::AnimNode::resume()
{
	Node::resume();
    if (!isIgnorResume) {
        isPause = false;
    }
}

void windy::AnimNode::pause()
{
	Node::pause();
	isPause = true;
}

int windy::AnimNode::getCurrFrame()
{
	return mCurrFrame;
}

void windy::AnimNode::initData()
{
	mSpeedScale = 1.f;
	mOneFrameTime = 0.05f;
	mLoopMode = 1;
	mCurrLoopCount = 1;
	isPause = true;
    isIgnorResume = false;
	mCurrPos = 0;
	mCurrAnim = nullptr;
	mCurrTime = 0;
	mCurrFrameStartTime = 0;
	mCurrFrame = 0;
	mTotalLastFrame = -1;
	if(mRoot){
		mRoot->removeFromParentAndCleanup(true);
	}
	mRoot = nullptr;
}

windy::AnimNode::AnimNode():
	mRoot(nullptr)
{
	setCascadeColorEnabled(true);
	setCascadeOpacityEnabled(true);
}
windy::AnimNode::~AnimNode()
{
	clear();
}

void windy::AnimNode::clear()
{
	for(auto i = mAnimSet.begin(); i != mAnimSet.end(); i++){
		delete i->second;
	}
	mAnimSet.clear();
}

void windy::AnimNode::__readName( int len, char *name )
{
	auto bytes = mData.getBytes();
	memcpy(name, bytes + mCurrPos, len);
	name[len] = '\0';
	mCurrPos += len;
};

string windy::AnimNode::readName()
{
	unsigned short len;
	readValue(&len);
	char *name = new char[len + 1];
	__readName(len, name);
	string res = name;
	delete [] name;
	return res;
}

void windy::AnimNode::skipBytes( int num )
{
	mCurrPos += num;
}

template<class T>
void windy::AnimNode::readValue( T *type )
{
	auto size = sizeof(T);
	auto bytes = mData.getBytes();
	memcpy(type, bytes + mCurrPos, size);
	mCurrPos += size;
}

void windy::AnimNode::hideAllChildren()
{
	for(auto child: mRoot->getChildren()){
		child->setVisible(false);
	}
}

void windy::AnimNode::onFrameEvent(int layerIndex, string texName, string evt )
{
	if(mFrameEventCallback)
		mFrameEventCallback(mCurrAnimName, mCurrFrame, evt, layerIndex, texName);
}

bool windy::AnimNode::isExistAnimation( const char *name )
{
    return mAnimSet.find(name) != mAnimSet.end();
}

windy::AnimNode* windy::AnimNode::getAnimation()
{
	return this;
}

int windy::AnimNode::getAnimFrameNum( const char * name )
{
	if(!isExistAnimation(name)){
		CCLOG("error: anim [%s] is not exist!", name);
		return 0;
	}
	return mAnimSet[name]->getMaxFrameNum();
}

windy::AnimNode* windy::AnimNode::clone()
{
	auto node = AnimNode::create();
	node->initData();
	node->mRoot = Node::create();
	node->mRoot->setCascadeColorEnabled(true);
	node->mRoot->setCascadeOpacityEnabled(true);
	node->addChild(node->mRoot, node->mRoot->getLocalZOrder(), "containerNode");
	for(auto i = mAnimSet.begin(); i != mAnimSet.end(); i++){
		node->mAnimSet[i->first] = i->second->clone(node);
	}
	node->mCurrAnimName = mCurrAnimName;
	node->mFileName = mFileName;
	node->isPause = isPause;
    node->isIgnorResume = isIgnorResume;
	node->mLoopMode = mLoopMode;
	node->mCurrLoopCount = mCurrLoopCount;
	node->mOneFrameTime = mOneFrameTime;
	node->mSpeedScale = mSpeedScale;
	node->mCurrTime = mCurrTime;
	node->mCurrFrameStartTime = mCurrFrameStartTime;
	node->mCurrUpdatePercent = mCurrUpdatePercent;
	node->mCurrFrame = mCurrFrame;
	node->mTotalLastFrame = mTotalLastFrame;
	node->mStartFrame = mStartFrame;
	node->mEndFrame = mEndFrame;
	node->mFrameEventCallback = mFrameEventCallback;
	node->mMovementEventCallback = mMovementEventCallback;
	node->mEveryFrameCallback = mEveryFrameCallback;

	for(auto i = mMarkMap.begin(); i != mMarkMap.end(); i++){
		node->mMarkMap[i->first] = i->second;
	}

	node->calculateContentSizeAndMove();
	
	node->setPosition(getPosition());
	node->setScaleX(getScaleX());
	node->setScaleY(getScaleY());
	node->setRotationSkewX(getRotationSkewX());
	node->setRotationSkewY(getRotationSkewY());

	return node;
}

void windy::AnimNode::addChildInner( Node *child, int zOrder, const string&name )
{
	mRoot->addChild(child, zOrder, name);
}

void windy::AnimNode::calculateContentSizeAndMove()
{
	string animName;
	if(isExistAnimation("idle")){
		animName = "idle";
	}else{
		/*for(auto i = mAnimSet.begin(); mAnimSet.end() != i; i++){
			animName = i->first;
			break;
		}*/
        setContentSize(Size(576, 320));
		return;
	}
	calculateContentSizeAndMove(animName.c_str());
}

bool windy::AnimNode::updateToMarkFrame(const string & mark, const string & animName)
{
	if(!isExistAnimation(animName.c_str())){
		return false;
	}
	return mAnimSet[animName]->updateToMarkFrame(mark, animName);
}

void windy::AnimNode::updateToFrame(const string &animName, int index){
	if(!isExistAnimation(animName.c_str())){
		return;
	}
	mAnimSet[animName]->updateToFrame(index);
}

void windy::AnimNode::updateToMarkFrameForCalaSize(const string & animName)
{
	if(animName == "idle"){
		updateToFrame(animName, 0);
	}else{
		updateToMarkFrame("updateSize", animName);
	}
}

void windy::AnimNode::calculateContentSizeAndMove( const char* animName )
{
	if(!isExistAnimation(animName)){
		return;
	}
	updateToMarkFrameForCalaSize(animName);
	float minX = INT_MAX, maxX = INT_MIN;
	float minY = INT_MAX, maxY = INT_MIN;
	for(auto &child: mRoot->getChildren()){
		auto pos = child->getPosition();
		auto childContentSize = child->getContentSize();
		if(pos.x - childContentSize.width / 2 < minX){
			minX = pos.x - childContentSize.width/2;
		}
		if(pos.y - childContentSize.height / 2 < minY){
			minY = pos.y - childContentSize.height / 2;
		}
		if(pos.x + childContentSize.width / 2 > maxX){
			maxX = pos.x + childContentSize.width / 2;
		}
		if(pos.y + childContentSize.height / 2 > maxY){
			maxY = pos.y + childContentSize.height / 2;
		}
	}

	Size contentSize(maxX - minX, maxY - minY);
	mRoot->setPosition(-minX, -minY);
	setContentSize(contentSize);
	setAnchorPoint(Vec2(/*0.5,0));//*/-minX / contentSize.width, -minY / contentSize.height));
}

string & windy::AnimNode::getCurrAnimName()
{
	return mCurrAnimName;
}

string & windy::AnimNode::getFileName()
{
	return mFileName;
}

void windy::AnimNode::preLoadOneAnim( const char * name )
{
	if(isExistAnimation(name)){
		auto maxFrameNum = getAnimFrameNum(name);
		auto anim = mAnimSet[name];
		for(auto i = 0; i < maxFrameNum; i++){
			anim->updateToFrame(i);
		}
	}
}

void windy::AnimNode::preLoadAllAnims()
{
	const char *names[] = {"atk","atk1","atk2","atk3","cheer","damaged","dead","idle","move"};
	for(auto &name: names){
		preLoadOneAnim(name);
	}
}

void windy::AnimNode::setBlendFunc( const BlendFunc &blendFunc )
{
	for(auto &child: mRoot->getChildren()){
		Sprite * sp = dynamic_cast<Sprite*>(child);
		if(sp){
			sp->setBlendFunc(blendFunc);
		}
	}
}

void windy::AnimNode::addOneMark( string &animName, int layerNum, int keyNum, string &mark )
{
	if(mMarkMap[animName].isNull()){
		mMarkMap[animName] = ValueVectorNull;
	}
	ValueMap vMap;
	vMap["layerNum"] = layerNum;
	vMap["keyNum"] = keyNum;
	vMap["mark"] = mark;
	mMarkMap[animName].asValueVector().push_back(Value(vMap));
}

const ValueVector & windy::AnimNode::getMarks( string& animName )
{
	if(mMarkMap.find(animName) != mMarkMap.end()){
		return mMarkMap[animName].asValueVector();
	}
	return ValueVectorNull;
}

const ValueMap & windy::AnimNode::getFrameNums()
{
	for(auto iter = mAnimSet.begin(); iter != mAnimSet.end(); iter++){
		mFrameNums[iter->first] = iter->second->getMaxFrameNum();
	}
	return mFrameNums;
}

void windy::AnimNode::changeDir( int d )
{
	CCASSERT(d == 1 || d == -1, "changeDir param d must be 1 or -1");
	mRoot->setScaleX(d);
	/*for(auto &child: mRoot->getChildren()){
		child->setScaleX(d);
	}*/
}

void windy::AnimNode::replaceSpriteByName( string texName, int layerIndex, int atFrame, Node *sp )
{
	//getLayer
	auto layer = mCurrAnim->getLayer(layerIndex);
	auto keyFrame = layer->getKeyFrameByName(texName, atFrame);
    auto texInfo = keyFrame->getCurrTexInfo();
    auto oldSp = layer->getSpriteByName(texName);
    
	//get parent
	auto parent = mRoot;

	//set attr
    
    sp->setPosition(texInfo.x, texInfo.y);
    sp->setCascadeColorEnabled(true);
    sp->setColor(Color3B(texInfo.r, texInfo.g, texInfo.b));
    sp->setCascadeOpacityEnabled(true);
    sp->setOpacity(texInfo.alpha);
    sp->setScaleX(texInfo.scaleX);
    sp->setScaleY(texInfo.scaleY);
    sp->setRotationSkewX(texInfo.skewX);
    sp->setRotationSkewY(texInfo.skewY);
	
	//remove old
    if(oldSp){
        oldSp->removeFromParent();
        layer->removeOneSprite(texName);
    }
    
	//add new
	parent->addChild(sp, layer->getIndex(), texName);
	layer->addOneSprite(texName, this, sp);
}
