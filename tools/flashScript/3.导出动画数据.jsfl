/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

fl.outputPanel.clear();

print("-----------------execute begin--------------")

//fl.openDocument("file:///C:/Users/Color/Desktop/fljs/测试.testTest.fla")

var doc = fl.getDocumentDOM()

var lib = doc.library

var its = lib.items

print(doc.path)

var outDir //输出文件夹名
var outFileName //输出文件名

//根据fla文件的名称路径，推算出输出文件夹名和输出文件名
//fla的命名应该以 "." 分为3部分：
// 测试.test.fla
// 第一部分：中文，对本文件的中文描述
// 第二部分：英文，表示本文件的英文标识符
// 第三部分：后缀，不用管。
// 其中第一部分中文可忽略。

//下面这种计算方法是为了兼容windows和mac两种操作系统
function calculateOutDir(){
    if(outDir != undefined && outDir != null){
        return
    }
    var outFileRoot = doc.path
    var outPathSp = outFileRoot.split("\\")

    if (outPathSp.length <= 1){
        outPathSp = outFileRoot.split("/")
    }

    var repl = outPathSp[outPathSp.length - 1]
    var replSp = repl.split(".")
    outFileName = replSp[replSp.length - 2]
    outFileRoot = outFileRoot.replace(repl, "")
    outFileRoot = outFileRoot.replace(new RegExp("\\\\","gm"),"/")
    outDir = "file:///" + outFileRoot
    print("outDir = " + outDir)
    print("outFileName = " + outFileName)
}

//获取输出json文件路径
function getJsonOutPath(){
    calculateOutDir()
	return outDir + outFileName + ".flajson"
}

//打印log
function print(msg){
	fl.trace(msg)
}

//加载json解析库
function loadLibs(){
	var jsonUri = fl.scriptURI
	var jsonUriSp = jsonUri.split("/")
	jsonUri = jsonUri.replace(jsonUriSp[jsonUriSp.length - 1], "")
	fl.runScript(jsonUri + "libs/json2.jsfl")
}

//将值加入数组中，防重复
function addValueToArr(v, arr){
	for(var i = 0; i < arr.length; i++){
		if(v == arr[i]){
			return
		}
	}
	arr.push(v)
}

//将library中的bitmap导出为文件
//needExportTexs是个数组，里面是需要导出的bitmap的名字
function exportPng(needExportTexs){
    calculateOutDir()
	var folder = outDir + outFileName
    //如果输出文件夹已存在则移除
	if(FLfile.exists(folder)){
		FLfile.remove(folder)
	}
    
    //创建文件夹
	FLfile.createFolder(folder)
    
    //将needExportTexs映射为map
	var texsMap = {}
	for(var h = 0; h < needExportTexs.length; h++){
		texsMap[needExportTexs[h]] = true
	}
    
    //遍历所有library的items，如果名字在needExportTexs中的bitmap，将会导出为png
	for(var i = 0; i < its.length; i++){
        //获取文件夹为pics的bitmap类型的图片
		var it = its[i]
		var itName = it.name
		var itNameSp = itName.split("/")
		var itShortName = itNameSp[itNameSp.length - 1]
		var fder = itName.replace("/" + itShortName, "")
		if(fder == "pics" && itName != "pics"){
			if(it.itemType != "bitmap"){
				print("error: 名字为"+itName+ "的项类型错误，应为 bitmap，当前类型为："+ it.itemType)
			}else{
				if(true == texsMap[itShortName]){
                    //导出图片，并设置为允许光滑和无损压缩
					it.allowSmoothing = true
					it.compressionType = "lossless"
					it.exportToFile(folder + "/" + itShortName)
				}
			}
		}
		
	}
}

