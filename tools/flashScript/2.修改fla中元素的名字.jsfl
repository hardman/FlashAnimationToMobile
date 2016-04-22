/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

fl.outputPanel.clear()

//fl.openDocument("file:///C:/Users/Color/Desktop/fljs/测试.fla");

var doc = fl.getDocumentDOM()

var lib = doc.library

var its = lib.items

//当前fla的文件路径
var uri = doc.pathURI

//fla的命名应该以 "." 分为3部分：
// 测试.test.fla
// 第一部分：中文，对本文件的中文描述
// 第二部分：英文，表示本文件的英文标识符
// 第三部分：后缀，不用管。
// 其中第一部分中文可忽略。

//根据fla文件的名称路径，获取文件名前缀，这里就是取英文部分名字
var spUri = uri.split("/")
var prefix = spUri[spUri.length - 1]
spPrefix = prefix.split(".")
prefix = spPrefix[spPrefix.length - 2]

//判断是否以str结尾
String.prototype.endWith = function(str){
	if (str == null || str == "" || this.length == 0 || str.length > this.length) {
  		return false;
  	} else if (this.substring(this.length - str.length) == str) {
		return true;
	} else {
	  	return false;
	}
}

//重命名，防止不同png重名。cocos2dx中，这一步尤为重要。
//因为cocos2dx中，会根据texture的名字缓存texture。
//app中可以给不同的动画分文件夹，就不那么重要了。
function renamePngs(){
	for(i = 0; i < its.length; i++){
		var it = its[i]
		var itName = it.name
		var spName = itName.split("/")
		var folder = spName[0]
		if(spName.length > 1){
			if(folder == "eles" || folder == "pics"){
				if(folder != itName){
					if( itName.indexOf(prefix + "_") == -1){
						fl.trace("itName=" +itName + ", folder=" + folder)
						lib.selectItem(itName)
						if(spName[1].endWith(".png")){
							lib.renameItem(prefix + "_" + spName[1])
						}else{
							lib.renameItem(prefix + "_" + spName[1] + ".png")
						}
					}else{
						fl.trace(folder + "/" +itName + " 已经包含 " + prefix + "_ 不需重命名了")
					}
				}
			}
		}
	}
}
//执行rename
renamePngs()

fl.trace("脚本执行完成")
