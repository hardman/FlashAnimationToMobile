/*
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/

//清除log栏中的所有文字
fl.outputPanel.clear()

//获取当前操作的文档,也就是当前fla
var doc = fl.getDocumentDOM()

//获取library，也就是右面带有movieclip和图片的那个列表
var lib = doc.library

//获取列表
var its = lib.items

//根据文件夹内的png图片,创建一一对应的movieclip
//下面代码流程其实就是在模拟实际用鼠标操作的过程
function createSymbols(){
	var symbolFolder = "eles"
	for(i = 0; i < its.length; i++){
		var it = its[i]
		if(it.itemType == "bitmap"){
            //允许平滑
			it.allowSmoothing = true
            //无损
			it.compressionType = "lossless"
			var spname = it.name.split("/")
			var itemName = spname[spname.length - 1]
            //创建movieclip的名称，无后缀
			var symbolName = symbolFolder + "/" + itemName
            //如果存在则不需创建
			if(lib.itemExists(symbolName)){
				fl.trace("元件 " + symbolName + " 已存在, 无需重新建立")
				continue
				//lib.deleteItem(symbolName)
			}
            //创建新的movie clip
			lib.addNewItem("movie clip", itemName, "top left")
            //开始编辑，固定写法，需要在完成后，结束编辑
			lib.editItem(itemName)
            //选中movie clip
			lib.selectItem(itemName)
			symbol = lib.getSelectedItems()[0]
            //如果文件夹不存在则创建文件夹
			if(!lib.itemExists(symbolFolder)){
			   lib.newFolder(symbolFolder)
			}
            //将movie clip 移到文件夹内，所有的movie clip 都在 eles这个文件夹内
			lib.moveToFolder(symbolFolder, symbol.name, false)
            //选中放入movieclip的图片
			lib.selectItem(it.name)
            //将图片加入到当前操作的movie clip中，因为上面的movie clip处于编辑模式，所以可以加入图片
			lib.addItemToDocument({x:0, y:0})
            //选中加入的图片
			doc.selectAll()
			ins = doc.selection[0]
            //设置图片的位置，居中。
			ins.x = -ins.width/2
			ins.y = -ins.height/2
            //退出编辑模式
			doc.exitEditMode()
			fl.trace("add " + it.name + " to " + symbol.name)
		}
	}
}

//创建一个名为 anims的文件夹 在 library中。
function createAnimFolder(){
	var animFolder = "anims"
	
	if(!lib.itemExists(animFolder)){
		lib.newFolder(animFolder)
	}
}

createSymbols()
createAnimFolder()
fl.trace("脚本执行完成")
