/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
#ifndef __WINDY_ANIMNODE_H__
#define __WINDY_ANIMNODE_H__

#include "windy.h"
#include "animationPlayer/Anim.h"

NS_WINDY_BEGIN
class Anim;
class AnimLayer;
class AnimNode: public Node{
	friend class AnimLayer;
	friend class AnimKeyFrame;
	ssize_t mCurrPos;
	Data mData;
	unordered_map<string, Anim*> mAnimSet;
	string mCurrAnimName;
	bool isPause;
    bool isIgnorResume;
	int mLoopMode;
	int mCurrLoopCount;
	float mOneFrameTime;
	float mSpeedScale;
	float mCurrTime;
	float mCurrFrameStartTime;
	float mCurrUpdatePercent;
	int mCurrFrame;
	int mTotalLastFrame;
	int mStartFrame;
	int mEndFrame;
	Anim* mCurrAnim;
	Node* mRoot;
	function<void (string animName, int atFrame, string evt, int layerIndex, string texName)> mFrameEventCallback;
	function<void (string animName, int evtType)> mMovementEventCallback;
    function<void (string animName, int atFrame)> mEveryFrameCallback;
    
    string mFileDir;
	string mFileName;
    bool isSheet;

	ValueMap mMarkMap;
	ValueMap mFrameNums;
protected:
	void calculateContentSizeAndMove();
	void addChildInner(Node *child, int zOrder, const string&name);
	bool init() override;
	void hideAllChildren();
	void updateToPercent();
	void update(float t) override;
	void setOneFrameTime(float f);
	int getCurrFrame();
	void clear();
	void initData();
	void __readName(int len, char *name);
	template<class T>
	void readValue(T *type);
	string readName();
	void skipBytes(int num);
	void onFrameEvent(int layerIndex, string texName, string evt );
	void doCallfuncAnimEnd(int frameSize);
	AnimNode();
	bool updateToMarkFrame(const string & mark, const string & animName);
	void updateToMarkFrameForCalaSize(const string & animName);
	void addOneMark(string &animName, int layerNum, int keyNum, string &mark);
    void onEnter();
    void onExit();
protected:
	~AnimNode();
public:
    AnimNode* clone();
    void updateToFrame(const string &animName, int index);
	void calculateContentSizeAndMove( const char* animName );
	void setEveryFrameCallFunc(function<void (string animName, int atFrame)> val) { mEveryFrameCallback = val; }
	void setMovementEventCallFunc(function<void (string animName, int evtType)> val) { mMovementEventCallback = val; }
	void setFrameEventCallFunc(function<void (string animName, int atFrame, string evt, int layerIndex, string texName)> val) { mFrameEventCallback = val; }
	void play(const char *name, int loop = -1);
	void playWithIndex( const char *name, int start, int to = -1, int loop = -1);
	void pauseAtIndex(int index);
	bool isExistAnimation(const char *name);
	int getAnimFrameNum(const char * name);
	void preLoadOneAnim(const char * name);
	void preLoadAllAnims();
	string & getFileName();
	string & getCurrAnimName();
	void pause();
    void resume();
	void stop();
	AnimNode* getAnimation();
	void replaceSpriteByName(string texName, int layerIndex, int atFrame, Node *sp);
	int getLoopMode() const { return mLoopMode; }
	void setLoopMode(int mode);
	void setSpeedScale(float speed);
	void changeDir(int d);
	float getSpeedScale();
	void load(const char * dir);
	void setShaderProgram( GLProgram * glProgram );
	void setBlendFunc(const BlendFunc &blendFunc);
	float getOneFrameTime();

	const ValueVector & getMarks(string& animName);

	const ValueMap & getFrameNums();

	CREATE_FUNC(AnimNode);
};

NS_WINDY_END
#endif
