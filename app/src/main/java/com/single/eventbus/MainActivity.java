package com.single.eventbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import co.library.eventbus.Bus;

public class MainActivity extends AppCompatActivity {
    EventButListener eventButListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        eventButListener = new EventButListener(this);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bus.getDefault().post(new Event("我接收到消息了..."));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        eventButListener.unRegist();
    }
}
