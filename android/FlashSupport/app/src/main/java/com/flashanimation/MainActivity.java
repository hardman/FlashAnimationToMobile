/*
copyright 2016 wanghongyu.
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
*/
package com.flashanimation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.flashanimation.newAnim.FlashAnimCommon;
import com.flashanimation.newAnim.FlashViewNew;
import com.flashanimation.view.FlashDataParser;
import com.flashanimation.view.FlashView;

public class MainActivity extends AppCompatActivity {

    FlashViewNew mFlashView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFlashView = (FlashViewNew)findViewById(R.id.flashview);

//        new Thread(){
//            @Override
//            public void run() {
//                boolean change = false;
//                while(true) {
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    if (change) {
//                        mFlashView.reload("heiniao", "flashAnims");
//                        mFlashView.play("atk", FlashAnimCommon.FlashLoopTimeForever);
//                    } else {
//                        mFlashView.reload("testDB", "flashAnims");
//                        mFlashView.play("applanbo", FlashAnimCommon.FlashLoopTimeForever);
//                    }
//                    change = !change;
//                }
//            }
//        }.start();
    }
}