//读取补间动画的每一个关键帧的信息。
function readInfo(){
	var obj = {}
	obj["name"] = outFileName
	obj["frameRate"] = doc.frameRate
	var usedTexs = []
	
	var animNum = 0
	var anims = []
	obj["anims"] = anims
	for(var i = 0; i < its.length; i++){
		var it = its[i]
		var itName = it.name
		var itNameSp = itName.split("/")
		var itShortName = itNameSp[itNameSp.length - 1]
		var folder = itName.replace("/" + itShortName, "")
		if(folder == "anims" && itName != "anims"){
			if(it.itemType != "movie clip"){
				print("error: 名字为"+itName+ "的项类型错误，应为 movie clip，当前类型为："+ it.itemType)
				return
			}else{
				print("handle anim " + itShortName + " begin")
				animNum++
				var oneAnim = {}
				anims.push(oneAnim)
				oneAnim["animName"] = itShortName
                //通过it.timeline获取动画的时间轴
				var itline = it.timeline
				oneAnim["layerNum"] = itline.layerCount
				oneAnim["frameMaxNum"] = itline.frameCount
				var animLayers = []
				oneAnim["layers"] = animLayers
                //layers就是时间轴上的每一层
				var itlayers = itline.layers
				for(var j = 0; j < itline.layerCount; j++){
					var itlayer = itlayers[j]
                    //frames就是每一层的所有关键帧
					var itframes = itlayer.frames
					var oneLayer = {}
					animLayers.push(oneLayer)
					var layerFrames = []
					oneLayer["frames"] = layerFrames
					var k = 0
					var hasLastIndex = false
					var lastIsEmpty = false
					for(; k < itframes.length;){
						var itframe = itframes[k]
						var oneFrame = {}
						layerFrames.push(oneFrame)
						oneFrame["isTween"] = (itframe.tweenType != "none")
						oneFrame["frameIndex"] = k
						oneFrame["isEmpty"] = false
						lastIsEmpty = false
						var itframeElements = itframe.elements
						if(itframeElements.length == 1){
							var itframeEle = itframeElements[0]
							if(itframeEle.elementType != "instance"){
								print("error: 名字为"+itName+ "的项，在层：" + itlayer.name + "的第" + k + "帧数据错误，这一帧上的图层不是instance类型")
								return
							}else{
								var eleTexName = itframeEle.libraryItem.name
								var eleTexNameSp = eleTexName.split("/")
								var eleTexShortName = eleTexNameSp[eleTexNameSp.length - 1]
								oneFrame["texName"] = eleTexShortName
								addValueToArr(eleTexShortName, usedTexs)
								//判断ele的instanceType合法
								if(itframeEle.instanceType != "symbol"){
									print("error: 名字为"+itName+ "的项，在层：" + itlayer.name + "的第" + k + "帧数据错误，这一帧上的图层上的Element不是symbol类型")
								}else{
									if(itframeEle.symbolType != "movie clip"){
										print("error: 名字为"+itName+ "的项，在层：" + itlayer.name + "的第" + k + "帧数据错误，这一帧上的图层上的Element的symbol类型不是 movie clip")
										return
									}else{
										//下面就是关键数据了,存储每一个关键帧的：位置，缩放，旋转，长度，名字，颜色等信息。
										//注意flash坐标系y轴为下方向为正方向，这里调整为上方向为正方向
										oneFrame["x"] = itframeEle.x
										oneFrame["y"] = -itframeEle.y
										oneFrame["scaleX"] = itframeEle.scaleX
										oneFrame["scaleY"] = itframeEle.scaleY
										oneFrame["skewX"] = itframeEle.skewX
										oneFrame["skewY"] = itframeEle.skewY
										oneFrame["duration"] = itframe.duration
										oneFrame["mark"] = itframeEle.name
										var frameColor = {}
                                        //透明度计算：alpha = 255 * alphaPercent + alphaMount
										function getAlphaValue(vA, vP){
											var res = 255
											res = res * vP * 0.01 + vA
											if(res < 0){
												res = 0
											}else if(res > 255){
												res = 255
											}
											return res
										}
										oneFrame["alpha"] = getAlphaValue(itframeEle.colorAlphaAmount, itframeEle.colorAlphaPercent)

                                        //在flash中颜色覆盖的rgb值(Color effect)请选择Advanced来做。
                                        //每个维度有2个变量一个是amount，一个是percent。
                                        //最终的值计算公式为：value = amount / (1 - percent * 0.01)
                                        //参考：http://blog.csdn.net/lake1314/article/details/7160963

                                        //这里还需注意下面的这个 a 只影响叠加的颜色。不同的a颜色看起来不同。
                                        //而上面的alpha则表示组件的透明度。二者具有不同意义。
										function getRGBValue(vA, vP){
											var res = 0
											if(vP < 100){
												res = vA / (1 - vP * 0.01)
											}
											if(res < 0){
												res = 0
											}else if(res > 255){
												res = 255
											}
											return res;
										}
										frameColor["r"] = getRGBValue(itframeEle.colorRedAmount, itframeEle.colorRedPercent)
										frameColor["g"] = getRGBValue(itframeEle.colorGreenAmount, itframeEle.colorGreenPercent)
										frameColor["b"] = getRGBValue(itframeEle.colorBlueAmount, itframeEle.colorBluePercent)
										frameColor["a"] = 255 * (1 - itframeEle.colorRedPercent * 0.01)
										oneFrame["color"] = frameColor
										if(k == itframes.length - 1){
											hasLastIndex = true
										} 
									}
								}
							}
						}else if (itframeElements.length == 0 ){
							oneFrame["isEmpty"] = true
							lastIsEmpty = true
						}else{
							print("error: 名字为"+itName+ "的项，在层：" + itlayer.name + "的第" + k + "帧数据错误，同一层，同一帧上只允许有一张图片，现在有 " + itframeElements.length + "张")
						}
						k += itframe.duration
					}
					if(k < oneAnim["frameMaxNum"] - 1 && !hasLastIndex && !lastIsEmpty){
						var oneFrame = {}
						layerFrames.push(oneFrame)
						oneFrame["isTween"] = false
						oneFrame["frameIndex"] = k
						oneFrame["isEmpty"] = true
					}

					oneLayer["keyFrameNum"] = layerFrames.length
				}
			}
		}
	}
	
	obj["animNum"] = animNum
	obj["texNum"] = usedTexs.length
	obj["textures"] = usedTexs
	return obj
}

//判断字符串是否存在中文
function hasChinese(s){
    return /.*[\u4e00-\u9fa5]+.*$/.test(s)
}

function execute(){
    //加载json库
    loadLibs()
    //计算路径
    calculateOutDir()
    //输出名字中不要中文
    if(!hasChinese(outFileName)){
        var obj = readInfo()
        var outFilePath = getJsonOutPath()
        print("outFilePath = " + outFilePath)
        FLfile.write(outFilePath,JSON.stringify(obj))
        print("文本已经写入到 " + outFilePath + " 中")
        exportPng(obj["textures"])
        print("图片已经保存到 " + outFilePath + "/" + outFileName + " 目录中")
        print("-----------------execute success--------------")
    }else{
        print("错误：fla文件名字格式错误，可以有2种：'中文.英文.fla', '英文.fla'")
        print("-----------------execute fail--------------")
    }
}

execute()

