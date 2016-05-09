package com.flashanimation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.flashanimation.view.FlashDataParser;
import com.flashanimation.view.FlashView;

public class MainActivity extends AppCompatActivity {

    FlashView mFlashView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFlashView = (FlashView)findViewById(R.id.flashview);

        new Thread(){
            @Override
            public void run() {
                boolean change = false;
                while(true) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (change) {
                        mFlashView.reload("heiniao", "flashAnims");
                        mFlashView.play("atk", FlashDataParser.FlashLoopTimeForever);
                    } else {
                        mFlashView.reload("testDB", "flashAnims");
                        mFlashView.play("applanbo", FlashDataParser.FlashLoopTimeForever);
                    }
                    change = !change;
                }
            }
        }.start();
    }
}
