[项目源码](https://github.com/hardman/FlashAnimationToMobile)。

在我的博客中，有详细的项目介绍和使用方法（这里的不完整），欢迎点击：

- [项目介绍](http://blog.csdn.net/hard_man/article/details/51222423)。

- [项目使用方法](http://blog.csdn.net/hard_man/article/details/51222696)。

使用本项目的准备工作
---
首先确保系统中安装了flash，并且flash版本应该在cs3或者以上。
然后把"源码根目录/tools/flashScript"目录内的所有文件和文件夹copy到如下目录：

- Mac：~/Library/Application Support/Adobe/[Flash CS+版本号]/[en_US或者zh_CN]/Configuration/Commands
- Windows：C:\Users\[用户名]\AppData\Local\Adobe\[Flash CS+版本号]\[en_US或者zh_CN]\Configuration\Commands

在文件管理器（或Finder）目录中看起来是这样的：

    --Commands
        -- 1.根据png创建元件.jsfl
        -- 2.修改fla中元素的名字.jsfl
        -- 3.导出动画数据.jsfl
        -- libs/
            --json2.jsfl
        -- ....其他文件
如图：
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/1.png?raw=true" width="600">

这时候打开flash，点击菜单栏中的 Commands（中文的话应该是命令），在下拉菜单中就能看到我们加入的脚本啦。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/2.png?raw=true" width="600">

到此为止准备工作就绪。

美术人员制作flash动画的步骤
--
下面步骤看起来很长，其实内容很简单，都是大家各自平时使用的经验，在这里写这么多是为了让小白用户不出错而已。
美术人员使用步骤：

- 新建一个as3.0的Flash Document。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/3.png?raw=true" width="600">

- 保存文档，请务必保存文档，否则脚本不生效，并按照如下规则命名：
fla的命名应该以 "." 分为3部分：
    测试.test.fla
 第一部分：中文，对本文件的中文描述。（不重要，可以随意取。）
 第二部分：英文，表示本文件的英文标识符。（重要，在代码中会使用到这个关键字。）
 第三部分：后缀，默认即可不用管。（使用.fla即可。）
 其中第一部分中文可忽略。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/4.png?raw=true" width="600">

- 在新建的Flash文件窗口右侧的Library栏中，点击右键，新建一个文件夹名为“pics”(**注意，名字不能错，后面有类似的要求也要遵守**)。

- 把制作flash的图片（png格式）拖入pics文件夹中。[**!!!注意，所有的png图片必须带后缀.png否则会出错！**]
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/5.png?raw=true" width="600">

- 点击commands中的脚本“1.根据png创建元件”。结果如图：
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/6.png?raw=true" width="600">

- 如果是cocos2dx中使用，为了避免Sprite Frame Cache重名，或者想要为图片生成跟本动画相关的独一无二的前缀，可以点击commands中的脚本“2.修改fla中元素的名字”。结果如下：
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/7.png?raw=true" width="600">

- iOS可能也有此问题。因为直接拖入xcode中的文件一般选择“create groups”，这个只是逻辑文件夹，如果其他文件夹内存在同名文件则会冲突。所以最好每次制作动画，添加png图片的时候，都执行一次脚本“2.修改fla中元素的名字“。

- 新建一个Movie clip（影片剪辑），取一个合适的名字。然后拖入anims文件夹中
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/8.png?raw=true" width="600">
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/9.png?raw=true" width="600">

- 双击该Movie clip，进入编辑模式，此时就可以使用eles文件夹中的Movie clip，制作动画了。制作动画的具体细节要求，见下面的要求。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/10.png?raw=true" width="600">

- 制作完成后，保存，美术人员的工作就完成了。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/11.png?raw=true" width="600">

美术人员制作flash动画完整要求
---

1. 下面涉及名字的地方可以使用 英文字母，数字和下划线，不要用中文。
2. 先制作动画所需要的图片，png/jpg格式的，所有的动画元素需要全部使用图片，不可以使用矢量图和文字等等。
3. 图片命名尽量简单，以减少程序处理的数据量。
4. 建立fla时，使用Action Script 3。
5. 在库中建立3个文件夹，名字为：pics（图片），anims（动画的动作，比如idle, move等），eles（图片对应的元件）。对应的资源请在不同的文件夹中建立。
6. 每张图片（pics）都需要生成一个元件（eles），不要把多张图片放在一个元件中。所以元件的数量应该同图片的数量是相同的。
7. 所有的元件请使用 "影片剪辑"(movie clip), 不要使用 "按钮" 和 "图片"。
8. 把制作好的png图片（只用png，不要用jpg或其它格式图片）导入到flash中，并拖进pics文件夹下面。
9. 依次生成png图片对应的元件（影片剪辑），把图片拖到元件中。使图片居中。元件名字应该同图片的名字完全相同。这一步可以使用脚本（“1.根据png创建元件“）代替这个操作。
10. 建立新的元件，还是使用"影片剪辑"(movie clip)，然后拖进anims文件夹中。这就是需要制作的动作了。
11. 这时候，就可以使用eles(不要使用pics中的图片)中的元件在时间轴中制作动作了。
12. 制作动作，帧的普通操作(关键帧关键帧之间的传统补间，只能使用传统补间)都可以使用，但是对关键帧的处理只支持以下几种：移动，缩放，旋转，倾斜，颜色叠加，透明度的变化 这5种变换。
13. 不要使用除13条中描述的其他任何对关键帧的操作，比如滤镜，显示混合等。
14. 不要使用缓动，不要使用补间动画时元件旋转等高端操作。如果某一帧某个元件不可见，可以通过设置它的透明度为0，或者插入空白关键帧来实现。
15. 不要使用嵌套动画：就是说关键帧上最好只用eles中的元素来做，不要做好了一段动画，把这段动画作为关键帧使用。。
16. 最后，保存成fla就可以了。美术人员最终输出就是一个.fla文件。

程序人员使用美术制作好的动画
---
程序拿到美术人员制作好的fla文件后，首先要进行一番检查，看看是否合格。
所以需要确保程序员熟悉flash的页面和菜单，并了解一些简单的flash软件操作。

- 打开.fla文件。简单检查一下文件完成度。
    - 是否3个文件夹都在(anims，pics，eles)。
    - 是否动画文件都在anims文件夹内。
    - 是否pics与eles内文件数量相同，并且一一对应，相对应的2个组件名字也要完全一致。
    - 是否pics和eles内的组件名字都有.png后缀。
- 如果需要给关键帧添加事件，需要选中该关键帧（首先在timeline中选中关键帧，然后在主页面中选中该帧代表的图片，过程中最好隐藏timeline中的其它层），然后点击右侧与library同级的标签页properties。在第一行标有 < Instance Name > 的输入框，输入你的事件名，程序能够在播放到这一帧的时候，触发这个事件。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/12.png?raw=true" width="600">

- 事件添加完成后，选择菜单：Commands（命令）- “3.导出动画数据”。窗口底部同Timeline（时间轴）同级的Output（输出）栏中会显示脚本执行过程。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/13.png?raw=true" width="600">

- 成功后，打开.fla文件所在的目录，即可看到".flajson文件"和.fla同名"图片文件夹"（里面是图片）。
<img src="https://github.com/hardman/OutLinkImages/blob/master/FlashAnimationToMobile/images/14.png?raw=true" width="600">

- 如果需要使用二进制动画描述文件，则需要把".flajson文件"转为".flabin文件"，这两个后缀也不能改。
转换需要使用脚本"源码根目录/tools/JsonToBin.py"文件。这是一个python脚本。如果系统内没有python，则需要安装一个。
然后打开命令行（mac中使用终端，Windows中可使用cmd）执行如下命令，执行后的.flabin就是转换成二进制后的文件。
```
    python 源码根目录/tools/JsonToBin.py [.flajson文件全路径] [.flabin文件全路径]
```

- 这时候可以把".flajson文件"（或者 ".flabin文件"，二者使用其一即可，代码库内部处理，无需额外写代码判断）和"图片文件夹"放入程序指定目录就可以使用了。
    - cocos2dx可以放在资源目录中任意位置。代码初始化时需要指定目录。
    - Android需要将这2个文件放入 Assets文件夹的子文件夹flashAnims中。
    - iOS拖入xcode中，选择“copy if need”和“create groups”，点击确定。

程序员如何在代码中调用动画
---
```
//cocos2dx版本使用方法
//包含头文件
#include "AnimNode.h"
using namespace windy;

... ...

//使用代码
AnimNode *animNode = AnimNode::create();
animNode->load("xxxx/flashFileName.flajson");
animNode->play("animationName", WINDY_ANIMNODE_LOOP_FOREVER);//这里的animationName就是flash中anims文件夹内的动画名称
superNode->addChild(animNode);
```
```
<!--Android版本使用方法-->
<!--Android还需要在manifest文件中添加权限，与demo中相同添加即可。不要忘记res/values目录中的flashview_attr.xml文件。 -->
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:FlashView="http://schemas.android.com/apk/res-auto" <!--!!!!!!注意这个要加-->
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.xcyo.yoyo.flashsupport.MainActivity">

    <com.flashanimation.view.FlashView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        FlashView:flashDir="flashAnims"
        FlashView:flashFileName="callTextAnim"
        FlashView:defaultAnim="arriving1" <!--这里的defaultAnim就是flash中anims文件夹内的动画名称-->
        FlashView:designDPI="326"
        FlashView:loopTimes="0"
        android:id="@+id/flashview"
        />

</RelativeLayout>
```
```
//iOS版本使用方法
#import "FlashView.h"

... ...

FlashView *flashView = [[FlashView alloc] initWithFlashName:@"flashFileName"];
flashView.frame = self.view.frame;// CGRectMake(100, 100, 200, 500);
flashView.backgroundColor = [UIColor clearColor];
[superView addSubview:flashView];
[flashView play:@"animationName" loopTimes:FOREVER];//这里的animationName就是flash中anims文件夹内的动画名称
```




