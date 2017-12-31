package com.zf.sync;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by zhangfei on 2017/11/12.
 */
public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*new Thread(){
            @Override
            public void run() {
                ProtoBufServer server = new ProtoBufServer();
                try {
                    server.bind(8888);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();*/
    }
}
